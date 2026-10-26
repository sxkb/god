package god.bai.god.shen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 言出法随事件分发器（与加载器无关）。
 *
 * <p>Fabric / Forge 各自的事件适配器只负责把平台事件转成这里的调用，
 * 真正的判定与触发逻辑全部集中在这一个类里，保证两端行为一致。</p>
 */
public final class ShenEventDispatcher {

    private static long lastTimeOfDay = -1;
    private static boolean lastRaining = false;
    private static boolean lastThundering = false;
    private static final Map<UUID, ResourceKey<Level>> lastDimensionMap = new HashMap<>();
    private static final Map<UUID, Boolean> sneakState = new HashMap<>();
    private static final Map<UUID, Boolean> groundState = new HashMap<>();

    private ShenEventDispatcher() {
    }

    public static void onEntityInteract(Player player, Level world, Entity entity) {
        if (world.isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventBus.fire(ShenEventType.PLAYER_RIGHT_CLICK,
            player, player.blockPosition(), world);
        if (entity instanceof LivingEntity livingEntity) {
            ShenEventBus.fire(new ShenEvent(ShenEventType.ENTITY_RIGHT_CLICK,
                player, livingEntity, entity.blockPosition(), world));
        }
    }

    public static void onRightClickBlock(Player player, Level world, BlockPos pos, Direction face, ItemStack stack) {
        if (world.isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventBus.fire(ShenEventType.PLAYER_RIGHT_CLICK, player, pos, world);
        ShenEventBus.fire(ShenEventType.BLOCK_RIGHT_CLICK, player, pos, world);
        ShenEventBus.fire(ShenEventType.BLOCK_INTERACT, player, pos, world);

        if (stack.getItem() instanceof BlockItem) {
            BlockPos placedPos = pos.relative(face);
            ShenEventBus.fire(ShenEventType.BLOCK_PLACE, player, placedPos, world);
        }
    }

    public static void onAttack(Player player, Entity entity) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        if (entity instanceof LivingEntity livingTarget) {
            ShenEventBus.fire(new ShenEvent(ShenEventType.PLAYER_ATTACK,
                player, livingTarget, entity.blockPosition(), player.level()));
        }
    }

    public static void onChat(ServerPlayer sender, String chatText) {
        if (sender == null) return;
        ShenEventBus.fire(new ShenEvent(ShenEventType.PLAYER_CHAT,
            sender, null, sender.blockPosition(), sender.level(), chatText));
    }

    public static void onRightClickItem(Player player, Level world, ItemStack stack) {
        if (world.isClientSide() || !(player instanceof ServerPlayer)) return;
        if (stack.isEdible()) {
            ShenEventBus.fire(ShenEventType.PLAYER_EAT, player, player.blockPosition(), world);
        }
        ShenEventBus.fire(ShenEventType.ITEM_USE, player, player.blockPosition(), world);
    }

    public static void onLivingHurt(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer player) {
            ShenEventBus.fire(ShenEventType.PLAYER_HURT, player, player.blockPosition(), player.level());
        } else {
            ShenEventBus.fire(ShenEventType.ENTITY_HURT, entity, entity.blockPosition(), entity.level());
        }
    }

    public static void onLivingDeath(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer player) {
            ShenEventBus.fire(ShenEventType.PLAYER_DEATH, player, player.blockPosition(), player.level());
        } else {
            ShenEventBus.fire(ShenEventType.ENTITY_DEATH, entity, entity.blockPosition(), entity.level());
        }
    }

    /** 无敌目标不应受到任何伤害 / 死亡 / 击退 / 弹射物影响 */
    public static boolean isInvincible(LivingEntity entity) {
        return entity != null && ShenEffectRegistry.isInvincible(entity);
    }

    public static void onRespawn(ServerPlayer player) {
        ShenEventBus.fire(ShenEventType.PLAYER_RESPAWN, player, player.blockPosition(), player.level());
    }

    public static void onSleepInBed(Player player, BlockPos pos) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventBus.fire(ShenEventType.PLAYER_BED_ENTER, player, pos, player.level());
    }

    public static void onWakeUp(Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) return;
        ShenEventBus.fire(ShenEventType.PLAYER_BED_LEAVE, player, player.blockPosition(), player.level());
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        ShenEventBus.fire(ShenEventType.PLAYER_JOIN_WORLD, player, player.blockPosition(), player.level());
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        ShenEventBus.fire(ShenEventType.PLAYER_LEAVE_WORLD, player, player.blockPosition(), player.level());
    }

    public static void onBlockBreak(Player player, Level world, BlockPos pos) {
        if (player != null && player.level().isClientSide()) return;
        ShenEventBus.fire(ShenEventType.BLOCK_BREAK, player, pos, world);
    }

    public static void onEntityJoinLevel(Level world, Entity entity) {
        if (world.isClientSide()) return;
        if (!(entity instanceof ServerPlayer) && entity instanceof LivingEntity living) {
            ShenEventBus.fire(ShenEventType.ENTITY_SPAWN,
                living, entity.blockPosition(), world);
        }
    }

    // ============ 服务端 tick（轮询类事件） ============
    public static void serverTick(MinecraftServer server) {
        if (server == null) return;

        ShenDelayManager.tick();

        if (server.getTickCount() % 20 == 0) {
            for (ServerLevel world : server.getAllLevels()) {
                long currentTime = world.getDayTime();
                long timeDiff = currentTime - lastTimeOfDay;
                if (lastTimeOfDay != -1 && Math.abs(timeDiff) >= 100) {
                    ServerPlayer nearestPlayer = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        if (p.level() == world) {
                            double dist = p.blockPosition().distSqr(BlockPos.ZERO);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearestPlayer = p;
                            }
                        }
                    }
                    if (nearestPlayer != null) {
                        ShenEventBus.fire(ShenEventType.WORLD_TIME_CHANGE,
                            nearestPlayer, nearestPlayer.blockPosition(), world);
                    }
                }
                lastTimeOfDay = currentTime;

                boolean raining = world.isRaining();
                boolean thundering = world.isThundering();
                if ((raining != lastRaining || thundering != lastThundering) && lastRaining) {
                    ServerPlayer nearestPlayer = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        if (p.level() == world) {
                            double dist = p.blockPosition().distSqr(BlockPos.ZERO);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearestPlayer = p;
                            }
                        }
                    }
                    if (nearestPlayer != null) {
                        ShenEventBus.fire(ShenEventType.WORLD_WEATHER_CHANGE,
                            nearestPlayer, nearestPlayer.blockPosition(), world);
                    }
                }
                lastRaining = raining;
                lastThundering = thundering;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ResourceKey<Level> currentDim = player.level().dimension();
                ResourceKey<Level> lastDim = lastDimensionMap.get(player.getUUID());
                if (lastDim != null && !lastDim.equals(currentDim)) {
                    ShenEventBus.fire(ShenEventType.PLAYER_DIMENSION_CHANGE,
                        player, player.blockPosition(), player.level());
                }
                lastDimensionMap.put(player.getUUID(), currentDim);
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ItemEntity nearestItem = null;
                double nearestDist = 2.0;
                for (Entity e : player.level().getEntities(player,
                        player.getBoundingBox().inflate(2.0))) {
                    if (e instanceof ItemEntity item && e.isAlive()) {
                        double dist = e.distanceTo(player);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestItem = item;
                        }
                    }
                }
                if (nearestItem != null) {
                    ShenEventBus.fire(ShenEventType.PLAYER_ITEM_PICKUP,
                        player, player.blockPosition(), player.level());
                }
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean isSneaking = player.isShiftKeyDown();
            Boolean wasSneaking = sneakState.get(player.getUUID());
            if (wasSneaking != null && isSneaking != wasSneaking && isSneaking) {
                ShenEventBus.fire(ShenEventType.PLAYER_SNEAK,
                    player, player.blockPosition(), player.level());
            }
            sneakState.put(player.getUUID(), isSneaking);

            boolean isOnGround = player.onGround();
            Boolean wasOnGround = groundState.get(player.getUUID());
            if (wasOnGround != null && wasOnGround && !isOnGround) {
                if (player.getDeltaMovement().y > 0) {
                    ShenEventBus.fire(ShenEventType.PLAYER_JUMP,
                        player, player.blockPosition(), player.level());
                }
            }
            groundState.put(player.getUUID(), isOnGround);

            if (server.getTickCount() % 20 == 0) {
                god.bai.god.shen.compat.ModAdapterManager.checkEntityNbtChanges(player);
            }
        }
    }
}
