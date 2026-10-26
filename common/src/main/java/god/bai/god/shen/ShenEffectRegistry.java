package god.bai.god.shen;

import god.bai.god.support.ForceTeleportHelper;
import god.bai.god.support.smfs.SmfsAPIWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ShenEffectRegistry {
    private static final Map<ShenEffectType, BiConsumer<ShenEvent, List<LivingEntity>>> EXECUTORS = new HashMap<>();

    /** 判断实体是否处于无敌状态（供伤害/死亡事件拦截查询；状态持久化于存档） */
    public static boolean isInvincible(Entity entity) {
        if (entity == null) return false;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return false;
        return ShenInvincibleStore.get(serverLevel).contains(entity.getUUID());
    }

    static {
        EXECUTORS.put(ShenEffectType.MODIFY_HEALTH, ShenEffectRegistry::executeModifyHealth);
        EXECUTORS.put(ShenEffectType.DEAL_DAMAGE, ShenEffectRegistry::executeDealDamage);
        EXECUTORS.put(ShenEffectType.APPLY_EFFECT, ShenEffectRegistry::executeApplyEffect);
        EXECUTORS.put(ShenEffectType.CLEAR_EFFECT, ShenEffectRegistry::executeClearEffect);
        EXECUTORS.put(ShenEffectType.MODIFY_NBT, ShenEffectRegistry::executeModifyNbt);
        EXECUTORS.put(ShenEffectType.MODIFY_AI, ShenEffectRegistry::executeModifyAi);
        EXECUTORS.put(ShenEffectType.KILL_ENTITY, ShenEffectRegistry::executeKillEntity);
        EXECUTORS.put(ShenEffectType.SUMMON_ENTITY, ShenEffectRegistry::executeSummonEntity);
        EXECUTORS.put(ShenEffectType.TELEPORT, ShenEffectRegistry::executeTeleport);
        EXECUTORS.put(ShenEffectType.SET_FIRE, ShenEffectRegistry::executeSetFire);
        EXECUTORS.put(ShenEffectType.EXPLOSION, ShenEffectRegistry::executeExplosion);
        EXECUTORS.put(ShenEffectType.GIVE_ITEM, ShenEffectRegistry::executeGiveItem);
        EXECUTORS.put(ShenEffectType.REMOVE_ITEM, ShenEffectRegistry::executeRemoveItem);
        EXECUTORS.put(ShenEffectType.MODIFY_ITEM_NBT, ShenEffectRegistry::executeModifyItemNbt);
        EXECUTORS.put(ShenEffectType.COPY_ITEM, ShenEffectRegistry::executeCopyItem);
        EXECUTORS.put(ShenEffectType.PLACE_BLOCK, ShenEffectRegistry::executePlaceBlock);
        EXECUTORS.put(ShenEffectType.BREAK_BLOCK, ShenEffectRegistry::executeBreakBlock);
        EXECUTORS.put(ShenEffectType.MODIFY_BLOCK_STATE, ShenEffectRegistry::executeModifyBlockState);
        EXECUTORS.put(ShenEffectType.MODIFY_TIME, ShenEffectRegistry::executeModifyTime);
        EXECUTORS.put(ShenEffectType.MODIFY_WEATHER, ShenEffectRegistry::executeModifyWeather);
        EXECUTORS.put(ShenEffectType.MODIFY_GAME_RULE, ShenEffectRegistry::executeModifyGameRule);
        EXECUTORS.put(ShenEffectType.SPAWN_PARTICLE, ShenEffectRegistry::executeSpawnParticle);
        EXECUTORS.put(ShenEffectType.PLAY_SOUND, ShenEffectRegistry::executePlaySound);
        EXECUTORS.put(ShenEffectType.SEND_MESSAGE, ShenEffectRegistry::executeSendMessage);
        EXECUTORS.put(ShenEffectType.EXECUTE_COMMAND, ShenEffectRegistry::executeCommand);
        EXECUTORS.put(ShenEffectType.REPLACE_ITEM, ShenEffectRegistry::executeReplaceItem);
        EXECUTORS.put(ShenEffectType.ENCHANT_ITEM, ShenEffectRegistry::executeEnchantItem);
        EXECUTORS.put(ShenEffectType.SPAWN_EXPERIENCE, ShenEffectRegistry::executeSpawnExperience);
        EXECUTORS.put(ShenEffectType.RUN_OTHER_RULE, ShenEffectRegistry::executeRunOtherRule);
        EXECUTORS.put(ShenEffectType.SET_VARIABLE, ShenEffectRegistry::executeSetVariable);
        EXECUTORS.put(ShenEffectType.MODIFY_VARIABLE, ShenEffectRegistry::executeModifyVariable);
        EXECUTORS.put(ShenEffectType.SET_GAMEMODE, ShenEffectRegistry::executeSetGamemode);
        EXECUTORS.put(ShenEffectType.CLEAR_INVENTORY, ShenEffectRegistry::executeClearInventory);
        EXECUTORS.put(ShenEffectType.CLEAR_ITEM, ShenEffectRegistry::executeClearItem);
        EXECUTORS.put(ShenEffectType.LOOP, ShenEffectRegistry::executeLoop);
        EXECUTORS.put(ShenEffectType.SET_FLYING, ShenEffectRegistry::executeSetFlying);
        EXECUTORS.put(ShenEffectType.SET_WALK_SPEED, ShenEffectRegistry::executeSetWalkSpeed);
        EXECUTORS.put(ShenEffectType.SET_FLY_SPEED, ShenEffectRegistry::executeSetFlySpeed);
        EXECUTORS.put(ShenEffectType.MODIFY_MAX_HEALTH, ShenEffectRegistry::executeModifyMaxHealth);
        EXECUTORS.put(ShenEffectType.REPAIR_ITEM, ShenEffectRegistry::executeRepairItem);
        EXECUTORS.put(ShenEffectType.SPAWN_LIGHTNING, ShenEffectRegistry::executeSpawnLightning);
        EXECUTORS.put(ShenEffectType.SET_INVISIBLE, ShenEffectRegistry::executeSetInvisible);
        EXECUTORS.put(ShenEffectType.SET_INVINCIBLE, ShenEffectRegistry::executeSetInvincible);
        EXECUTORS.put(ShenEffectType.SATURATE_FOOD, ShenEffectRegistry::executeSaturateFood);
        EXECUTORS.put(ShenEffectType.SET_STEP_HEIGHT, ShenEffectRegistry::executeSetStepHeight);
        EXECUTORS.put(ShenEffectType.SET_RESISTANCE, ShenEffectRegistry::executeSetResistance);
        EXECUTORS.put(ShenEffectType.SWAP_HAND_ITEMS, ShenEffectRegistry::executeSwapHandItems);
        EXECUTORS.put(ShenEffectType.RANDOM_TELEPORT, ShenEffectRegistry::executeRandomTeleport);
        EXECUTORS.put(ShenEffectType.SET_AIR, ShenEffectRegistry::executeSetAir);
        EXECUTORS.put(ShenEffectType.MODIFY_ATTRIBUTE, ShenEffectRegistry::executeModifyAttribute);
        EXECUTORS.put(ShenEffectType.FREEZE_ENTITY, ShenEffectRegistry::executeFreezeEntity);
        EXECUTORS.put(ShenEffectType.PLACE_STRUCTURE, ShenEffectRegistry::executePlaceStructure);
        EXECUTORS.put(ShenEffectType.MASS_REPLACE_BLOCKS, ShenEffectRegistry::executeMassReplaceBlocks);
        EXECUTORS.put(ShenEffectType.CLEAVE_LAND, ShenEffectRegistry::executeCleaveLand);
        EXECUTORS.put(ShenEffectType.MOD_ADAPTER_EFFECT, ShenEffectRegistry::executeModAdapterEffect);

        EXECUTORS.put(ShenEffectType.SMFS_SET_SPIRIT_RESISTANCE, ShenEffectRegistry::executeSmfsSetSpiritResistance);
        EXECUTORS.put(ShenEffectType.SMFS_SET_SPIRIT_DAMAGE, ShenEffectRegistry::executeSmfsSetSpiritDamage);
        EXECUTORS.put(ShenEffectType.SMFS_SET_CURRENT_SPIRIT, ShenEffectRegistry::executeSmfsSetCurrentSpirit);
        EXECUTORS.put(ShenEffectType.SMFS_SET_MAX_SPIRIT, ShenEffectRegistry::executeSmfsSetMaxSpirit);
        EXECUTORS.put(ShenEffectType.SMFS_SET_SANITY, ShenEffectRegistry::executeSmfsSetSanity);
        EXECUTORS.put(ShenEffectType.SMFS_SET_REVIVAL_FACTOR, ShenEffectRegistry::executeSmfsSetRevivalFactor);
        EXECUTORS.put(ShenEffectType.SMFS_SET_MAIN_GHOST_SLOT, ShenEffectRegistry::executeSmfsSetMainGhostSlot);
        EXECUTORS.put(ShenEffectType.SMFS_CYCLE_MAIN_GHOST, ShenEffectRegistry::executeSmfsCycleMainGhost);
        EXECUTORS.put(ShenEffectType.SMFS_SET_NBT_ATTRIBUTE, ShenEffectRegistry::executeSmfsSetNbtAttribute);

        EXECUTORS.put(ShenEffectType.SMFS_ACTIVATE_DOMAIN, ShenEffectRegistry::executeSmfsActivateDomain);
        EXECUTORS.put(ShenEffectType.SMFS_SET_DOMAIN_TYPE, ShenEffectRegistry::executeSmfsSetDomainType);
        EXECUTORS.put(ShenEffectType.SMFS_SET_DOMAIN_LEVEL, ShenEffectRegistry::executeSmfsSetDomainLevel);

        EXECUTORS.put(ShenEffectType.SMFS_SET_GHOST_EYE_LEVEL, ShenEffectRegistry::executeSmfsSetGhostEyeLevel);
        EXECUTORS.put(ShenEffectType.SMFS_TOGGLE_GHOST_EYE, ShenEffectRegistry::executeSmfsToggleGhostEye);

        EXECUTORS.put(ShenEffectType.SMFS_LOCK_SLOT, ShenEffectRegistry::executeSmfsLockSlot);
        EXECUTORS.put(ShenEffectType.SMFS_CLEAR_SLOT, ShenEffectRegistry::executeSmfsClearSlot);
        EXECUTORS.put(ShenEffectType.SMFS_SWAP_SLOTS, ShenEffectRegistry::executeSmfsSwapSlots);
        EXECUTORS.put(ShenEffectType.SMFS_SET_SLOT_GHOST, ShenEffectRegistry::executeSmfsSetSlotGhost);
    }

    public static void execute(ShenEffectType type, ShenEvent event, List<LivingEntity> targets, ShenRule rule) {
        BiConsumer<ShenEvent, List<LivingEntity>> executor = EXECUTORS.get(type);
        if (executor != null) {
            event.rule = rule;
            executor.accept(event, targets);
            event.rule = null;
        }
    }

    public static void execute(ShenRule.ShenEffectEntry entry, ShenEvent event, List<LivingEntity> targets) {
        BiConsumer<ShenEvent, List<LivingEntity>> executor = EXECUTORS.get(entry.effectType);
        if (executor != null) {
            // 创建临时规则，使用效果步骤的参数
            ShenRule tempRule = new ShenRule();
            tempRule.params = entry.params;
            event.rule = tempRule;
            executor.accept(event, targets);
            event.rule = null;
        }
    }

    private static void executeModifyHealth(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String targetType = rule.getParam("target", "current");
        for (LivingEntity target : targets) {
            float amount = (float) rule.getParamDouble("amount", target.getMaxHealth());
            boolean percent = rule.getParamBool("percent", false);
            float finalAmount = percent ? target.getMaxHealth() * amount / 100f : amount;

            if ("max".equals(targetType)) {
                net.minecraft.world.entity.ai.attributes.Attribute maxHealthAttr =
                    BuiltInRegistries.ATTRIBUTE.get(
                        ResourceLocation.tryParse("minecraft:generic.max_health"));
                if (maxHealthAttr != null) {
                    net.minecraft.world.entity.ai.attributes.AttributeInstance attr = target.getAttribute(maxHealthAttr);
                    if (attr != null) {
                        float clamped = Math.max(0, Math.min(finalAmount, 100000));
                        attr.setBaseValue(clamped);
                        if (target.getHealth() > target.getMaxHealth()) {
                            target.setHealth(target.getMaxHealth());
                        }
                    }
                }
            } else {
                target.setHealth(Math.max(0, Math.min(target.getMaxHealth(), finalAmount)));
            }
        }
    }

    private static void executeDealDamage(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            float amount = (float) rule.getParamDouble("amount", 10.0);
            String damageTypeId = rule.getParam("damageType", "minecraft:generic");
            ResourceLocation dtId = ResourceLocation.tryParse(damageTypeId);
            if (dtId == null) dtId = new ResourceLocation("minecraft", "generic");

            if ("smfs:ghost".equals(dtId.toString())) {
                LivingEntity attacker = event.source != null ? event.source : target;
                SmfsAPIWrapper.dealSpiritDamage(target, amount, attacker);
                continue;
            }

            try {
                ResourceKey<DamageType> dtKey = ResourceKey.create(Registries.DAMAGE_TYPE, dtId);
                var registryEntry = ((ServerLevel) target.level())
                    .registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(dtKey);
                DamageSource source = new DamageSource(registryEntry, event.source, event.source);
                target.hurt(source, amount);
            } catch (Exception e) {
                // 安全兜底 — 如果是灵异伤害兜底也走灵异通道
                if ("smfs:ghost".equals(dtId.toString())) {
                    LivingEntity attacker = event.source != null ? event.source : target;
                    SmfsAPIWrapper.dealSpiritDamage(target, amount, attacker);
                    continue;
                }
                DamageSource fallback = (event.source != null)
                    ? target.damageSources().mobAttack(event.source)
                    : target.damageSources().generic();
                target.hurt(fallback, amount);
            }
        }
    }

    private static void executeApplyEffect(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String effectId = rule.getParam("effect", "minecraft:glowing");
        int duration = rule.getParamInt("duration", 200);
        int amplifier = rule.getParamInt("amplifier", 0);
        boolean showParticles = rule.getParamBool("showParticles", true);

        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectId));
        if (effect == null) {
            effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation("minecraft:glowing"));
        }
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, showParticles));
        }
    }

    private static void executeClearEffect(ShenEvent event, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            target.removeAllEffects();
        }
    }

    private static void executeModifyNbt(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String nbtMode = rule.getParam("nbtMode", "modify");
        String nbtKey = rule.getParam("nbtKey", "");
        String nbtValue = rule.getParam("nbtValue", "");
        String nbtType = rule.getParam("nbtType", "string");

        for (LivingEntity target : targets) {
            CompoundTag nbt = new CompoundTag();
            target.saveWithoutId(nbt);

            if (!nbtKey.isEmpty()) {
                if ("delete".equals(nbtMode)) {
                    nbt.remove(nbtKey);
                } else {
                    switch (nbtType) {
                        case "int":
                            try { nbt.putInt(nbtKey, Integer.parseInt(nbtValue)); } catch (NumberFormatException ignored) {}
                            break;
                        case "double":
                            try { nbt.putDouble(nbtKey, Double.parseDouble(nbtValue)); } catch (NumberFormatException ignored) {}
                            break;
                        case "boolean":
                            nbt.putBoolean(nbtKey, "true".equalsIgnoreCase(nbtValue));
                            break;
                        case "compound":
                            try {
                                CompoundTag parsed = TagParser.parseTag(nbtValue);
                                nbt.put(nbtKey, parsed);
                            } catch (Exception ignored) {}
                            break;
                        default:
                            nbt.putString(nbtKey, nbtValue);
                            break;
                    }
                }
            }
            target.load(nbt);
        }
    }

    private static void executeModifyAi(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean setAi = rule.getParamBool("ai", false);
        for (LivingEntity target : targets) {
            CompoundTag nbt = new CompoundTag();
            target.saveWithoutId(nbt);
            nbt.putBoolean("NoAI", !setAi);
            target.load(nbt);
        }
    }

    private static void executeKillEntity(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean discard = rule.getParamBool("discard", false);
        for (LivingEntity target : targets) {
            if (discard) {
                target.discard();
            } else {
                target.kill();
            }
        }
    }

    private static void executeSummonEntity(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        String entityTypeId = rule.getParam("entityType", "minecraft:lightning_bolt");
        int count = rule.getParamInt("count", 1);

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityTypeId));
        if (entityType == null) return;

        for (LivingEntity target : targets) {
            Vec3 centerPos = target.position();
            for (int i = 0; i < count; i++) {
                Entity entity = entityType.create(serverWorld);
                if (entity == null) continue;
                double offsetX = (serverWorld.random.nextDouble() - 0.5) * 2;
                double offsetZ = (serverWorld.random.nextDouble() - 0.5) * 2;
                entity.setPos(centerPos.x + offsetX, centerPos.y, centerPos.z + offsetZ);
                serverWorld.addFreshEntity(entity);
            }
        }
    }

    private static void executeTeleport(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        double x = rule.getParamDouble("x", 0);
        double y = rule.getParamDouble("y", 64);
        double z = rule.getParamDouble("z", 0);
        boolean relative = rule.getParamBool("relative", false);
        boolean switchDimension = rule.getParamBool("switchDimension", false);
        String dimensionId = rule.getParam("dimension", "minecraft:overworld");

        ServerLevel targetWorld = serverWorld;
        if (switchDimension && !dimensionId.isEmpty()) {
            ResourceKey<Level> dimKey =
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
            ServerLevel dimWorld = serverWorld.getServer().getLevel(dimKey);
            if (dimWorld != null) {
                targetWorld = dimWorld;
            }
        }

        for (LivingEntity target : targets) {
            double tx = relative ? target.getX() + x : x;
            double ty = relative ? target.getY() + y : y;
            double tz = relative ? target.getZ() + z : z;
            if (target instanceof ServerPlayer player) {
                ForceTeleportHelper.setForceTeleport(true);
                player.teleportTo(targetWorld, tx, ty, tz, player.getYRot(), player.getXRot());
                ForceTeleportHelper.setForceTeleport(false);
            } else {
                target.teleportTo(tx, ty, tz);
            }
        }
    }

    private static void executeSetFire(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            int ticks = (int) rule.resolveNumber("ticks", 100, target.getRemainingFireTicks());
            // 原 Fabric 的 setOnFireFor(ticks) 语义为“秒”，内部 *20；这里保持同等行为
            target.setRemainingFireTicks(ticks * 20);
        }
    }

    private static void executeExplosion(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        float power = (float) rule.getParamDouble("power", 4.0);
        boolean destroyBlocks = rule.getParamBool("destroyBlocks", true);
        boolean createFire = rule.getParamBool("createFire", false);
        boolean atImpact = rule.getParamBool("atImpact", false);

        var explosionType = destroyBlocks
            ? Level.ExplosionInteraction.TNT
            : Level.ExplosionInteraction.NONE;

        if (atImpact) {
            BlockPos eventPos = event.pos;
            serverWorld.explode(event.source, eventPos.getX() + 0.5, eventPos.getY(), eventPos.getZ() + 0.5,
                power, createFire, explosionType);
            return;
        }

        for (LivingEntity target : targets) {
            Vec3 pos = target.position();
            serverWorld.explode(
                event.source, pos.x, pos.y, pos.z, power, createFire, explosionType);
        }
    }

    private static void executeGiveItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String itemId = rule.getParam("item", "minecraft:diamond");
        int count = rule.getParamInt("count", 1);
        String nbtStr = rule.getParam("itemNbt", "");

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)), count);
        if (!nbtStr.isEmpty()) {
            try {
                stack.setTag(TagParser.parseTag(nbtStr));
            } catch (Exception ignored) {}
        }

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                player.getInventory().placeItemBackInInventory(stack.copy());
            }
        }
    }

    private static void executeRemoveItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String itemId = rule.getParam("item", "");
        int count = rule.getParamInt("count", 1);

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                if (itemId.isEmpty()) {
                    player.getMainHandItem().shrink(count);
                } else {
                    net.minecraft.world.item.Item targetItem = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                    if (targetItem != null) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(targetItem)) {
                                stack.shrink(count);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void executeModifyItemNbt(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String nbtKey = rule.getParam("nbtKey", "");
        String nbtValue = rule.getParam("nbtValue", "");
        String nbtType = rule.getParam("nbtType", "string");

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && !nbtKey.isEmpty()) {
                    CompoundTag nbt = stack.getOrCreateTag();
                    switch (nbtType) {
                        case "int":
                            try { nbt.putInt(nbtKey, Integer.parseInt(nbtValue)); } catch (NumberFormatException ignored) {}
                            break;
                        case "double":
                            try { nbt.putDouble(nbtKey, Double.parseDouble(nbtValue)); } catch (NumberFormatException ignored) {}
                            break;
                        case "boolean":
                            nbt.putBoolean(nbtKey, "true".equalsIgnoreCase(nbtValue));
                            break;
                        case "compound":
                            try { nbt.put(nbtKey, TagParser.parseTag(nbtValue)); } catch (Exception ignored) {}
                            break;
                        default:
                            nbt.putString(nbtKey, nbtValue);
                            break;
                    }
                }
            }
        }
    }

    private static void executeCopyItem(ShenEvent event, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem().copy();
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }

    private static void executePlaceBlock(ShenEvent event, List<LivingEntity> targets) {
        if (event.world == null) return;
        ShenRule rule = event.rule;
        String blockId = rule.getParam("block", "minecraft:stone");

        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
        if (block == null) block = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft:stone"));

        if (event.pos != null) {
            event.world.setBlock(event.pos, block.defaultBlockState(), 3);
        }
    }

    private static void executeBreakBlock(ShenEvent event, List<LivingEntity> targets) {
        if (event.world != null && event.pos != null) {
            ShenRule rule = event.rule;
            boolean dropItems = rule.getParamBool("dropItems", true);
            event.world.destroyBlock(event.pos, dropItems);
        }
    }

    private static void executeModifyBlockState(ShenEvent event, List<LivingEntity> targets) {
        if (event.world == null || event.pos == null) return;
        ShenRule rule = event.rule;
        String blockId = rule.getParam("block", "minecraft:stone");

        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
        if (block != null) {
            event.world.setBlock(event.pos, block.defaultBlockState(), 3);
        }
    }

    private static void executeModifyTime(ShenEvent event, List<LivingEntity> targets) {
        if (event.world instanceof ServerLevel serverWorld) {
            ShenRule rule = event.rule;
            long time = (long) rule.getParamDouble("time", 1000);
            boolean absolute = rule.getParamBool("absolute", false);
            if (absolute) {
                serverWorld.setDayTime(time);
            } else {
                serverWorld.setDayTime(serverWorld.getDayTime() + time);
            }
        }
    }

    private static void executeModifyWeather(ShenEvent event, List<LivingEntity> targets) {
        if (event.world instanceof ServerLevel serverWorld) {
            ShenRule rule = event.rule;
            String weather = rule.getParam("weather", "clear");
            int duration = rule.getParamInt("duration", 6000);

            switch (weather) {
                case "rain":
                    serverWorld.setWeatherParameters(0, duration, true, false);
                    break;
                case "thunder":
                    serverWorld.setWeatherParameters(0, duration, true, true);
                    break;
                default:
                    serverWorld.setWeatherParameters(duration, 0, false, false);
                    break;
            }
        }
    }

    private static void executeModifyGameRule(ShenEvent event, List<LivingEntity> targets) {
        if (event.world instanceof ServerLevel serverWorld) {
            ShenRule rule = event.rule;
            String gameRuleName = rule.getParam("gameRule", "doDaylightCycle");
            boolean value = rule.getParamBool("value", true);

            GameRules gameRules = serverWorld.getGameRules();
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                    if (gameRuleName.equals(key.getId())) {
                        if (value) {
                            gameRules.getRule(key).set(1, serverWorld.getServer());
                        } else {
                            gameRules.getRule(key).set(0, serverWorld.getServer());
                        }
                    }
                }

                @Override
                public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                    if (gameRuleName.equals(key.getId())) {
                        gameRules.getRule(key).set(value, serverWorld.getServer());
                    }
                }
            });
        }
    }

    private static void executeSpawnParticle(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        String particleId = rule.getParam("particle", "minecraft:flame");
        int count = rule.getParamInt("count", 20);
        double spread = rule.getParamDouble("spread", 0.5);

        String colorStr = rule.getParam("color", "");
        int colorR, colorG, colorB;
        boolean hasExplicitColor = !colorStr.isEmpty();
        if (hasExplicitColor) {
            String[] parts = colorStr.split(",");
            colorR = parts.length > 0 ? clampInt(parts[0], 255) : 255;
            colorG = parts.length > 1 ? clampInt(parts[1], 0) : 0;
            colorB = parts.length > 2 ? clampInt(parts[2], 0) : 0;
        } else {
            // 兼容旧规则
            colorR = rule.getParamInt("colorR", 255);
            colorG = rule.getParamInt("colorG", 0);
            colorB = rule.getParamInt("colorB", 0);
        }
        float dustSize = (float) rule.getParamDouble("dustSize", 1.0);
        boolean isDefaultColor = "255,0,0".equals(colorStr) && dustSize == 1.0;
        boolean hasCustomColor = (hasExplicitColor && !isDefaultColor)
            || (colorR != 255 || colorG != 0 || colorB != 0 || dustSize != 1.0);

        ParticleOptions particle;

        if (hasCustomColor) {
            float r = Math.min(1.0f, Math.max(0, colorR / 255.0f));
            float g = Math.min(1.0f, Math.max(0, colorG / 255.0f));
            float b = Math.min(1.0f, Math.max(0, colorB / 255.0f));
            particle = new DustParticleOptions(new Vector3f(r, g, b), dustSize);
        } else {
            particle = ParticleTypes.FLAME;
            try {
                Object raw = BuiltInRegistries.PARTICLE_TYPE.get(new ResourceLocation(particleId));
                if (raw instanceof ParticleOptions pe) {
                    particle = pe;
                }
            } catch (Exception ignored) {}
        }

        for (LivingEntity target : targets) {
            serverWorld.sendParticles(particle,
                target.getX(), target.getY() + 1, target.getZ(),
                count, spread, spread, spread, 0.02);
        }
    }

    private static int clampInt(String str, int defaultVal) {
        try {
            return Math.max(0, Math.min(255, Integer.parseInt(str.trim())));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static void executePlaySound(ShenEvent event, List<LivingEntity> targets) {
        if (event.world == null) return;
        ShenRule rule = event.rule;
        String soundId = rule.getParam("sound", "minecraft:entity.experience_orb.pickup");
        float volume = (float) rule.getParamDouble("volume", 1.0);
        float pitch = (float) rule.getParamDouble("pitch", 1.0);

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
        if (sound == null) sound = SoundEvents.EXPERIENCE_ORB_PICKUP;

        if (event.pos != null) {
            event.world.playSound(null, event.pos, sound, SoundSource.PLAYERS, volume, pitch);
        } else {
            for (LivingEntity target : targets) {
                event.world.playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
            }
        }
    }

    private static void executeSendMessage(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String message = rule.getParam("message", "§6[言出法随] §e规则已触发！");
        boolean broadcastToAll = rule.getParamBool("broadcast", false);

        if (broadcastToAll) {
            if (event.world != null && event.world.getServer() != null) {
                for (ServerPlayer player : event.world.getServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                }
            }
        } else {
            for (LivingEntity target : targets) {
                if (target instanceof ServerPlayer player) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                }
            }
        }
    }

    private static void executeCommand(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        String command = rule.getParam("command", "say 言出法随!");
        boolean executeForEach = rule.getParamBool("executeForEach", false);

        CommandSourceStack baseSource = serverWorld.getServer().createCommandSourceStack()
            .withPosition(event.pos != null ? Vec3.atCenterOf(event.pos) : Vec3.ZERO)
            .withLevel(serverWorld);

        if (executeForEach) {
            for (LivingEntity target : targets) {
                CommandSourceStack source = baseSource.withEntity(target);
                serverWorld.getServer().getCommands().performPrefixedCommand(source, command);
            }
        } else {
            if (event.source != null) {
                baseSource = baseSource.withEntity(event.source);
            }
            serverWorld.getServer().getCommands().performPrefixedCommand(baseSource, command);
        }
    }

    private static void executeReplaceItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String removeItemId = rule.getParam("removeItem", "");
        int removeCount = rule.getParamInt("removeCount", 1);
        String giveItemId = rule.getParam("giveItem", "minecraft:diamond");
        int giveCount = rule.getParamInt("giveCount", 1);

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                if (!removeItemId.isEmpty()) {
                    net.minecraft.world.item.Item removeItem = BuiltInRegistries.ITEM.get(new ResourceLocation(removeItemId));
                    if (removeItem != null) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(removeItem)) {
                                stack.shrink(removeCount);
                                break;
                            }
                        }
                    }
                }
                ItemStack giveStack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(giveItemId)), giveCount);
                player.getInventory().placeItemBackInInventory(giveStack);
            }
        }
    }

    private static void executeEnchantItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String enchantId = rule.getParam("enchantment", "minecraft:sharpness");
        int level = rule.getParamInt("level", 1);

        net.minecraft.world.item.enchantment.Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(enchantId));
        if (enchantment == null) return;

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty()) {
                    stack.enchant(enchantment, level);
                }
            }
        }
    }

    private static void executeSpawnExperience(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        int amount = rule.getParamInt("amount", 100);

        for (LivingEntity target : targets) {
            ExperienceOrb.award(serverWorld, target.position(), amount);
        }
    }

    private static void executeRunOtherRule(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String targetRuleName = rule.getParam("targetRuleName", "");

        if (targetRuleName.isEmpty()) return;

        for (ShenLaw law : ShenEventBus.getActiveLaws()) {
            for (ShenRule targetRule : law.rules) {
                if (targetRule.name.equals(targetRuleName) || targetRule.ruleId.equals(targetRuleName)) {
                    try {
                        ShenRuleExecutor.execute(targetRule, event);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static void executeSetVariable(ShenEvent event, List<LivingEntity> targets) {
        if (event.world == null || event.world.getServer() == null) return;
        ShenRule rule = event.rule;
        String varKey = rule.getParam("varKey", "default");
        String varValue = rule.getParam("varValue", "1");
        String scope = rule.getParam("scope", "player");

        ShenVariableStore store = ShenVariableStore.get(event.world.getServer());
        if ("global".equals(scope)) {
            store.setGlobal(varKey, varValue);
        } else {
            for (LivingEntity target : targets) {
                if (target instanceof ServerPlayer player) {
                    store.setPlayer(player.getUUID(), varKey, varValue);
                }
            }
        }
    }

    private static void executeModifyVariable(ShenEvent event, List<LivingEntity> targets) {
        if (event.world == null || event.world.getServer() == null) return;
        ShenRule rule = event.rule;
        String varKey = rule.getParam("varKey", "default");
        double amount = rule.getParamDouble("amount", 1.0);
        String scope = rule.getParam("scope", "player");

        ShenVariableStore store = ShenVariableStore.get(event.world.getServer());
        if ("global".equals(scope)) {
            store.modifyGlobal(varKey, amount);
        } else {
            for (LivingEntity target : targets) {
                if (target instanceof ServerPlayer player) {
                    store.modifyPlayer(player.getUUID(), varKey, amount);
                }
            }
        }
    }

    private static void executeSetGamemode(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String modeName = rule.getParam("mode", "survival");

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                net.minecraft.world.level.GameType mode = net.minecraft.world.level.GameType.byName(modeName);
                if (mode == null) mode = net.minecraft.world.level.GameType.SURVIVAL;
                player.gameMode.changeGameModeForPlayer(mode);
            }
        }
    }

    private static void executeClearInventory(ShenEvent event, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                player.getInventory().clearContent();
            }
        }
    }

    private static void executeClearItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String itemId = rule.getParam("item", "minecraft:stone");
        int maxCount = rule.getParamInt("maxCount", -1);

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                int removed = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.getItem().toString().contains(itemId)) {
                        if (maxCount > 0 && removed >= maxCount) break;
                        int toRemove = Math.min(stack.getCount(), maxCount > 0 ? maxCount - removed : stack.getCount());
                        stack.shrink(toRemove);
                        removed += toRemove;
                    }
                }
            }
        }
    }

    private static void executeLoop(ShenEvent event, List<LivingEntity> targets) {
        // 只有首次触发时才启动循环，防止无限递归
        if (event.loopCount > 0) return;

        ShenRule rule = event.rule;
        int times = rule.getParamInt("times", 3);
        int intervalTicks = rule.getParamInt("interval", 20);

        for (int i = 1; i <= times; i++) {
            int delayTicks = intervalTicks * i;
            ShenDelayManager.schedule(delayTicks, () -> {
                if (event.source != null && !event.source.isRemoved()) {
                    // 创建带循环标记的事件，防止再次触发LOOP
                    ShenEvent loopEvent = new ShenEvent(event.type, event.source, event.target, event.pos, event.world, event.chatMessage);
                    loopEvent.loopCount = 1;
                    ShenEventBus.fire(loopEvent);
                }
            });
        }
    }

    private static void executeSetFlying(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean canFly = rule.getParamBool("value", true);
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                player.getAbilities().mayfly = canFly;
                player.onUpdateAbilities();
            }
        }
    }

    private static void executeSetWalkSpeed(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                float current = player.getAbilities().getWalkingSpeed();
                float speed = (float) rule.resolveNumber("value", 0.1, current);
                player.getAbilities().setWalkingSpeed(speed);
                player.onUpdateAbilities();
            }
        }
    }

    private static void executeSetFlySpeed(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayer player) {
                float current = player.getAbilities().getFlyingSpeed();
                float speed = (float) rule.resolveNumber("value", 0.05, current);
                player.getAbilities().setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
        }
    }

    private static void executeModifyMaxHealth(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        net.minecraft.world.entity.ai.attributes.Attribute maxHealthAttr = BuiltInRegistries.ATTRIBUTE.get(
            ResourceLocation.tryParse("minecraft:generic.max_health"));
        if (maxHealthAttr == null) return;
        for (LivingEntity target : targets) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance attr = target.getAttribute(maxHealthAttr);
            if (attr == null) continue;
            double currentBase = attr.getBaseValue();
            double newBase = rule.resolveNumber("amount", 40.0, currentBase);
            // 减法模式保底 1 点血
            if ("subtract".equals(rule.getParam("amount_op", "set"))) {
                newBase = Math.max(1.0, newBase);
            }
            attr.setBaseValue(newBase);
            if (target.getHealth() > target.getMaxHealth()) {
                target.setHealth(target.getMaxHealth());
            }
        }
    }

    private static void executeRepairItem(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int damage = rule.getParamInt("damage", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty()) {
                    if (damage <= 0) {
                        stack.setDamageValue(0);
                    } else {
                        stack.setDamageValue(Math.max(0, stack.getDamageValue() - damage));
                    }
                }
            }
        }
    }

    private static void executeSpawnLightning(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean effectOnly = rule.getParamBool("effectOnly", false);
        if (event.world == null) return;
        for (LivingEntity target : targets) {
            LightningBolt lightning =
                EntityType.LIGHTNING_BOLT.create(event.world);
            if (lightning != null) {
                lightning.setPos(target.getX(), target.getY(), target.getZ());
                if (effectOnly) {
                    lightning.setVisualOnly(true);
                }
                event.world.addFreshEntity(lightning);
            }
        }
    }

    private static void executeSetInvisible(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean invisible = rule.getParamBool("value", true);
        for (LivingEntity target : targets) {
            target.setInvisible(invisible);
        }
    }

    private static void executeSetInvincible(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean invincible = rule.getParamBool("value", true);
        if (!(event.world instanceof ServerLevel serverLevel)) return;
        ShenInvincibleStore store = ShenInvincibleStore.get(serverLevel);
        for (LivingEntity target : targets) {
            if (invincible) {
                store.add(target.getUUID());
            } else {
                store.remove(target.getUUID());
            }
        }
    }

    private static void executeSaturateFood(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int food = rule.getParamInt("food", 20);
        float saturation = (float) rule.getParamDouble("saturation", 5.0f);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                player.getFoodData().eat(food, saturation);
            }
        }
    }

    private static void executeSetStepHeight(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            CompoundTag nbt = target.saveWithoutId(new CompoundTag());
            float current = nbt.contains("StepHeight") ? nbt.getFloat("StepHeight") : 0.6f;
            double height = rule.resolveNumber("height", 1.0, current);
            nbt.putFloat("StepHeight", (float) Math.max(0, height));
            target.load(nbt);
        }
    }

    private static void executeSetResistance(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int amplifier = rule.getParamInt("amplifier", 0);
        int duration = rule.getParamInt("duration", 200);
        for (LivingEntity target : targets) {
            MobEffect resistance = BuiltInRegistries.MOB_EFFECT.get(
                ResourceLocation.tryParse("minecraft:resistance"));
            if (resistance != null) {
                target.addEffect(new MobEffectInstance(resistance, duration, amplifier));
            }
        }
    }

    private static void executeSwapHandItems(ShenEvent event, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                player.getInventory().items.set(player.getInventory().selected, off);
                player.getInventory().offhand.set(0, main);
            }
        }
    }

    private static void executeRandomTeleport(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        double range = rule.getParamDouble("range", 50.0);
        java.util.Random rand = new java.util.Random();
        for (LivingEntity target : targets) {
            if (event.world == null) continue;
            double x = target.getX() + (rand.nextDouble() - 0.5) * range * 2;
            double z = target.getZ() + (rand.nextDouble() - 0.5) * range * 2;
            double y = target.getY();
            BlockPos tpPos = BlockPos.containing(x, y, z);
            tpPos = event.world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, tpPos);
            target.teleportTo(tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5);
        }
    }

    private static void executeSetAir(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            int air = (int) rule.resolveNumber("air", 300, target.getAirSupply());
            target.setAirSupply(Math.max(0, Math.min(300, air)));
        }
    }

    private static void executeModifyAttribute(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String attrId = rule.getParam("attribute", "minecraft:generic.movement_speed");
        net.minecraft.world.entity.ai.attributes.Attribute attr = BuiltInRegistries.ATTRIBUTE.get(
            ResourceLocation.tryParse(attrId));
        if (attr == null) return;
        for (LivingEntity target : targets) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = target.getAttribute(attr);
            if (instance != null) {
                double current = instance.getBaseValue();
                double value = rule.resolveNumber("value", 0.1, current);
                instance.setBaseValue(value);
            }
        }
    }

    private static void executeFreezeEntity(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        for (LivingEntity target : targets) {
            int ticks = (int) rule.resolveNumber("ticks", 100, target.getTicksFrozen());
            target.setTicksFrozen(ticks);
        }
    }

    private static void executePlaceStructure(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        String structureId = rule.getParam("structureId", "minecraft:village_plains");
        boolean useTargetPos = rule.getParamBool("useTargetPos", true);
        boolean ignoreEntities = rule.getParamBool("ignoreEntities", true);
        int integrity = rule.getParamInt("integrity", 100);
        long seed = rule.getParamInt("seed", (int) (serverWorld.random.nextLong() & 0x7FFFFFFF));

        for (LivingEntity target : targets) {
            BlockPos placePos = useTargetPos ? target.blockPosition() : event.pos;
            if (placePos == null) placePos = BlockPos.ZERO;

            try {
                ResourceLocation structId = ResourceLocation.tryParse(structureId);
                if (structId == null) continue;

                java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate> templateOpt =
                    serverWorld.getStructureManager().get(structId);

                if (templateOpt.isPresent()) {
                    net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = templateOpt.get();
                    net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings placementData =
                        new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                            .setIgnoreEntities(ignoreEntities)
                            .setRotationPivot(new BlockPos(0, 0, 0));

                    template.placeInWorld(serverWorld, placePos, placePos, placementData,
                        net.minecraft.util.RandomSource.create(seed),
                        Block.UPDATE_ALL);
                } else {
                    String cmd = "place structure " + structureId + " " + placePos.getX() + " " + placePos.getY() + " " + placePos.getZ();
                    serverWorld.getServer().getCommands().performPrefixedCommand(
                        serverWorld.getServer().createCommandSourceStack().withSuppressedOutput(), cmd);
                }
            } catch (Exception e) {
                if (event.source instanceof ServerPlayer player) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c放置结构失败: " + e.getMessage()));
                }
            }
        }
    }

    private static void executeMassReplaceBlocks(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        String sourceBlockId = rule.getParam("sourceBlock", "minecraft:stone");
        String targetBlockId = rule.getParam("targetBlock", "minecraft:air");
        int radiusX = rule.getParamInt("radiusX", 5);
        int radiusY = rule.getParamInt("radiusY", 5);
        int radiusZ = rule.getParamInt("radiusZ", 5);
        String shape = rule.getParam("shape", "cube");
        boolean useTargetPos = rule.getParamBool("useTargetPos", true);

        // "all" = 全部方块：区域内一切方块都替换为目标方块
        boolean replaceAll = "all".equals(sourceBlockId) || "minecraft:all".equals(sourceBlockId);

        Block sourceBlock = replaceAll ? null : BuiltInRegistries.BLOCK.get(new ResourceLocation(sourceBlockId));
        Block targetBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(targetBlockId));
        if (targetBlock == null) return;
        if (!replaceAll && sourceBlock == null) return;

        for (LivingEntity target : targets) {
            BlockPos center = useTargetPos ? target.blockPosition() : event.pos;
            if (center == null) center = BlockPos.ZERO;
            int count = 0;

            for (int dx = -radiusX; dx <= radiusX; dx++) {
                for (int dy = -radiusY; dy <= radiusY; dy++) {
                    for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                        BlockPos checkPos = center.offset(dx, dy, dz);

                        if ("sphere".equals(shape)) {
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist > radiusX) continue;
                        }
                        if ("cylinder".equals(shape)) {
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist > radiusX || Math.abs(dy) > radiusY) continue;
                        }

                        if (replaceAll || serverWorld.getBlockState(checkPos).is(sourceBlock)) {
                            serverWorld.setBlock(checkPos, targetBlock.defaultBlockState(), 3);
                            count++;
                        }
                    }
                }
            }
        }
    }

    /** 劈大陆：把目标格加入劈除区域（仅做世界边界检查，不做区块间保护——
     *  输入多少格就劈多少格，未加载区块也会直接劈） */
    private static void addRegionBlock(ServerLevel world, BlockPos base, int ox, int oy, int oz,
                                       Set<BlockPos> region) {
        BlockPos bp = base.offset(ox, oy, oz);
        if (!world.isInWorldBounds(bp)) return;
        region.add(bp);
    }

    private static void executeCleaveLand(ShenEvent event, List<LivingEntity> targets) {
        if (!(event.world instanceof ServerLevel serverWorld)) return;
        ShenRule rule = event.rule;
        int length = Math.max(1, Math.min(640, rule.getParamInt("length", 5)));
        int width = Math.max(1, Math.min(640, rule.getParamInt("width", 5)));
        int depth = Math.max(1, Math.min(640, rule.getParamInt("depth", 5)));
        double damage = rule.getParamDouble("damage", 5.0);
        String damageTypeId = rule.getParam("damageType", "minecraft:generic");

        for (LivingEntity target : targets) {
            if (!(target.level() instanceof ServerLevel world)) continue;

            // —— 方向：平视方向（任意角度），投影判定精确覆盖 ——
            // 把每个候选格投影到"劈轴 u"与"垂直轴 v"：u∈(0..length]、v∈宽度带内的
            // 格子全部覆盖。任意转头角度（含 5°/22.5°/30°…）都能劈正面前方，
            // 斜向时相邻排格子全部纳入，无浮点漏格、无奇偶错位、无残留
            Vec3 f = Vec3.directionFromRotation(0, target.getYRot()).normalize();
            double fx = f.x, fz = f.z;
            if (fx == 0 && fz == 0) fz = 1; // 兜底

            BlockPos base = target.blockPosition();
            int bx = base.getX(), bz = base.getZ();
            double halfW = (width - 1) / 2.0;
            int spanX = (int) Math.ceil(Math.abs(fx) * length + Math.abs(fz) * width) + 1;
            int spanZ = (int) Math.ceil(Math.abs(fz) * length + Math.abs(fx) * width) + 1;

            Set<BlockPos> region = new HashSet<>();
            for (int x = bx - spanX; x <= bx + spanX; x++) {
                for (int z = bz - spanZ; z <= bz + spanZ; z++) {
                    double u = fx * (x - bx) + fz * (z - bz);
                    if (u <= 0 || u > length + 0.5) continue;
                    double v = -fz * (x - bx) + fx * (z - bz);
                    if (v < -halfW - 0.5 || v > (width - 1 - halfW) + 0.5) continue;
                    for (int k = 0; k < depth; k++) {
                        addRegionBlock(world, base, x - bx, -k, z - bz, region);
                    }
                }
            }

            // 删除方块（劈开大地）：区域内一切方块替换为空气
            for (BlockPos bp : region) {
                world.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
            }

            // 造成伤害（伤害类型可自选；不伤及触发者本人）
            if (damage > 0) {
                ResourceLocation dtId = ResourceLocation.tryParse(damageTypeId);
                if (dtId == null) dtId = new ResourceLocation("minecraft", "generic");
                ResourceKey<DamageType> dtKey = ResourceKey.create(Registries.DAMAGE_TYPE, dtId);
                var registryEntry = world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(dtKey);
                DamageSource source = new DamageSource(registryEntry, event.source, event.source);

                Set<LivingEntity> hit = new HashSet<>();
                for (BlockPos bp : region) {
                    for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(bp))) {
                        if (e == event.source) continue;
                        hit.add(e);
                    }
                }
                for (LivingEntity e : hit) {
                    e.hurt(source, (float) damage);
                }
            }
        }
    }

    private static void executeModAdapterEffect(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String effId = rule.getParam("adapterEffectId", "");
        if (effId.isEmpty()) return;
        String value = rule.getParam("value", "0");
        for (LivingEntity target : targets) {
            god.bai.god.shen.compat.ModAdapterManager.executeEffect(effId, value, target);
        }
    }

    private static void executeSmfsSetSpiritResistance(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        float value = (float) rule.getParamDouble("value", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setSpiritResistance(player, value);
            }
        }
    }

    private static void executeSmfsSetSpiritDamage(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        float value = (float) rule.getParamDouble("value", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setSpiritDamage(player, value);
            }
        }
    }

    private static void executeSmfsSetCurrentSpirit(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        float value = (float) rule.getParamDouble("value", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setCurrentSpirit(player, value);
            }
        }
    }

    private static void executeSmfsSetMaxSpirit(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        float value = (float) rule.getParamDouble("value", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setMaxSpirit(player, value);
            }
        }
    }

    private static void executeSmfsSetSanity(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        float value = (float) rule.getParamDouble("value", 100);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setSanity(player, value);
            }
        }
    }

    private static void executeSmfsSetRevivalFactor(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        double value = rule.getParamDouble("value", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setRevivalFactor(player, value);
            }
        }
    }

    private static void executeSmfsSetMainGhostSlot(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int slotIndex = rule.getParamInt("slotIndex", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setMainGhostSlot(player, slotIndex);
            }
        }
    }

    private static void executeSmfsCycleMainGhost(ShenEvent event, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.cycleMainGhost(player);
            }
        }
    }

    private static void executeSmfsSetNbtAttribute(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        String key = rule.getParam("attributeKey", "");
        double value = rule.getParamDouble("value", 0);
        if (key.isEmpty()) return;
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setNbtAttribute(player, key, value);
            }
        }
    }

    private static void executeSmfsActivateDomain(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean active = rule.getParamBool("value", true);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostDomainActive(player, active);
            }
        }
    }

    private static void executeSmfsSetDomainType(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int color = rule.getParamInt("color", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostDomainColor(player, color);
            }
        }
    }

    private static void executeSmfsSetDomainLevel(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int level = rule.getParamInt("level", 1);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostDomainLevel(player, level);
            }
        }
    }

    private static void executeSmfsSetGhostEyeLevel(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int level = rule.getParamInt("level", 1);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostEyeLevel(player, level);
            }
        }
    }

    private static void executeSmfsToggleGhostEye(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        boolean active = rule.getParamBool("value", true);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostEyeActive(player, active);
            }
        }
    }

    private static void executeSmfsLockSlot(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int slotIndex = rule.getParamInt("slotIndex", 0);
        boolean locked = rule.getParamBool("locked", true);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setSlotLocked(player, slotIndex, locked);
            }
        }
    }

    private static void executeSmfsClearSlot(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int slotIndex = rule.getParamInt("slotIndex", 0);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.clearSlot(player, slotIndex);
            }
        }
    }

    private static void executeSmfsSwapSlots(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int slotA = rule.getParamInt("slotA", 0);
        int slotB = rule.getParamInt("slotB", 1);
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.swapSlots(player, slotA, slotB);
            }
        }
    }

    private static void executeSmfsSetSlotGhost(ShenEvent event, List<LivingEntity> targets) {
        ShenRule rule = event.rule;
        int slotIndex = rule.getParamInt("slotIndex", 0);
        String ghostItemId = rule.getParam("ghostItemId", "");
        if (ghostItemId.isEmpty()) return;
        for (LivingEntity target : targets) {
            if (target instanceof Player player) {
                god.bai.god.shen.compat.smfs.SmfsIntegration.setGhostInSlot(player, slotIndex, ghostItemId);
            }
        }
    }
}
