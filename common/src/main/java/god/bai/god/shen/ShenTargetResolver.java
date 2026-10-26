package god.bai.god.shen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShenTargetResolver {

    public static List<LivingEntity> resolve(ShenTargetType targetType, ShenEvent event, ShenRule rule) {
        List<LivingEntity> targets = new ArrayList<>();

        switch (targetType) {
            case EVENT_SOURCE:
                if (event.source != null) {
                    targets.add(event.source);
                }
                break;

            case ATTACKED_TARGET:
                if (event.target != null) {
                    targets.add(event.target);
                }
                break;

            case TYPE_FILTER:
                resolveTypeFilter(targets, event, rule);
                break;

            case NEARBY_RANGE:
                resolveNearbyRange(targets, event, rule);
                break;

            case LINE_OF_SIGHT:
                resolveLineOfSight(targets, event, rule);
                break;

            case SPECIFIC_ENTITY:
                resolveSpecificEntity(targets, event, rule);
                break;

            case ALL_PLAYERS:
                resolveAllPlayers(targets, event, rule);
                break;

            case RANDOM_ONE:
                resolveRandomOne(targets, event, rule);
                break;

            case STRUCTURE_BOUNDS:
                resolveStructureBounds(targets, event, rule);
                break;

            case REGION:
                resolveRegion(targets, event, rule);
                break;
        }

        return targets;
    }

    private static void resolveAllPlayers(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.world != null && event.world.getServer() != null) {
            for (ServerPlayer player : event.world.getServer().getPlayerList().getPlayers()) {
                targets.add(player);
            }
        }
    }

    private static void resolveRandomOne(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        List<LivingEntity> pool = new ArrayList<>();
        if (event.world == null || event.pos == null) return;
        double range = rule.getParamDouble("range", 50.0);
        String entityType = rule.getParam("entityType", "all");

        AABB box = new AABB(event.pos).inflate(range);
        for (Entity entity : event.world.getEntities((Entity) null, box)) {
            if (entity instanceof LivingEntity living) {
                if (entity == event.source) continue;
                if (matchesEntityType(living, entityType)) {
                    pool.add(living);
                }
            }
        }

        if (!pool.isEmpty()) {
            java.util.Random rand = new java.util.Random();
            targets.add(pool.get(rand.nextInt(pool.size())));
        }
    }

    private static void resolveTypeFilter(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
    if (event.world == null) return;
    String entityType = rule.getParam("entityType", "all");
    double range = rule.getParamDouble("range", 50);
    boolean includeSelf = rule.getParamBool("includeSelf", false);

    AABB box = new AABB(event.pos).inflate(range);
    for (Entity entity : event.world.getEntities((Entity) null, box)) {
        if (entity instanceof LivingEntity living) {
            if (!includeSelf && living == event.source) continue;
            if (matchesEntityType(living, entityType)) {
                targets.add(living);
            }
        }
    }
}

    private static boolean matchesEntityType(LivingEntity entity, String typeFilter) {
        if (typeFilter.equals("all")) return true;
        if (typeFilter.equals("player")) return entity instanceof Player;
        if (typeFilter.equals("monster")) return entity instanceof Monster;
        if (typeFilter.equals("animal")) return entity instanceof Animal;

        ResourceLocation filterId = ResourceLocation.tryParse(typeFilter);
        if (filterId != null) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return filterId.equals(entityId);
        }
        return false;
    }

    private static void resolveNearbyRange(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.world == null || event.pos == null) return;
        double range = rule.getParamDouble("range", 10.0);
        String entityType = rule.getParam("entityType", "all");
        boolean includeSource = rule.getParamBool("includeSource", false);

        AABB box = new AABB(event.pos).inflate(range);
        for (Entity entity : event.world.getEntities((Entity) null, box)) {
            if (entity instanceof LivingEntity living) {
                if (!includeSource && entity == event.source) continue;
                if (matchesEntityType(living, entityType)) {
                    targets.add(living);
                }
            }
        }
    }

    private static void resolveLineOfSight(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.source instanceof ServerPlayer player) {
            double range = rule.getParamDouble("range", 50.0);
            HitResult hitResult = player.pick(range, 0.0f, false);

            if (hitResult instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof LivingEntity target) {
                    targets.add(target);
                    return;
                }
            }

            if (event.pos != null && event.world != null) {
                AABB box = new AABB(event.pos).inflate(2.0);
                for (Entity entity : event.world.getEntities(player, box)) {
                    if (entity instanceof LivingEntity living) {
                        targets.add(living);
                        return;
                    }
                }
            }
        }
    }

    private static void resolveSpecificEntity(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.world == null) return;
        String uuidStr = rule.getParam("uuid", "");
        String playerName = rule.getParam("playerName", "");
        String entityName = rule.getParam("entityName", "");

        if (!uuidStr.isEmpty()) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (event.world.getServer() != null) {
                    for (ServerPlayer player : event.world.getServer().getPlayerList().getPlayers()) {
                        if (player.getUUID().equals(uuid)) {
                            targets.add(player);
                            break;
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        if (!playerName.isEmpty()) {
            if (event.world.getServer() != null) {
                ServerPlayer namedPlayer = event.world.getServer().getPlayerList().getPlayerByName(playerName);
                if (namedPlayer != null) {
                    targets.add(namedPlayer);
                }
            }
        }

        if (!entityName.isEmpty()) {
            ResourceLocation targetId = ResourceLocation.tryParse(entityName);
            if (targetId != null && event.world.getServer() != null) {
                for (ServerLevel sw : event.world.getServer().getAllLevels()) {
                    for (Entity entity : sw.getAllEntities()) {
                        if (entity instanceof LivingEntity living
                            && targetId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()))) {
                            targets.add(living);
                        }
                    }
                }
            }
        }
    }

    private static void resolveStructureBounds(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.world == null || !(event.world instanceof ServerLevel serverWorld)) return;
        String structureId = rule.getParam("structureId", "");
        double range = rule.getParamDouble("range", 200);
        String entityType = rule.getParam("entityType", "all");

        if (structureId.isEmpty()) return;
        ResourceLocation expectedId = ResourceLocation.tryParse(structureId);
        BlockPos center = event.pos != null ? event.pos : (event.source != null ? event.source.blockPosition() : BlockPos.ZERO);

        int chunkRange = (int) Math.ceil(range / 16.0) + 1;
        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                LevelChunk chunk = serverWorld.getChunk(
                    (center.getX() + dx * 16) >> 4,
                    (center.getZ() + dz * 16) >> 4);
                var starts = chunk.getAllStarts();

                for (var entry : starts.entrySet()) {
                    Structure structure = entry.getKey();
                    ResourceLocation id = serverWorld.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE).getKey(structure);

                    boolean matches = expectedId != null && id != null && id.equals(expectedId);
                    if (!matches && id != null) {
                        matches = id.toString().contains(structureId);
                    }

                    if (matches) {
                        var start = entry.getValue();
                        BoundingBox bb = start.getBoundingBox();
                        AABB boundingBox = new AABB(
                            bb.minX(), bb.minY(), bb.minZ(),
                            bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);
                        for (Entity entity : event.world.getEntities((Entity) null, boundingBox)) {
                            if (entity instanceof LivingEntity living && living != event.source
                                    && matchesEntityType(living, entityType)) {
                                targets.add(living);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void resolveRegion(List<LivingEntity> targets, ShenEvent event, ShenRule rule) {
        if (event.world == null) return;

        // 读取 GUI 参数：X/Y/Z 轴半径 + 中心模式（事件位置 / 事件源）
        double radiusX = rule.getParamDouble("radiusX", 5.0);
        double radiusY = rule.getParamDouble("radiusY", 5.0);
        double radiusZ = rule.getParamDouble("radiusZ", 5.0);
        String centerMode = rule.getParam("centerMode", "eventPos");

        // 确定区域中心
        BlockPos center;
        if ("eventSource".equals(centerMode) && event.source != null) {
            center = event.source.blockPosition();
        } else if (event.pos != null) {
            center = event.pos;
        } else if (event.source != null) {
            center = event.source.blockPosition();
        } else {
            return;
        }

        // 按半径构建区域包围盒，检索区域内所有生物作为目标
        AABB box = new AABB(center).inflate(radiusX, radiusY, radiusZ);
        for (Entity entity : event.world.getEntities((Entity) null, box)) {
            if (entity instanceof LivingEntity living && living != event.source) {
                targets.add(living);
            }
        }
    }
}
