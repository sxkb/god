package god.bai;

import god.bai.god.platform.Platform;
import god.bai.god.platform.Platforms;
import god.bai.god.shen.ShenCommand;
import god.bai.god.shen.ShenEventDispatcher;
import god.bai.god.shen.ShenSystem;
import god.bai.god.shen.compat.smfs.SmfsIntegration;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 言出法随 —— 与加载器无关的公共入口。
 *
 * <p>Fabric / Forge 的入口类只需：注入 {@link Platform}、注册命令、注册事件与网络，
 * 然后调用这里的 init / 生命周期方法即可。</p>
 */
public final class GodBootstrap {

    public static final String MOD_ID = "god";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private GodBootstrap() {
    }

    public static void init(Platform platform) {
        Platforms.set(platform);
        ShenSystem.initialize();
        LOGGER.info("言出法随系统初始化完成（平台: {}）", platform.getClass().getSimpleName());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        ShenCommand.register(dispatcher);
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        player.server.execute(() -> ShenSystem.loadPlayerLaws(player));
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        player.server.execute(() -> ShenSystem.unloadPlayerLaws(player));
    }

    public static void onServerTick(MinecraftServer server) {
        ShenEventDispatcher.serverTick(server);
        SmfsIntegration.tickServer(server);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
