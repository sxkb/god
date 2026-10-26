package god.bai.god.shen;

import net.minecraft.network.chat.Component;

public enum ShenConditionType {
    HEALTH_COMPARE("属性比较", "生命值"),
    EXPERIENCE_COMPARE("属性比较", "经验值"),
    NBT_COMPARE("属性比较", "NBT值"),
    ITEM_IN_HAND("物品检查", "手持物品"),
    ITEM_IN_INVENTORY("物品检查", "背包物品"),
    HAS_EFFECT("状态检查", "拥有药水效果"),
    IN_DIMENSION("状态检查", "在特定维度"),
    RANDOM_CHANCE("随机概率", "概率触发"),
    CHAT_MESSAGE("文本匹配", "聊天内容"),
    TIME_RANGE("世界条件", "时间范围"),
    WEATHER_CONDITION("世界条件", "天气条件"),
    BIOME("世界条件", "生物群系"),
    LIGHT_LEVEL("世界条件", "光照等级"),
    IS_SNEAKING("玩家状态", "是否潜行"),
    IS_SPRINTING("玩家状态", "是否疾跑"),
    ITEM_HAS_ENCHANTMENT("物品检查", "物品附魔"),
    IS_ON_FIRE("状态检查", "是否着火"),
    IS_IN_WATER("状态检查", "是否在水中"),
    IS_IN_LAVA("状态检查", "是否在岩浆中"),
    PLAYER_DISTANCE("属性比较", "实体距离"),
    VARIABLE_COMPARE("变量系统", "变量比较"),
    GAMEMODE("玩家状态", "玩家模式"),
    FOOD_LEVEL("属性比较", "饱食度"),
    PLAYER_LEVEL("属性比较", "经验等级"),
    ENTITY_TYPE("状态检查", "实体类型"),
    ENTITY_EXISTS("状态检查", "实体存在"),
    IS_ELYTRA_FLYING("玩家状态", "鞘翅飞行"),
    IS_WEARING_ARMOR("物品检查", "穿戴盔甲"),
    BLOCK_BELOW("世界条件", "下方方块"),
    BLOCK_ABOVE("世界条件", "上方方块"),
    PERMISSION_LEVEL("玩家状态", "权限等级"),
    IS_GLIDING("玩家状态", "滑翔"),
    IS_SWIMMING("玩家状态", "游泳"),
    IS_RIDING("玩家状态", "骑乘实体"),
    ATTRIBUTE_VALUE("属性比较", "属性值"),
    IS_WEARING_ITEM("物品检查", "穿戴指定物品"),
    BLOCK_IN_HAND("物品检查", "手持方块"),
    ITEM_NAME("物品检查", "物品名称"),
    MOON_PHASE("世界条件", "月相"),
    DIFFICULTY("世界条件", "游戏难度"),
    IN_STRUCTURE("世界条件", "在结构中"),
    STRUCTURE_DISTANCE("世界条件", "结构距离"),
    MOD_ADAPTER_CONDITION("适配器条件", "模组适配条件"),
    SPECIFIC_PLAYER("玩家状态", "指定玩家"),
    IS_ATTACKING("玩家状态", "正在攻击"),
    NONE("特殊条件", "无条件"),

    SMFS_HAS_GHOST_TYPE("神秘复苏", "拥有指定鬼类型"),
    SMFS_SLOT_OCCUPIED("神秘复苏", "槽位是否占用"),
    SMFS_HAS_MAIN_GHOST("神秘复苏", "是否有主鬼"),
    SMFS_SPIRIT_ATTR_COMPARE("神秘复苏", "灵异属性比较"),
    SMFS_GHOST_POSSESSION("神秘复苏", "复苏程度比较"),
    SMFS_MAIN_GHOST_TYPE("神秘复苏", "主鬼类型"),

    SMFS_HAS_GHOST_DOMAIN("神秘复苏", "是否开启鬼域"),
    SMFS_GHOST_DOMAIN_TYPE("神秘复苏", "鬼域颜色类型"),
    SMFS_GHOST_DOMAIN_LEVEL("神秘复苏", "鬼域等级比较"),

    SMFS_HAS_GHOST_EYE("神秘复苏", "是否开启鬼"),
    SMFS_GHOST_EYE_LEVEL("神秘复苏", "鬼等级比较"),

    SMFS_SLOT_LEVEL_COMPARE("神秘复苏", "槽位等级比较"),
    SMFS_SLOT_COUNT_COMPARE("神秘复苏", "已驾驭数量比较");

    private final String category;
    private final String displayName;

    ShenConditionType(String category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Component toText() {
        return Component.literal(category + " · " + displayName);
    }
}
