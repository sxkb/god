package god.bai.god.shen.ui;

import god.bai.god.shen.ShenEffectType;
import god.bai.god.shen.ShenTargetType;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.*;

/**
 * 言出法随参数辅助类
 */
public class ShenParamHelper {

    public enum WidgetType {
        DROPDOWN,
        SLIDER_PRESET,
        BOOLEAN,
        NUMBER,
        TEXT,
        COLOR
    }

    public enum RegistryType {
        STATUS_EFFECT,
        ENTITY_TYPE,
        ITEM,
        BLOCK,
        PARTICLE,
        SOUND_EVENT,
        DIMENSION,
        DAMAGE_TYPE,
        ATTRIBUTE,
        ENCHANTMENT,
        NBT_VARIABLE
    }

    public static class ParamDef {
        public final String key;
        public final String label;
        public final WidgetType widgetType;
        public final String defaultValue;
        public final String[][] options;
        public final int min;
        public final int max;
        public final boolean allowCustom;
        public final RegistryType registryType;
        public final boolean operable;

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue,
                        String[][] options, int min, int max, boolean allowCustom, RegistryType registryType, boolean operable) {
            this.key = key;
            this.label = label;
            this.widgetType = widgetType;
            this.defaultValue = defaultValue;
            this.options = options;
            this.min = min;
            this.max = max;
            this.allowCustom = allowCustom;
            this.registryType = registryType;
            this.operable = operable;
        }

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue, String[][] options) {
            this(key, label, widgetType, defaultValue, options, 0, 0, false, null, false);
        }

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue, String[][] options, boolean allowCustom) {
            this(key, label, widgetType, defaultValue, options, 0, 0, allowCustom, null, false);
        }

        public ParamDef(String key, String label, String defaultValue, RegistryType registryType) {
            this(key, label, WidgetType.DROPDOWN, defaultValue, null, 0, 0, true, registryType, false);
        }

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue, int min, int max) {
            this(key, label, widgetType, defaultValue, null, min, max, false, null, false);
        }

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue, int min, int max, boolean operable) {
            this(key, label, widgetType, defaultValue, null, min, max, false, null, operable);
        }

        public ParamDef(String key, String label, WidgetType widgetType, String defaultValue) {
            this(key, label, widgetType, defaultValue, null, 0, 0, false, null, false);
        }
    }

    private static java.util.Map<String, String> buildChineseNameMap(String[][] options) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (String[] opt : options) {
            map.put(opt[1], opt[0]);
        }
        return map;
    }

    public static String[][] buildRegistryOptions(RegistryType type) {
        List<String[]> result = new ArrayList<>();

        switch (type) {
            case STATUS_EFFECT: {
                for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
                    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                    if (id == null) continue;
                    String cnName = I18n.get(effect.getDescriptionId());
                    // 避免显示奇怪的翻译键
                    if (cnName.startsWith("effect.") || cnName.startsWith("enchantment.")) continue;
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case ENTITY_TYPE: {
                for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    if (id == null) continue;
                    String cnName = I18n.get(entityType.getDescriptionId());
                    String translationKey = entityType.getDescriptionId();
                    if (cnName.equals(translationKey) || cnName.startsWith("entity.")) {
                        cnName = formatIdToName(id);
                    }
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case ITEM: {
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id == null) continue;
                    String cnName = I18n.get(item.getDescriptionId());
                    String translationKey = item.getDescriptionId();
                    if (cnName.equals(translationKey) || cnName.startsWith("item.") || cnName.startsWith("block.")) {
                        cnName = formatIdToName(id);
                    }
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case BLOCK: {
                for (Block block : BuiltInRegistries.BLOCK) {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id == null) continue;
                    String cnName = I18n.get(block.getDescriptionId());
                    String translationKey = block.getDescriptionId();
                    if (cnName.equals(translationKey) || cnName.startsWith("block.")) {
                        cnName = formatIdToName(id);
                    }
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case PARTICLE: {
                java.util.Map<String, String> cnMap = buildChineseNameMap(PARTICLE_OPTIONS);
                for (ParticleType<?> particle : BuiltInRegistries.PARTICLE_TYPE) {
                    ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(particle);
                    if (id == null) continue;
                    String cnName = cnMap.get(id.toString());
                    if (cnName == null) {
                        String path = id.getPath().replace('_', ' ').replace(".", " ");
                        StringBuilder sb = new StringBuilder();
                        boolean nextUpper = true;
                        for (char c : path.toCharArray()) {
                            if (c == ' ') { nextUpper = true; sb.append(c); }
                            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
                            else sb.append(c);
                        }
                        cnName = sb.toString();
                    }
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case SOUND_EVENT: {
                java.util.Map<String, String> cnMap = buildChineseNameMap(SOUND_OPTIONS);
                for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
                    ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
                    if (id == null) continue;
                    String cnName = cnMap.get(id.toString());
                    if (cnName == null) {
                        String subtitleKey = "subtitles." + id.getPath().replace('/', '.');
                        cnName = I18n.get(subtitleKey);
                        if (cnName.equals(subtitleKey)) {
                            String path = id.getPath().replace('_', ' ').replace('/', ' ');
                            StringBuilder sb = new StringBuilder();
                            boolean nextUpper = true;
                            for (char c : path.toCharArray()) {
                                if (c == ' ') { nextUpper = true; sb.append(c); }
                                else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
                                else sb.append(c);
                            }
                            cnName = sb.toString();
                        }
                    }
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case DIMENSION: {
                for (Map.Entry<String, String> e : BUILTIN_DIMENSION_NAMES.entrySet()) {
                    result.add(new String[]{e.getValue(), e.getKey()});
                }
                break;
            }
            case DAMAGE_TYPE: {
                for (Map.Entry<String, String> e : BUILTIN_DAMAGE_TYPE_NAMES.entrySet()) {
                    result.add(new String[]{e.getValue(), e.getKey()});
                }
                break;
            }
            case ATTRIBUTE: {
                for (net.minecraft.world.entity.ai.attributes.Attribute attr : BuiltInRegistries.ATTRIBUTE) {
                    ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
                    if (id == null) continue;
                    String cnName = tryTranslateAttribute(id, attr);
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case ENCHANTMENT: {
                for (net.minecraft.world.item.enchantment.Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
                    ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(ench);
                    if (id == null) continue;
                    String cnName = tryTranslateEnchantment(id, ench);
                    result.add(new String[]{cnName, id.toString()});
                }
                break;
            }
            case NBT_VARIABLE: {
                for (Map.Entry<String, String> e : NBT_PATH_TRANSLATIONS.entrySet()) {
                    result.add(new String[]{e.getValue(), e.getKey()});
                }
                for (god.bai.god.shen.compat.NbtPathRegistry.WatchedPath wp :
                        god.bai.god.shen.compat.NbtPathRegistry.getAll()) {
                    result.add(new String[]{wp.displayName, wp.nbtPath});
                }
                break;
            }
        }

        Set<String> seen = new HashSet<>();
        List<String[]> unique = new ArrayList<>();
        for (String[] entry : result) {
            if (seen.add(entry[1])) {
                unique.add(entry);
            }
        }
        unique.sort(Comparator.comparing(a -> a[0]));
        return unique.toArray(new String[0][0]);
    }

    /**
     * 「移山填海」源方块选项：在全部方块注册表前插入"全部方块"选项
     */
    public static String[][] buildSourceBlockOptions() {
        String[][] base = buildRegistryOptions(RegistryType.BLOCK);
        String[][] result = new String[base.length + 1][];
        result[0] = new String[]{"全部方块", "all"};
        System.arraycopy(base, 0, result, 1, base.length);
        return result;
    }

    private static final java.util.Map<String, String> BUILTIN_DIMENSION_NAMES = new HashMap<>();
    static {
        BUILTIN_DIMENSION_NAMES.put("minecraft:overworld", "主世界");
        BUILTIN_DIMENSION_NAMES.put("minecraft:the_nether", "下界");
        BUILTIN_DIMENSION_NAMES.put("minecraft:the_end", "末地");
        for (int i = 1; i <= 18; i++) {
            BUILTIN_DIMENSION_NAMES.put("guiyu:hell_" + i, "地狱第" + i + "层");
        }
    }

    private static final java.util.Map<String, String> BUILTIN_DAMAGE_TYPE_NAMES = new HashMap<>();
    static {
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:generic", "通用伤害");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:in_fire", "火焰中");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:on_fire", "着火");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:lava", "岩浆");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:hot_floor", "岩浆块");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:in_wall", "窒息");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:cramming", "挤压");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:drown", "溺水");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:starve", "饥饿");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:cactus", "仙人掌");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:fall", "摔落");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:fly_into_wall", "飞行撞击");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:out_of_world", "虚空");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:generic_kill", "强制击杀");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:magic", "魔法");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:indirect_magic", "范围魔法");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:wither", "凋零");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:dragon_breath", "龙息");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:dry_out", "脱水");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:sweet_berry_bush", "甜浆果丛");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:freeze", "冰冻");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:stalagmite", "钟乳石");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:falling_block", "落沙");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:falling_anvil", "落铁砧");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:falling_stalactite", "落钟乳石");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:sting", "虫蛰");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:mob_attack", "近战攻击");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:mob_attack_no_aggro", "无仇恨攻击");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:player_attack", "玩家攻击");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:arrow", "箭矢");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:trident", "三叉戟");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:mob_projectile", "弹射物");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:fireball", "火球");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:fireworks", "烟花");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:thorns", "荆棘");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:explosion", "爆炸");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:player_explosion", "玩家爆炸");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:bad_respawn_point", "重生点错误");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:lightning_bolt", "闪电");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:sonic_boom", "音波");
        BUILTIN_DAMAGE_TYPE_NAMES.put("minecraft:outside_border", "边界外");
        BUILTIN_DAMAGE_TYPE_NAMES.put("smfs:ghost", "灵异伤害");
    }

    private static String formatIdToName(ResourceLocation id) {
        String path = id.getPath().replace('_', ' ').replace("/", " ");
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : path.toCharArray()) {
            if (c == ' ') { nextUpper = true; sb.append(c); }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else sb.append(c);
        }
        return "§7[" + id.getNamespace() + "]§r " + sb.toString();
    }

    private static final java.util.Map<String, String> NBT_PATH_TRANSLATIONS = new LinkedHashMap<>();
    static {
        NBT_PATH_TRANSLATIONS.put("Health", "生命值");
        NBT_PATH_TRANSLATIONS.put("AbsorptionAmount", "伤害吸收");
        NBT_PATH_TRANSLATIONS.put("HurtTime", "受伤时间");
        NBT_PATH_TRANSLATIONS.put("HurtByTimestamp", "受伤时间戳");
        NBT_PATH_TRANSLATIONS.put("DeathTime", "死亡时间");
        NBT_PATH_TRANSLATIONS.put("FallDistance", "摔落距离");
        NBT_PATH_TRANSLATIONS.put("OnGround", "在地面");
        NBT_PATH_TRANSLATIONS.put("Air", "空气值");
        NBT_PATH_TRANSLATIONS.put("Fire", "着火时间");
        NBT_PATH_TRANSLATIONS.put("FoodLevel", "饱食度");
        NBT_PATH_TRANSLATIONS.put("foodLevel", "饱食度");
        NBT_PATH_TRANSLATIONS.put("Saturation", "饱和度");
        NBT_PATH_TRANSLATIONS.put("foodSaturationLevel", "饱和度");
        NBT_PATH_TRANSLATIONS.put("XpLevel", "经验等级");
        NBT_PATH_TRANSLATIONS.put("XpTotal", "累计经验");
        NBT_PATH_TRANSLATIONS.put("Score", "计分板");
        NBT_PATH_TRANSLATIONS.put("SleepTimer", "睡眠计时");
        NBT_PATH_TRANSLATIONS.put("playerGameType", "游戏模式");
        NBT_PATH_TRANSLATIONS.put("Motion", "运动向量");
        NBT_PATH_TRANSLATIONS.put("Pos", "位置");
        NBT_PATH_TRANSLATIONS.put("Rotation", "旋转角度");
        NBT_PATH_TRANSLATIONS.put("UUID", "唯一ID");
        NBT_PATH_TRANSLATIONS.put("CustomName", "自定义名称");
        NBT_PATH_TRANSLATIONS.put("ActiveEffects", "药水效果列表");
        NBT_PATH_TRANSLATIONS.put("Attributes", "属性列表");
        NBT_PATH_TRANSLATIONS.put("Base", "基础值");
        NBT_PATH_TRANSLATIONS.put("Name", "名称");
        NBT_PATH_TRANSLATIONS.put("Modifiers", "修饰符");
        NBT_PATH_TRANSLATIONS.put("Amount", "数值");
        NBT_PATH_TRANSLATIONS.put("Operation", "运算类型");
        NBT_PATH_TRANSLATIONS.put("ArmorItems", "盔甲栏");
        NBT_PATH_TRANSLATIONS.put("HandItems", "手持物品");
        NBT_PATH_TRANSLATIONS.put("Inventory", "背包");
        NBT_PATH_TRANSLATIONS.put("EnderItems", "末影箱");
        NBT_PATH_TRANSLATIONS.put("id", "注册ID");
        NBT_PATH_TRANSLATIONS.put("Count", "数量");
        NBT_PATH_TRANSLATIONS.put("tag", "标签/NBT");
        NBT_PATH_TRANSLATIONS.put("Enchantments", "附魔列表");
        NBT_PATH_TRANSLATIONS.put("Damage", "损坏值");
        NBT_PATH_TRANSLATIONS.put("lvl", "等级");
        NBT_PATH_TRANSLATIONS.put("display", "显示属性");
        NBT_PATH_TRANSLATIONS.put("Lore", "描述");
        NBT_PATH_TRANSLATIONS.put("RepairCost", "修复花费");
        NBT_PATH_TRANSLATIONS.put("recipeBook", "配方书");
        NBT_PATH_TRANSLATIONS.put("seenCredits", "已看鸣谢");
        NBT_PATH_TRANSLATIONS.put("SpawnX", "重生X");
        NBT_PATH_TRANSLATIONS.put("SpawnY", "重生Y");
        NBT_PATH_TRANSLATIONS.put("SpawnZ", "重生Z");
        NBT_PATH_TRANSLATIONS.put("SpawnAngle", "重生角度");
        NBT_PATH_TRANSLATIONS.put("SpawnForced", "强制重生点");
        NBT_PATH_TRANSLATIONS.put("SpawnDimension", "重生维度");
        NBT_PATH_TRANSLATIONS.put("Dimension", "所在维度");

        NBT_PATH_TRANSLATIONS.put("cardinal_components", "模组组件");
        NBT_PATH_TRANSLATIONS.put("mana", "魔力");
        NBT_PATH_TRANSLATIONS.put("maxMana", "最大魔力");
        NBT_PATH_TRANSLATIONS.put("stamina", "体力");
        NBT_PATH_TRANSLATIONS.put("level", "等级");
        NBT_PATH_TRANSLATIONS.put("experience", "经验");
        NBT_PATH_TRANSLATIONS.put("skillPoints", "技能点");
        NBT_PATH_TRANSLATIONS.put("cultivation", "修为/境界");
    }

    public static String tryTranslateDimension(ResourceLocation dimId) {
        String key1 = "dimension." + dimId.getNamespace() + "." + dimId.getPath();
        String t1 = I18n.get(key1);
        if (!t1.equals(key1)) return t1;

        String key2 = "world." + dimId.getNamespace() + "." + dimId.getPath();
        String t2 = I18n.get(key2);
        if (!t2.equals(key2)) return t2;

        String key3 = "generator." + dimId.getPath();
        String t3 = I18n.get(key3);
        if (!t3.equals(key3)) return t3;

        String builtin = BUILTIN_DIMENSION_NAMES.get(dimId.toString());
        if (builtin != null) return builtin;

        return formatIdToName(dimId);
    }

    public static String tryTranslateDamageType(ResourceLocation dtId) {
        String builtin = BUILTIN_DAMAGE_TYPE_NAMES.get(dtId.toString());
        if (builtin != null) return builtin;

        String deathKey = "death.attack." + dtId.getPath();
        String t = I18n.get(deathKey);
        if (!t.equals(deathKey)) return t;

        String deathKeyNS = "death.attack." + dtId.getNamespace() + "." + dtId.getPath();
        String tNS = I18n.get(deathKeyNS);
        if (!tNS.equals(deathKeyNS)) return tNS;

        return formatIdToName(dtId);
    }

    public static String tryTranslateNbtVariable(String nbtKey) {
        String cnName = NBT_PATH_TRANSLATIONS.get(nbtKey);
        if (cnName != null) return cnName;
        return "📋 " + nbtKey;
    }

    public static String tryTranslateAttribute(ResourceLocation attrId, net.minecraft.world.entity.ai.attributes.Attribute attr) {
        String cnName = BUILTIN_ATTRIBUTE_NAMES.get(attrId.toString());
        if (cnName != null) return cnName;
        String i18nKey = "attribute.name." + attrId.getPath();
        String t = I18n.get(i18nKey);
        if (!t.equals(i18nKey)) return t;
        return formatIdToName(attrId);
    }

    public static String tryTranslateEnchantment(ResourceLocation enchId, net.minecraft.world.item.enchantment.Enchantment ench) {
        String cnName = BUILTIN_ENCHANTMENT_NAMES.get(enchId.toString());
        if (cnName != null) return cnName;
        String i18nName = I18n.get(ench.getDescriptionId());
        if (!i18nName.startsWith("enchantment.")) return i18nName;
        return formatIdToName(enchId);
    }

    private static final java.util.Map<String, String> BUILTIN_ATTRIBUTE_NAMES = new HashMap<>();
    static {
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.max_health", "最大生命值");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.follow_range", "跟随范围");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.knockback_resistance", "击退抗性");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.movement_speed", "移动速度");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.flying_speed", "飞行速度");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.attack_damage", "攻击伤害");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.attack_knockback", "攻击击退");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.attack_speed", "攻击速度");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.armor", "护甲值");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.armor_toughness", "护甲韧性");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.luck", "幸运值");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:zombie.spawn_reinforcements", "僵尸增援概率");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:horse.jump_strength", "马跳跃力");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.max_absorption", "最大吸收值");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.step_height", "步高");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.gravity", "重力");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.safe_fall_distance", "安全跌落距离");
        BUILTIN_ATTRIBUTE_NAMES.put("minecraft:generic.fall_damage_multiplier", "坠落伤害倍率");
    }

    private static final java.util.Map<String, String> BUILTIN_ENCHANTMENT_NAMES = new HashMap<>();
    static {
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:protection", "保护");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:fire_protection", "火焰保护");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:feather_falling", "摔落保护");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:blast_protection", "爆炸保护");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:projectile_protection", "弹射物保护");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:respiration", "水下呼吸");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:aqua_affinity", "水下速掘");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:thorns", "荆棘");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:depth_strider", "深海探索者");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:frost_walker", "冰霜行者");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:binding_curse", "绑定诅咒");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:sharpness", "锋利");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:smite", "亡灵杀手");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:bane_of_arthropods", "节肢杀手");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:knockback", "击退");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:fire_aspect", "火焰附加");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:looting", "抢夺");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:sweeping", "横扫之刃");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:efficiency", "效率");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:silk_touch", "精准采集");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:unbreaking", "耐久");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:fortune", "时运");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:power", "力量");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:punch", "冲击");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:flame", "火矢");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:infinity", "无限");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:luck_of_the_sea", "海之眷顾");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:lure", "饵钓");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:loyalty", "忠诚");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:impaling", "穿刺");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:riptide", "激流");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:channeling", "引雷");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:multishot", "多重射击");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:quick_charge", "快速装填");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:piercing", "穿透");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:mending", "经验修补");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:vanishing_curse", "消失诅咒");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:soul_speed", "灵魂疾行");
        BUILTIN_ENCHANTMENT_NAMES.put("minecraft:swift_sneak", "迅捷潜行");
    }

    public static String[][] buildDimensionOptionsFromRegistry(RegistryAccess registryManager) {
        List<String[]> result = new ArrayList<>();
        try {
            Registry<DimensionType> dimRegistry = registryManager.registry(Registries.DIMENSION_TYPE).orElse(null);
            if (dimRegistry != null) {
                for (Map.Entry<ResourceKey<DimensionType>, DimensionType> entry : dimRegistry.entrySet()) {
                    ResourceLocation dimId = entry.getKey().location();
                    if (dimId == null) continue;
                    String cnName = tryTranslateDimension(dimId);
                    result.add(new String[]{cnName, dimId.toString()});
                }
            }
        } catch (Exception e) {
            // 安全兜底
        }
        if (result.isEmpty()) {
            for (Map.Entry<String, String> e : BUILTIN_DIMENSION_NAMES.entrySet()) {
                result.add(new String[]{e.getValue(), e.getKey()});
            }
        }
        Set<String> seen = new HashSet<>();
        List<String[]> unique = new ArrayList<>();
        for (String[] entry : result) {
            if (seen.add(entry[1])) {
                unique.add(entry);
            }
        }
        unique.sort(Comparator.comparing(a -> a[0]));
        return unique.toArray(new String[0][0]);
    }

    public static String[][] buildDamageTypeOptionsFromRegistry(RegistryAccess registryManager) {
        List<String[]> result = new ArrayList<>();
        try {
            Registry<net.minecraft.world.damagesource.DamageType> dmgRegistry = registryManager.registry(Registries.DAMAGE_TYPE).orElse(null);
            if (dmgRegistry != null) {
                for (Map.Entry<ResourceKey<net.minecraft.world.damagesource.DamageType>, net.minecraft.world.damagesource.DamageType> entry : dmgRegistry.entrySet()) {
                    ResourceLocation dtId = entry.getKey().location();
                    if (dtId == null) continue;
                    String cnName = tryTranslateDamageType(dtId);
                    result.add(new String[]{cnName, dtId.toString()});
                }
            }
        } catch (Exception e) {
            // 安全兜底
        }
        if (result.isEmpty()) {
            for (Map.Entry<String, String> e : BUILTIN_DAMAGE_TYPE_NAMES.entrySet()) {
                result.add(new String[]{e.getValue(), e.getKey()});
            }
        }
        Set<String> seen = new HashSet<>();
        List<String[]> unique = new ArrayList<>();
        for (String[] entry : result) {
            if (seen.add(entry[1])) {
                unique.add(entry);
            }
        }
        unique.sort(Comparator.comparing(a -> a[0]));
        return unique.toArray(new String[0][0]);
    }

    public static boolean isLargeRegistry(RegistryType type) {
        return type == RegistryType.ITEM || type == RegistryType.BLOCK;
    }

    public static final String[][] POTION_OPTIONS = {
        {"发光", "minecraft:glowing"},
        {"生命恢复", "minecraft:regeneration"},
        {"力量", "minecraft:strength"},
        {"速度", "minecraft:speed"},
        {"抗性提升", "minecraft:resistance"},
        {"跳跃提升", "minecraft:jump_boost"},
        {"隐身", "minecraft:invisibility"},
        {"夜视", "minecraft:night_vision"},
        {"水下呼吸", "minecraft:water_breathing"},
        {"防火", "minecraft:fire_resistance"},
        {"缓慢", "minecraft:slowness"},
        {"虚弱", "minecraft:weakness"},
        {"中毒", "minecraft:poison"},
        {"凋零", "minecraft:wither"},
        {"饥饿", "minecraft:hunger"},
        {"迟缓", "minecraft:mining_fatigue"},
        {"急迫", "minecraft:haste"},
        {"饱和", "minecraft:saturation"},
        {"幸运", "minecraft:luck"},
        {"不祥之兆", "minecraft:bad_omen"},
        {"村庄英雄", "minecraft:village_heroe"},
        {"海豚的恩惠", "minecraft:dolphins_grace"},
        {"缓降", "minecraft:slow_falling"},
        {"黑暗", "minecraft:darkness"},
    };

    public static final String[][] ENTITY_OPTIONS = {
        {"闪电", "minecraft:lightning_bolt"},
        {"苦力怕", "minecraft:creeper"},
        {"僵尸", "minecraft:zombie"},
        {"骷髅", "minecraft:skeleton"},
        {"蜘蛛", "minecraft:spider"},
        {"末影人", "minecraft:enderman"},
        {"烈焰人", "minecraft:blaze"},
        {"凋零骷髅", "minecraft:wither_skeleton"},
        {"猪灵", "minecraft:piglin"},
        {"铁傀儡", "minecraft:iron_golem"},
        {"雪傀儡", "minecraft:snow_golem"},
        {"羊", "minecraft:sheep"},
        {"牛", "minecraft:cow"},
        {"猪", "minecraft:pig"},
        {"鸡", "minecraft:chicken"},
        {"马", "minecraft:horse"},
        {"狼", "minecraft:wolf"},
        {"猫", "minecraft:cat"},
        {"村民", "minecraft:villager"},
        {"末影水晶", "minecraft:end_crystal"},
        {"船", "minecraft:boat"},
        {"矿车", "minecraft:minecart"},
        {"TNT", "minecraft:tnt"},
        {"掉落物", "minecraft:item"},
        {"经验球", "minecraft:experience_orb"},
    };

    public static final String[][] ITEM_OPTIONS = {
        {"钻石", "minecraft:diamond"},
        {"铁锭", "minecraft:iron_ingot"},
        {"金锭", "minecraft:gold_ingot"},
        {"绿宝石", "minecraft:emerald"},
        {"下界合金锭", "minecraft:netherite_ingot"},
        {"石头", "minecraft:stone"},
        {"草方块", "minecraft:grass_block"},
        {"泥土", "minecraft:dirt"},
        {"圆石", "minecraft:cobblestone"},
        {"橡木原木", "minecraft:oak_log"},
        {"铁剑", "minecraft:iron_sword"},
        {"钻石剑", "minecraft:diamond_sword"},
        {"钻石镐", "minecraft:diamond_pickaxe"},
        {"弓", "minecraft:bow"},
        {"苹果", "minecraft:apple"},
        {"金苹果", "minecraft:golden_apple"},
        {"附魔金苹果", "minecraft:enchanted_golden_apple"},
        {"面包", "minecraft:bread"},
        {"红石", "minecraft:redstone"},
        {"青金石", "minecraft:lapis_lazuli"},
        {"铁胸甲", "minecraft:iron_chestplate"},
        {"钻石胸甲", "minecraft:diamond_chestplate"},
        {"下界合金胸甲", "minecraft:netherite_chestplate"},
        {"末影珍珠", "minecraft:ender_pearl"},
        {"烈焰棒", "minecraft:blaze_rod"},
        {"书", "minecraft:book"},
        {"附魔之瓶", "minecraft:experience_bottle"},
        {"不死图腾", "minecraft:totem_of_undying"},
    };

    public static final String[][] BLOCK_OPTIONS = {
        {"石头", "minecraft:stone"},
        {"草方块", "minecraft:grass_block"},
        {"泥土", "minecraft:dirt"},
        {"圆石", "minecraft:cobblestone"},
        {"橡木原木", "minecraft:oak_log"},
        {"橡木木板", "minecraft:oak_planks"},
        {"玻璃", "minecraft:glass"},
        {"沙子", "minecraft:sand"},
        {"金块", "minecraft:gold_block"},
        {"铁块", "minecraft:iron_block"},
        {"钻石块", "minecraft:diamond_block"},
        {"下界合金块", "minecraft:netherite_block"},
        {"冰", "minecraft:ice"},
        {"蓝冰", "minecraft:blue_ice"},
        {"岩浆块", "minecraft:magma_block"},
        {"TNT", "minecraft:tnt"},
        {"书架", "minecraft:bookshelf"},
        {"熔炉", "minecraft:furnace"},
        {"工作台", "minecraft:crafting_table"},
        {"箱子", "minecraft:chest"},
        {"末地传送门框架", "minecraft:end_portal_frame"},
        {"基岩", "minecraft:bedrock"},
        {"命令方块", "minecraft:command_block"},
        {"屏障", "minecraft:barrier"},
    };

    public static final String[][] WEATHER_OPTIONS = {
        {"☀️ 晴天", "clear"},
        {"🌧️ 下雨", "rain"},
        {"⛈️ 雷暴", "thunder"},
    };

    public static final String[][] TIME_OPTIONS = {
        {"🌅 黎明", "0"},
        {"☀️ 白天", "1000"},
        {"🌞 正午", "6000"},
        {"🌆 黄昏", "13000"},
        {"🌙 夜晚", "18000"},
    };

    public static final String[][] PARTICLE_OPTIONS = {
        {"火焰", "minecraft:flame"},
        {"爱心", "minecraft:heart"},
        {"音符", "minecraft:note"},
        {"附魔", "minecraft:enchant"},
        {"紫水晶", "minecraft:portal"},
        {"爆炸", "minecraft:explosion"},
        {"烟雾", "minecraft:smoke"},
        {"滴血", "minecraft:dripping_blood"},
        {"滴水", "minecraft:dripping_water"},
        {"雷电", "minecraft:electrical_spark"},
        {"咒语", "minecraft:witch"},
        {"潜影贝", "minecraft:dragon_breath"},
        {"末影信号", "minecraft:end_rod"},
        {"水雾", "minecraft:splash"},
        {"钓鱼", "minecraft:fishing"},
        {"朝贡", "minecraft:totem_of_undying"},
    };

    public static final String[][] SOUND_OPTIONS = {
        {"经验拾取", "minecraft:entity.experience_orb.pickup"},
        {"等级提升", "minecraft:entity.player.levelup"},
        {"爆炸", "minecraft:entity.generic.explode"},
        {"闪电", "minecraft:entity.lightning_bolt.thunder"},
        {"TNT引信", "minecraft:entity.tnt.primed"},
        {"铁砧", "minecraft:block.anvil.land"},
        {"开箱子", "minecraft:block.chest.open"},
        {"关箱子", "minecraft:block.chest.close"},
        {"打雷", "minecraft:entity.lightning_bolt.impact"},
        {"末影传送", "minecraft:entity.enderman.teleport"},
        {"骷髅弓", "minecraft:entity.skeleton.shoot"},
        {"龙吼", "minecraft:entity.ender_dragon.growl"},
        {"苦力怕", "minecraft:entity.creeper.primed"},
        {"不死图腾", "minecraft:item.totem.use"},
        {"烟花", "minecraft:entity.firework_rocket.launch"},
    };

    public static final String[][] GAME_RULE_OPTIONS = {
        {"昼夜循环", "doDaylightCycle"},
        {"生物生成", "doMobSpawning"},
        {"方块掉落", "doTileDrops"},
        {"火焰蔓延", "doFireTick"},
        {"天气变化", "doWeatherCycle"},
        {"自然恢复", "naturalRegeneration"},
        {"玩家是否掉落", "fallDamage"},
        {"TNT爆炸", "tntExplodes"},
        {"命令方块", "commandBlockOutput"},
        {"存活玩家数", "maxCommandChainLength"},
        {"死亡不掉落", "keepInventory"},
        {"怪物破坏", "mobGriefing"},
        {"展示坐标", "showDeathMessages"},
        {"立即重生", "doImmediateRespawn"},
    };

    public static final String[][] DURATION_OPTIONS = {
        {"3秒", "60"},
        {"5秒", "100"},
        {"10秒", "200"},
        {"30秒", "600"},
        {"1分钟", "1200"},
        {"5分钟", "6000"},
        {"永久", "2147483647"},
    };

    public static final String[][] AMPLIFIER_OPTIONS = {
        {"I", "0"}, {"II", "1"}, {"III", "2"}, {"IV", "3"}, {"V", "4"},
    };

    public static final String[][] COUNT_OPTIONS_1_64 = {
        {"1", "1"}, {"4", "4"}, {"8", "8"}, {"16", "16"}, {"32", "32"}, {"64", "64"},
    };

    public static final String[][] COUNT_OPTIONS_1_10 = {
        {"1", "1"}, {"2", "2"}, {"3", "3"}, {"5", "5"}, {"10", "10"},
    };

    public static final String[][] EXPLOSION_POWER_OPTIONS = {
        {"小型", "2.0"}, {"中型", "4.0"}, {"大型", "8.0"}, {"核弹", "16.0"},
    };

    public static final String[][] RANGE_OPTIONS = {
        {"5格", "5"}, {"10格", "10"}, {"20格", "20"}, {"50格", "50"}, {"无限", "500"},
    };

    public static List<ParamDef> getEffectParams(ShenEffectType effectType) {
        List<ParamDef> params = new ArrayList<>();
        switch (effectType) {
            case MODIFY_HEALTH:
                params.add(new ParamDef("target", "目标属性", WidgetType.DROPDOWN, "current", new String[][]{
                    {"当前生命值", "current"}, {"最大生命值", "max"},
                }));
                params.add(new ParamDef("amount", "生命值", WidgetType.NUMBER, "20", 0, 10000));
                params.add(new ParamDef("percent", "百分比模式", WidgetType.BOOLEAN, "false"));
                break;
            case DEAL_DAMAGE:
                params.add(new ParamDef("damageType", "伤害类型", "minecraft:generic", RegistryType.DAMAGE_TYPE));
                params.add(new ParamDef("amount", "伤害值", WidgetType.NUMBER, "10", 1, 10000));
                break;
            case APPLY_EFFECT:
                params.add(new ParamDef("effect", "药水效果", "minecraft:glowing", RegistryType.STATUS_EFFECT));
                params.add(new ParamDef("duration", "持续时间", WidgetType.SLIDER_PRESET, "200", DURATION_OPTIONS));
                params.add(new ParamDef("amplifier", "等级", WidgetType.NUMBER, "0", 0, 255));
                params.add(new ParamDef("showParticles", "显示粒子", WidgetType.BOOLEAN, "true"));
                break;
            case KILL_ENTITY:
                params.add(new ParamDef("discard", "直接清除", WidgetType.BOOLEAN, "false"));
                break;
            case SUMMON_ENTITY:
                params.add(new ParamDef("entityType", "实体类型", "minecraft:lightning_bolt", RegistryType.ENTITY_TYPE));
                params.add(new ParamDef("count", "数量", WidgetType.SLIDER_PRESET, "1", COUNT_OPTIONS_1_10));
                break;
            case TELEPORT:
                params.add(new ParamDef("x", "X坐标", WidgetType.NUMBER, "0", -30000000, 30000000));
                params.add(new ParamDef("y", "Y坐标", WidgetType.NUMBER, "64", -64, 512));
                params.add(new ParamDef("z", "Z坐标", WidgetType.NUMBER, "0", -30000000, 30000000));
                params.add(new ParamDef("relative", "相对当前位置", WidgetType.BOOLEAN, "false"));
                params.add(new ParamDef("switchDimension", "跨维度传送", WidgetType.BOOLEAN, "false"));
                params.add(new ParamDef("dimension", "目标维度", "minecraft:overworld", RegistryType.DIMENSION));
                break;
            case SET_FIRE:
                params.add(new ParamDef("ticks", "燃烧时间(秒)", WidgetType.NUMBER, "100", 1, 10000, true));
                break;
            case EXPLOSION:
                params.add(new ParamDef("power", "爆炸威力", WidgetType.DROPDOWN, "4.0", EXPLOSION_POWER_OPTIONS));
                params.add(new ParamDef("destroyBlocks", "破坏方块", WidgetType.BOOLEAN, "true"));
                params.add(new ParamDef("createFire", "产生火焰", WidgetType.BOOLEAN, "false"));
                params.add(new ParamDef("atImpact", "命中点爆炸(弹射物)", WidgetType.BOOLEAN, "false"));
                break;
            case GIVE_ITEM:
                params.add(new ParamDef("item", "物品", "minecraft:diamond", RegistryType.ITEM));
                params.add(new ParamDef("count", "数量", WidgetType.SLIDER_PRESET, "1", COUNT_OPTIONS_1_64));
                break;
            case REMOVE_ITEM:
                params.add(new ParamDef("item", "物品", "minecraft:diamond", RegistryType.ITEM));
                params.add(new ParamDef("count", "数量", WidgetType.SLIDER_PRESET, "1", COUNT_OPTIONS_1_64));
                break;
            case PLACE_BLOCK:
                params.add(new ParamDef("block", "方块", "minecraft:stone", RegistryType.BLOCK));
                break;
            case BREAK_BLOCK:
                params.add(new ParamDef("dropItems", "掉落物品", WidgetType.BOOLEAN, "true"));
                break;
            case MODIFY_BLOCK_STATE:
                params.add(new ParamDef("block", "方块", "minecraft:stone", RegistryType.BLOCK));
                break;
            case MODIFY_TIME:
                params.add(new ParamDef("time", "时间", WidgetType.DROPDOWN, "1000", TIME_OPTIONS));
                params.add(new ParamDef("absolute", "绝对时间", WidgetType.BOOLEAN, "false"));
                break;
            case MODIFY_WEATHER:
                params.add(new ParamDef("weather", "天气", WidgetType.DROPDOWN, "clear", WEATHER_OPTIONS));
                params.add(new ParamDef("duration", "持续时长", WidgetType.SLIDER_PRESET, "6000", DURATION_OPTIONS));
                break;
            case MODIFY_GAME_RULE:
                params.add(new ParamDef("gameRule", "游戏规则", WidgetType.DROPDOWN, "doDaylightCycle", GAME_RULE_OPTIONS));
                params.add(new ParamDef("value", "开启/关闭", WidgetType.BOOLEAN, "true"));
                break;
            case SPAWN_PARTICLE:
                params.add(new ParamDef("particle", "粒子效果", "minecraft:flame", RegistryType.PARTICLE));
                params.add(new ParamDef("count", "数量", WidgetType.SLIDER_PRESET, "20", COUNT_OPTIONS_1_64));
                params.add(new ParamDef("spread", "扩散范围", WidgetType.NUMBER, "0.5", 0, 10));
                // 颜色参数：用颜色选择器替代原来的3个数字输入
                params.add(new ParamDef("color", "粒子颜色", WidgetType.COLOR, "255,0,0"));
                params.add(new ParamDef("dustSize", "粒子大小", WidgetType.NUMBER, "1.0", 0, 10));
                break;
            case PLAY_SOUND:
                params.add(new ParamDef("sound", "声音", "minecraft:entity.experience_orb.pickup", RegistryType.SOUND_EVENT));
                params.add(new ParamDef("volume", "音量", WidgetType.NUMBER, "1.0", 0, 10));
                params.add(new ParamDef("pitch", "音调", WidgetType.NUMBER, "1.0", 0, 10));
                break;
            case SEND_MESSAGE:
                params.add(new ParamDef("message", "消息内容", WidgetType.TEXT, "§6[言出法随] §e规则已触发！"));
                params.add(new ParamDef("broadcast", "全服广播", WidgetType.BOOLEAN, "false"));
                break;
            case EXECUTE_COMMAND:
                params.add(new ParamDef("command", "命令", WidgetType.TEXT, "say 言出法随!"));
                params.add(new ParamDef("executeForEach", "每个目标都执行", WidgetType.BOOLEAN, "false"));
                break;
            case MODIFY_NBT:
                params.add(new ParamDef("nbtMode", "操作模式", WidgetType.DROPDOWN, "modify", new String[][]{
                    {"修改值", "modify"}, {"新增字段", "add"}, {"删除字段", "delete"},
                }));
                params.add(new ParamDef("nbtKey", "NBT键名", WidgetType.DROPDOWN, "Health",
                    buildRegistryOptions(RegistryType.NBT_VARIABLE)));
                params.add(new ParamDef("nbtValue", "NBT值", WidgetType.TEXT, ""));
                params.add(new ParamDef("nbtType", "值类型", WidgetType.DROPDOWN, "string", new String[][]{
                    {"文本", "string"}, {"整数", "int"}, {"小数", "double"}, {"开关", "boolean"}, {"复合", "compound"},
                }));
                break;
            case MODIFY_AI:
                params.add(new ParamDef("ai", "启用AI", WidgetType.BOOLEAN, "false"));
                break;
            case MODIFY_ITEM_NBT:
                params.add(new ParamDef("nbtKey", "NBT键名", WidgetType.TEXT, ""));
                params.add(new ParamDef("nbtValue", "NBT值", WidgetType.TEXT, ""));
                params.add(new ParamDef("nbtType", "值类型", WidgetType.DROPDOWN, "string", new String[][]{
                    {"文本", "string"}, {"整数", "int"}, {"小数", "double"}, {"开关", "boolean"}, {"复合", "compound"},
                }));
                break;
            case COPY_ITEM:
                break;
            case CLEAR_EFFECT:
                break;
            case REPLACE_ITEM:
                params.add(new ParamDef("removeItem", "消耗物品", "minecraft:diamond", RegistryType.ITEM));
                params.add(new ParamDef("removeCount", "消耗数量", WidgetType.SLIDER_PRESET, "1", COUNT_OPTIONS_1_64));
                params.add(new ParamDef("giveItem", "获得物品", "minecraft:emerald", RegistryType.ITEM));
                params.add(new ParamDef("giveCount", "获得数量", WidgetType.SLIDER_PRESET, "1", COUNT_OPTIONS_1_64));
                break;
            case ENCHANT_ITEM:
                params.add(new ParamDef("enchantment", "附魔ID", "minecraft:sharpness", RegistryType.ENCHANTMENT));
                params.add(new ParamDef("level", "附魔等级", WidgetType.NUMBER, "1", 1, 255));
                break;
            case SPAWN_EXPERIENCE:
                params.add(new ParamDef("amount", "经验值", WidgetType.NUMBER, "100", 1, 100000));
                break;
            case RUN_OTHER_RULE:
                params.add(new ParamDef("targetRuleName", "目标规则名", WidgetType.TEXT, ""));
                break;
            case SET_VARIABLE:
                params.add(new ParamDef("varKey", "变量名", WidgetType.TEXT, "default"));
                params.add(new ParamDef("varValue", "变量值", WidgetType.TEXT, "1"));
                params.add(new ParamDef("scope", "作用域", WidgetType.DROPDOWN, "player", new String[][]{
                    {"玩家", "player"}, {"全局", "global"},
                }));
                break;
            case MODIFY_VARIABLE:
                params.add(new ParamDef("varKey", "变量名", WidgetType.TEXT, "default"));
                params.add(new ParamDef("amount", "变化量", WidgetType.NUMBER, "1", -100000, 100000));
                params.add(new ParamDef("scope", "作用域", WidgetType.DROPDOWN, "player", new String[][]{
                    {"玩家", "player"}, {"全局", "global"},
                }));
                break;
            case SET_GAMEMODE:
                params.add(new ParamDef("mode", "模式", WidgetType.DROPDOWN, "survival", new String[][]{
                    {"生存", "survival"}, {"创造", "creative"}, {"冒险", "adventure"}, {"旁观", "spectator"},
                }));
                break;
            case CLEAR_INVENTORY:
                break;
            case CLEAR_ITEM:
                params.add(new ParamDef("item", "物品ID", "minecraft:diamond", RegistryType.ITEM));
                params.add(new ParamDef("maxCount", "最多清除数量", WidgetType.NUMBER, "-1", -1, 64));
                break;
            case LOOP:
                params.add(new ParamDef("times", "执行次数", WidgetType.NUMBER, "3", 1, 100));
                params.add(new ParamDef("interval", "间隔(tick)", WidgetType.NUMBER, "20", 1, 1000));
                break;
            case SET_FLYING:
                params.add(new ParamDef("value", "允许飞行", WidgetType.BOOLEAN, "true"));
                break;
            case SET_WALK_SPEED:
                params.add(new ParamDef("value", "行走速度", WidgetType.NUMBER, "0.1", 0, 10, true));
                break;
            case SET_FLY_SPEED:
                params.add(new ParamDef("value", "飞行速度", WidgetType.NUMBER, "0.05", 0, 10, true));
                break;
            case MODIFY_MAX_HEALTH:
                params.add(new ParamDef("amount", "最大生命值", WidgetType.NUMBER, "40", 1, 100000, true));
                break;
            case REPAIR_ITEM:
                params.add(new ParamDef("damage", "修复耐久(0=全修)", WidgetType.NUMBER, "0", 0, 100000));
                break;
            case SPAWN_LIGHTNING:
                params.add(new ParamDef("effectOnly", "仅特效", WidgetType.BOOLEAN, "false"));
                break;
            case SET_INVISIBLE:
                params.add(new ParamDef("value", "隐身", WidgetType.BOOLEAN, "true"));
                break;
            case SET_INVINCIBLE:
                params.add(new ParamDef("value", "无敌", WidgetType.BOOLEAN, "true"));
                break;
            case SATURATE_FOOD:
                params.add(new ParamDef("food", "食物值", WidgetType.NUMBER, "20", 0, 20));
                params.add(new ParamDef("saturation", "饱和值", WidgetType.NUMBER, "5.0", 0, 20));
                break;
            case SET_STEP_HEIGHT:
                params.add(new ParamDef("height", "步高", WidgetType.NUMBER, "1.0", 0, 5, true));
                break;
            case SET_RESISTANCE:
                params.add(new ParamDef("amplifier", "等级", WidgetType.NUMBER, "0", 0, 10));
                params.add(new ParamDef("duration", "时长(tick)", WidgetType.NUMBER, "200", 20, 100000));
                break;
            case SWAP_HAND_ITEMS:
                break;
            case RANDOM_TELEPORT:
                params.add(new ParamDef("range", "范围", WidgetType.SLIDER_PRESET, "50", RANGE_OPTIONS));
                break;
            case SET_AIR:
                params.add(new ParamDef("air", "空气值", WidgetType.NUMBER, "300", 0, 300, true));
                break;
            case MODIFY_ATTRIBUTE:
                params.add(new ParamDef("attribute", "属性ID", "minecraft:generic.movement_speed", RegistryType.ATTRIBUTE));
                params.add(new ParamDef("value", "属性值", WidgetType.NUMBER, "0.1", 0, 100, true));
                break;
            case FREEZE_ENTITY:
                params.add(new ParamDef("ticks", "冻结时长(tick)", WidgetType.NUMBER, "100", 20, 100000, true));
                break;
            case CLEAVE_LAND:
                params.add(new ParamDef("length", "向前长度(格)", WidgetType.NUMBER, "5", 1, 640, true));
                params.add(new ParamDef("width", "宽度(格)", WidgetType.NUMBER, "5", 1, 640, true));
                params.add(new ParamDef("depth", "深度(层)", WidgetType.NUMBER, "5", 1, 640, true));
                params.add(new ParamDef("damage", "伤害值", WidgetType.NUMBER, "5", 0, 10000));
                params.add(new ParamDef("damageType", "伤害类型", "minecraft:generic", RegistryType.DAMAGE_TYPE));
                break;
            case MOD_ADAPTER_EFFECT:
                params.add(new ParamDef("adapterEffectId", "适配器效果",
                    WidgetType.DROPDOWN, "",
                    buildAdapterEffectOptions()));
                params.add(new ParamDef("value", "数值", WidgetType.NUMBER, "0", -100000, 100000, true));
                break;
            case PLACE_STRUCTURE:
                params.add(new ParamDef("structureId", "结构ID", WidgetType.DROPDOWN, "minecraft:village_plains",
                    buildStructureOptions()));
                params.add(new ParamDef("useTargetPos", "目标位置放置", WidgetType.BOOLEAN, "true"));
                params.add(new ParamDef("ignoreEntities", "忽略实体", WidgetType.BOOLEAN, "true"));
                params.add(new ParamDef("integrity", "完整度(%)", WidgetType.NUMBER, "100", 1, 100, true));
                params.add(new ParamDef("seed", "随机种子", WidgetType.NUMBER, "0", 0, 9999999, true));
                break;

            case MASS_REPLACE_BLOCKS:
                params.add(new ParamDef("sourceBlock", "源方块", WidgetType.DROPDOWN, "minecraft:stone", buildSourceBlockOptions(), true));
                params.add(new ParamDef("targetBlock", "目标方块", "minecraft:air", RegistryType.BLOCK));
                params.add(new ParamDef("radiusX", "X轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("radiusY", "Y轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("radiusZ", "Z轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("shape", "形状", WidgetType.DROPDOWN, "cube",
                    new String[][]{{"立方体", "cube"}, {"球体", "sphere"}, {"圆柱体", "cylinder"}}));
                params.add(new ParamDef("useTargetPos", "目标位置为中心", WidgetType.BOOLEAN, "true"));
                break;

            case SMFS_SET_SPIRIT_RESISTANCE:
                params.add(new ParamDef("value", "灵异抗性值", WidgetType.NUMBER, "50", 0, 2147483647, true));
                break;
            case SMFS_SET_SPIRIT_DAMAGE:
                params.add(new ParamDef("value", "灵异伤害值", WidgetType.NUMBER, "10", 0, 2147483647, true));
                break;
            case SMFS_SET_CURRENT_SPIRIT:
                params.add(new ParamDef("value", "当前灵异强度", WidgetType.NUMBER, "100", 0, 2147483647, true));
                break;
            case SMFS_SET_MAX_SPIRIT:
                params.add(new ParamDef("value", "最大灵异强度", WidgetType.NUMBER, "200", 0, 2147483647, true));
                break;
            case SMFS_SET_SANITY:
                params.add(new ParamDef("value", "理智值(0~100)", WidgetType.NUMBER, "100", 0, 100, true));
                break;
            case SMFS_SET_REVIVAL_FACTOR:
                params.add(new ParamDef("value", "复苏因子(0.0~1.0)", WidgetType.NUMBER, "0.3", 0, 100, true));
                break;
            case SMFS_SET_MAIN_GHOST_SLOT:
                params.add(new ParamDef("slotIndex", "槽位索引(0~99)", WidgetType.NUMBER, "0", 0, 99, true));
                break;
            case SMFS_CYCLE_MAIN_GHOST:
                break;
            case SMFS_SET_NBT_ATTRIBUTE:
                params.add(new ParamDef("attributeKey", "属性键名", WidgetType.TEXT, "myCustomAttr"));
                params.add(new ParamDef("value", "属性值", WidgetType.NUMBER, "0", -10000000, 10000000, true));
                break;

            case SMFS_ACTIVATE_DOMAIN:
                params.add(new ParamDef("value", "是否开启", WidgetType.DROPDOWN, "true", new String[][]{
                    {"开启", "true"}, {"关闭", "false"},
                }, true));
                break;
            case SMFS_SET_DOMAIN_TYPE:
                params.add(new ParamDef("color", "鬼域颜色", WidgetType.DROPDOWN, "red",
                    god.bai.god.shen.compat.smfs.SmfsAdapter.buildGhostDomainOptions(), true));
                break;
            case SMFS_SET_DOMAIN_LEVEL:
                params.add(new ParamDef("level", "鬼域等级(1~10)", WidgetType.NUMBER, "1", 1, 10, true));
                break;

            case SMFS_SET_GHOST_EYE_LEVEL:
                params.add(new ParamDef("level", "鬼等级(1~10)", WidgetType.NUMBER, "1", 1, 10, true));
                break;
            case SMFS_TOGGLE_GHOST_EYE:
                params.add(new ParamDef("value", "是否开启", WidgetType.DROPDOWN, "true", new String[][]{
                    {"开启", "true"}, {"关闭", "false"},
                }, true));
                break;

            case SMFS_LOCK_SLOT:
                params.add(new ParamDef("slotIndex", "槽位索引(0~99)", WidgetType.NUMBER, "0", 0, 99, true));
                params.add(new ParamDef("locked", "是否锁定", WidgetType.DROPDOWN, "true", new String[][]{
                    {"锁定", "true"}, {"解锁", "false"},
                }, true));
                break;
            case SMFS_CLEAR_SLOT:
                params.add(new ParamDef("slotIndex", "槽位索引(0~99)", WidgetType.NUMBER, "0", 0, 99, true));
                break;
            case SMFS_SWAP_SLOTS:
                params.add(new ParamDef("slotA", "槽位A(0~99)", WidgetType.NUMBER, "0", 0, 99, true));
                params.add(new ParamDef("slotB", "槽位B(0~99)", WidgetType.NUMBER, "1", 1, 99, true));
                break;
            case SMFS_SET_SLOT_GHOST:
                params.add(new ParamDef("slotIndex", "槽位索引(0~99)", WidgetType.NUMBER, "0", 0, 99, true));
                params.add(new ParamDef("ghostItemId", "选择厉鬼", "", RegistryType.ITEM));
                break;
        }
        return params;
    }

    public static List<ParamDef> getTargetParams(ShenTargetType targetType) {
        List<ParamDef> params = new ArrayList<>();
        switch (targetType) {
            case TYPE_FILTER:
                params.add(new ParamDef("entityType", "实体类型", WidgetType.DROPDOWN, "all", new String[][]{
                    {"所有", "all"}, {"玩家", "player"}, {"怪物", "monster"}, {"动物", "animal"},
                }, true));
                params.add(new ParamDef("range", "范围", WidgetType.SLIDER_PRESET, "50", RANGE_OPTIONS));
                params.add(new ParamDef("includeSelf", "包含自己", WidgetType.BOOLEAN, "false"));
                break;
            case NEARBY_RANGE:
                params.add(new ParamDef("range", "范围", WidgetType.SLIDER_PRESET, "10", RANGE_OPTIONS));
                params.add(new ParamDef("entityType", "实体类型", WidgetType.DROPDOWN, "all", new String[][]{
                    {"所有", "all"}, {"玩家", "player"}, {"怪物", "monster"}, {"动物", "animal"},
                }, true));
                params.add(new ParamDef("includeSource", "包含触发者", WidgetType.BOOLEAN, "false"));
                break;
            case LINE_OF_SIGHT:
                params.add(new ParamDef("range", "范围", WidgetType.SLIDER_PRESET, "50", RANGE_OPTIONS));
                break;
            case SPECIFIC_ENTITY:
                params.add(new ParamDef("playerName", "玩家名", WidgetType.TEXT, ""));
                params.add(new ParamDef("entityName", "生物类型", "minecraft:zombie", RegistryType.ENTITY_TYPE));
                params.add(new ParamDef("uuid", "UUID", WidgetType.TEXT, ""));
                break;
            case ALL_PLAYERS:
                break;
            case RANDOM_ONE:
                params.add(new ParamDef("range", "范围", WidgetType.SLIDER_PRESET, "50", RANGE_OPTIONS));
                params.add(new ParamDef("entityType", "实体类型", WidgetType.DROPDOWN, "all", new String[][]{
                    {"所有", "all"}, {"玩家", "player"}, {"怪物", "monster"}, {"动物", "animal"},
                }, true));
                break;
            case STRUCTURE_BOUNDS:
                params.add(new ParamDef("structureId", "结构ID", WidgetType.DROPDOWN, "minecraft:village_plains",
                    buildStructureOptions()));
                params.add(new ParamDef("range", "搜索范围", WidgetType.SLIDER_PRESET, "200", RANGE_OPTIONS));
                params.add(new ParamDef("entityType", "实体类型", WidgetType.DROPDOWN, "all", new String[][]{
                    {"所有", "all"}, {"玩家", "player"}, {"怪物", "monster"}, {"动物", "animal"},
                }, true));
                break;
            case REGION:
                params.add(new ParamDef("radiusX", "X轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("radiusY", "Y轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("radiusZ", "Z轴半径", WidgetType.NUMBER, "5", 1, 64, true));
                params.add(new ParamDef("centerMode", "中心模式", WidgetType.DROPDOWN, "eventPos",
                    new String[][]{{"事件位置", "eventPos"}, {"事件源", "eventSource"}}));
                break;
            default:
                break;
        }
        return params;
    }

    public static String[][] buildAdapterEffectOptions() {
        return god.bai.god.shen.compat.ModAdapterManager.buildAdapterOptions("effect");
    }

    public static String[][] buildStructureOptions() {
        List<String[]> result = new ArrayList<>();
        Set<String> existingIds = new HashSet<>();

        result.add(new String[]{"村庄平原", "minecraft:village_plains"});
        result.add(new String[]{"村庄沙漠", "minecraft:village_desert"});
        result.add(new String[]{"村庄热带草原", "minecraft:village_savanna"});
        result.add(new String[]{"村庄雪地", "minecraft:village_snowy"});
        result.add(new String[]{"村庄针叶林", "minecraft:village_taiga"});
        result.add(new String[]{"废弃矿井", "minecraft:mineshaft"});
        result.add(new String[]{"废弃矿井(恶地)", "minecraft:mineshaft_mesa"});
        result.add(new String[]{"要塞", "minecraft:stronghold"});
        result.add(new String[]{"沙漠神殿", "minecraft:desert_pyramid"});
        result.add(new String[]{"丛林神庙", "minecraft:jungle_pyramid"});
        result.add(new String[]{"女巫小屋", "minecraft:swamp_hut"});
        result.add(new String[]{"雪屋", "minecraft:igloo"});
        result.add(new String[]{"海底遗迹", "minecraft:monument"});
        result.add(new String[]{"林地府邸", "minecraft:mansion"});
        result.add(new String[]{"末地城", "minecraft:end_city"});
        result.add(new String[]{"沉船", "minecraft:shipwreck"});
        result.add(new String[]{"水下废墟", "minecraft:ocean_ruin"});
        result.add(new String[]{"掠夺者前哨站", "minecraft:pillager_outpost"});
        result.add(new String[]{"堡垒遗迹", "minecraft:bastion_remnant"});
        result.add(new String[]{"下界要塞", "minecraft:fortress"});
        result.add(new String[]{"下界化石", "minecraft:nether_fossil"});
        result.add(new String[]{"废弃传送门", "minecraft:ruined_portal"});
        result.add(new String[]{"远古城市", "minecraft:ancient_city"});
        result.add(new String[]{"埋藏的宝藏", "minecraft:buried_treasure"});
        result.add(new String[]{"古迹废墟", "minecraft:trail_ruins"});

        for (String[] entry : result) {
            existingIds.add(entry[1]);
        }

        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client != null) {
                var server = client.getSingleplayerServer();
                if (server != null) {
                    var structureRegistry = server.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                    if (structureRegistry != null) {
                        for (var entry : structureRegistry.entrySet()) {
                            var id = entry.getKey().location();
                            if (id == null) continue;
                            String idStr = id.toString();
                            if (existingIds.contains(idStr)) continue;
                            String cnName = tryTranslateStructure(id);
                            result.add(new String[]{cnName, idStr});
                            existingIds.add(idStr);
                        }
                    }
                }
                if (client.level != null) {
                    var structureRegistry = client.level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                    if (structureRegistry != null) {
                        for (var entry : structureRegistry.entrySet()) {
                            var id = entry.getKey().location();
                            if (id == null) continue;
                            String idStr = id.toString();
                            if (existingIds.contains(idStr)) continue;
                            String cnName = tryTranslateStructure(id);
                            result.add(new String[]{cnName, idStr});
                            existingIds.add(idStr);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return result.toArray(new String[0][]);
    }

    private static String tryTranslateStructure(ResourceLocation id) {
        String namespace = id.getNamespace();
        String path = id.getPath();

        // 尝试所有可能的翻译键模式（优先级从高到低）
        String[][] keyPatterns = {
            {"structure." + namespace + "." + path},
            {"generator." + namespace + "." + path},
            {"worldgen.structure." + namespace + "." + path},
            {"configured_structure." + namespace + "." + path},
            {"structure_feature." + namespace + "." + path},
            {"feature." + namespace + "." + path},
            {"placed_feature." + namespace + "." + path},
            {"structure." + path},
            {"generator." + path},
        };

        for (String[] patterns : keyPatterns) {
            for (String key : patterns) {
                String translated = I18n.get(key);
                if (!translated.equals(key)) {
                    return translated;
                }
            }
        }

        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                var structure = mc.level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(id);
                if (structure != null) {
                    String name = structure.toString();
                    if (name != null && !name.isEmpty() && name.contains(":")) {
                        String[] parts = name.split(":");
                        if (parts.length > 1) {
                            String objName = parts[1].replace('{', ' ').trim();
                            if (!objName.isEmpty()) {
                                return formatIdToName(new ResourceLocation(namespace, objName));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return formatIdToName(id);
    }

    public static String[][] buildStructureTemplateOptions() {
        List<String[]> result = new ArrayList<>();
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.getResourceManager() != null) {
                java.util.Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources =
                    client.getResourceManager().listResources("structures",
                        path -> path.getPath().endsWith(".nbt"));

                for (ResourceLocation resourceId : resources.keySet()) {
                    String path = resourceId.getPath();
                    if (!path.startsWith("structures/")) continue;
                    String templatePath = path.substring("structures/".length());
                    if (templatePath.endsWith(".nbt")) {
                        templatePath = templatePath.substring(0, templatePath.length() - ".nbt".length());
                    }
                    ResourceLocation templateId = new ResourceLocation(resourceId.getNamespace(), templatePath);
                    String idStr = templateId.toString();
                    String cnName = tryTranslateTemplateName(templateId);
                    result.add(new String[]{cnName, idStr});
                }
            }
        } catch (Exception ignored) {
        }

        if (result.isEmpty()) {
            result.add(new String[]{"（无可用模板）", ""});
        }

        result.sort((a, b) -> a[0].compareTo(b[0]));
        return result.toArray(new String[0][]);
    }

    private static String tryTranslateTemplateName(ResourceLocation id) {
        String path = id.getPath();
        String namespace = id.getNamespace();

        String key1 = "structure." + namespace + "." + path.replace('/', '.');
        String t1 = net.minecraft.client.resources.language.I18n.get(key1);
        if (!t1.equals(key1)) return t1;

        String key2 = "structure." + path.replace('/', '.');
        String t2 = net.minecraft.client.resources.language.I18n.get(key2);
        if (!t2.equals(key2)) return t2;

        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(namespace).append("] ");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public static String[][] buildPlayerOptions() {
        List<String[]> result = new ArrayList<>();
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.getConnection() != null) {
                for (net.minecraft.client.multiplayer.PlayerInfo entry :
                    client.getConnection().getOnlinePlayers()) {
                    String name = entry.getProfile().getName();
                    result.add(new String[]{name, name});
                }
            }
        } catch (Exception ignored) {}
        if (result.isEmpty()) {
            result.add(new String[]{"（无在线玩家）", ""});
        }
        return result.toArray(new String[0][0]);
    }
}