package god.bai.god.shen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 言出法随命令入口：/god shen
 * 无任何权限要求，普通玩家（无作弊权限）也可直接呼出编辑器。
 */
public class ShenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 追加式注册：若 /god 根已被其他模组注册（如完整神 /god on|off），
        // 仅追加 shen 分支，不整体覆盖根节点，保证两个模组命令共存。
        var shenNode = Commands.literal("shen")
            .then(Commands.literal("open")
                .executes(ShenCommand::executeOpen))
            .then(Commands.literal("list")
                .executes(ShenCommand::executeList))
            .then(Commands.literal("reload")
                .executes(ShenCommand::executeReload))
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        ServerPlayer player = context.getSource().getPlayer();
                        if (player != null) {
                            for (ShenLaw law : ShenDataManager.loadLaws(player)) {
                                builder.suggest(law.name);
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(ShenCommand::executeDelete)))
            .executes(ShenCommand::executeOpen)
            .build();
        var godRoot = dispatcher.getRoot().getChild("god");
        if (godRoot != null) {
            if (godRoot.getChild("shen") == null) {
                godRoot.addChild(shenNode);
            }
        } else {
            dispatcher.register(Commands.literal("god").then(shenNode));
        }
    }

    private static int executeOpen(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("只能由玩家执行此命令"));
            return 0;
        }

        if (!ShenSystem.canUseShen(player)) {
            player.sendSystemMessage(Component.literal("§c你没有权限使用言出法随系统"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6[言出法随] §e打开编辑器..."));
        ShenServerNetwork.sendOpenScreen(player);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        if (!ShenSystem.canUseShen(player)) {
            player.sendSystemMessage(Component.literal("§c你没有权限使用言出法随系统"));
            return 0;
        }

        List<ShenLaw> laws = ShenDataManager.loadLaws(player);
        if (laws.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e你还没有保存任何法则"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6=== 已保存的法则 ==="));
        for (int i = 0; i < laws.size(); i++) {
            ShenLaw law = laws.get(i);
            String status = law.enabled ? "§a[启用]" : "§c[禁用]";
            player.sendSystemMessage(Component.literal("§e" + (i + 1) + ". " + law.name + " " + status + " §7(" + law.rules.size() + "条规则)"));
        }
        return laws.size();
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        if (!ShenSystem.canUseShen(player)) {
            player.sendSystemMessage(Component.literal("§c你没有权限使用言出法随系统"));
            return 0;
        }

        ShenSystem.unloadPlayerLaws(player);
        ShenSystem.loadPlayerLaws(player);
        player.sendSystemMessage(Component.literal("§a法则已重新加载"));
        return 1;
    }

    private static int executeDelete(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        if (!ShenSystem.canUseShen(player)) {
            player.sendSystemMessage(Component.literal("§c你没有权限使用言出法随系统"));
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        ShenDataManager.deleteLaw(player, name);
        player.sendSystemMessage(Component.literal("§c已删除法则: " + name));
        return 1;
    }
}
