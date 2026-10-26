package god.bai.god.forge;

import god.bai.GodBootstrap;
import god.bai.god.shen.ShenEventDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge 事件适配：只负责把 Forge 事件转成 {@link ShenEventDispatcher} 的调用。
 */
public class ForgeEvents {

    // ============ 右键 ============
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return;
        ShenEventDispatcher.onEntityInteract(event.getEntity(), event.getLevel(), event.getTarget());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return;
        ShenEventDispatcher.onRightClickBlock(event.getEntity(), event.getLevel(),
            event.getPos(), event.getFace(), event.getItemStack());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer)) return;
        ShenEventDispatcher.onRightClickItem(event.getEntity(), event.getLevel(), event.getItemStack());
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventDispatcher.onAttack(player, event.getTarget());
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ShenEventDispatcher.onChat(event.getPlayer(), event.getMessage().getString());
    }

    // ============ 受伤 / 死亡 ============
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ShenEventDispatcher.onLivingHurt(event.getEntity());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ShenEventDispatcher.onLivingDeath(event.getEntity());
    }

    // ============ 无敌 ============
    @SubscribeEvent
    public void onInvincibleAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ShenEventDispatcher.isInvincible(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInvincibleHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ShenEventDispatcher.isInvincible(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInvincibleDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ShenEventDispatcher.isInvincible(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInvincibleDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ShenEventDispatcher.isInvincible(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInvincibleKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ShenEventDispatcher.isInvincible(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInvincibleProjectile(ProjectileImpactEvent event) {
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit)) return;
        Entity hit = entityHit.getEntity();
        if (hit.level().isClientSide()) return;
        if (!(hit instanceof LivingEntity living) || !ShenEventDispatcher.isInvincible(living)) return;
        event.getProjectile().setDeltaMovement(event.getProjectile().getDeltaMovement().scale(-1.1));
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
    }

    // ============ 重生 / 睡眠 ============
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShenEventDispatcher.onRespawn(player);
        }
    }

    @SubscribeEvent
    public void onSleepInBed(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventDispatcher.onSleepInBed(player, event.getPos());
    }

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventDispatcher.onWakeUp(player);
    }

    // ============ 进入 / 离开世界 ============
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GodBootstrap.onPlayerLoggedIn(player);
            ShenEventDispatcher.onPlayerLoggedIn(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GodBootstrap.onPlayerLoggedOut(player);
            ShenEventDispatcher.onPlayerLoggedOut(player);
        }
    }

    // ============ 方块 / 实体 ============
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.level().isClientSide()) return;
        ShenEventDispatcher.onBlockBreak(player, (Level) event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        ShenEventDispatcher.onEntityJoinLevel(event.getLevel(), event.getEntity());
    }

    // ============ 服务端 tick ============
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GodBootstrap.onServerTick(event.getServer());
    }
}
