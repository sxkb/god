package god.bai.god.fabric;

import god.bai.GodBootstrap;
import god.bai.god.shen.ShenEventDispatcher;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fabric 事件适配：只负责把 Fabric API 事件转成 {@link ShenEventDispatcher} 的调用。
 */
public final class FabricEvents {

    private FabricEvents() {
    }

    public static void registerCommon() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ShenEventDispatcher.onEntityInteract(player, world, entity);
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            onRightClickBlock(player, world, hand, hit);
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ShenEventDispatcher.onRightClickItem(player, world, player.getItemInHand(hand));
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ShenEventDispatcher.onAttack(player, entity);
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
            ShenEventDispatcher.onBlockBreak(player, world, pos));

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
            ShenEventDispatcher.onEntityJoinLevel(world, entity));

        // 伤害 / 死亡：无敌目标直接取消，其余触发「受伤 / 死亡」事件
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (ShenEventDispatcher.isInvincible(entity)) return false;
            ShenEventDispatcher.onLivingHurt(entity);
            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
            !ShenEventDispatcher.isInvincible(entity));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!ShenEventDispatcher.isInvincible(entity)) {
                ShenEventDispatcher.onLivingDeath(entity);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
            ShenEventDispatcher.onRespawn(newPlayer));

        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof Player player) {
                ShenEventDispatcher.onSleepInBed(player, sleepingPos);
            }
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof Player player) {
                ShenEventDispatcher.onWakeUp(player);
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
            ShenEventDispatcher.onChat(sender, message.decoratedContent().getString()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GodBootstrap.onPlayerLoggedIn(handler.player);
            ShenEventDispatcher.onPlayerLoggedIn(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GodBootstrap.onPlayerLoggedOut(handler.player);
            ShenEventDispatcher.onPlayerLoggedOut(handler.player);
        });

        ServerTickEvents.END_SERVER_TICK.register(GodBootstrap::onServerTick);
    }

    private static void onRightClickBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        ShenEventDispatcher.onRightClickBlock(player, world, hit.getBlockPos(), hit.getDirection(), player.getItemInHand(hand));
    }
}
