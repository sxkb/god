package god.bai.god.shen;

import net.minecraft.network.chat.Component;

public enum ShenEventType {
    PLAYER_RIGHT_CLICK("玩家事件", "右键点击"),
    PLAYER_ATTACK("玩家事件", "攻击生物"),
    PLAYER_SNEAK("玩家事件", "潜行"),
    PLAYER_JUMP("玩家事件", "跳跃"),
    PLAYER_HURT("玩家事件", "受伤"),
    PLAYER_DEATH("玩家事件", "死亡"),
    PLAYER_RESPAWN("玩家事件", "重生"),
    PLAYER_DIMENSION_CHANGE("玩家事件", "切换维度"),
    PLAYER_JOIN_WORLD("玩家事件", "进入世界"),
    PLAYER_LEAVE_WORLD("玩家事件", "离开世界"),
    PLAYER_CHAT("玩家事件", "聊天"),
    PLAYER_EAT("玩家事件", "进食"),
    ITEM_USE("玩家事件", "使用物品"),
    ENTITY_SPAWN("实体事件", "生物生成"),
    ENTITY_DEATH("实体事件", "生物死亡"),
    ENTITY_HURT("实体事件", "生物受伤"),
    ENTITY_RIGHT_CLICK("实体事件", "右键实体"),
    BLOCK_BREAK("方块事件", "方块被破坏"),
    BLOCK_PLACE("方块事件", "方块被放置"),
    BLOCK_RIGHT_CLICK("方块事件", "右键方块"),
    BLOCK_INTERACT("方块事件", "交互方块"),
    WORLD_TIME_CHANGE("世界事件", "世界时间变化"),
    WORLD_WEATHER_CHANGE("世界事件", "天气变化"),
    PLAYER_ITEM_PICKUP("玩家事件", "拾取物品"),
    PLAYER_ITEM_DROP("玩家事件", "丢弃物品"),
    PLAYER_CONSUME_ITEM("玩家事件", "消耗物品"),
    PLAYER_BED_ENTER("玩家事件", "上床睡眠"),
    PLAYER_BED_LEAVE("玩家事件", "起床"),
    ENTITY_TARGET("实体事件", "被生物锁定"),
    PROJECTILE_HIT("实体事件", "弹射物击中"),
    PLAYER_ADVANCEMENT("玩家事件", "获得进度"),
    STRUCTURE_GENERATED("结构事件", "结构生成"),
    MOD_ADAPTER_EVENT("适配器事件", "模组适配事件"),

    SMFS_SLOT_CHANGE("神秘复苏", "槽位变化"),
    SMFS_SPIRIT_ATTR_CHANGE("神秘复苏", "灵异属性变化"),
    SMFS_MAIN_GHOST_SWITCH("神秘复苏", "主鬼切换"),
    SMFS_GHOST_SKILL_USED("神秘复苏", "鬼技能使用");

    private final String category;
    private final String displayName;

    ShenEventType(String category, String displayName) {
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
