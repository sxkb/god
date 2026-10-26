package god.bai.god.shen;

import net.minecraft.network.chat.Component;

public enum ShenEffectType {
    MODIFY_HEALTH("实体操作", "修改生命值"),
    DEAL_DAMAGE("实体操作", "造成伤害"),
    APPLY_EFFECT("实体操作", "应用药水效果"),
    CLEAR_EFFECT("实体操作", "清除药水效果"),
    MODIFY_NBT("实体操作", "修改NBT"),
    MODIFY_AI("实体操作", "修改AI目标"),
    KILL_ENTITY("实体操作", "杀死/清除"),
    SUMMON_ENTITY("实体操作", "召唤实体"),
    TELEPORT("实体操作", "传送"),
    SET_FIRE("实体操作", "点燃"),
    EXPLOSION("实体操作", "爆炸"),
    GIVE_ITEM("物品操作", "给予物品"),
    REMOVE_ITEM("物品操作", "移除物品"),
    MODIFY_ITEM_NBT("物品操作", "修改物品NBT"),
    COPY_ITEM("物品操作", "复制物品"),
    PLACE_BLOCK("方块操作", "放置方块"),
    BREAK_BLOCK("方块操作", "破坏方块"),
    MODIFY_BLOCK_STATE("方块操作", "修改方块状态"),
    MODIFY_TIME("世界操作", "修改时间"),
    MODIFY_WEATHER("世界操作", "修改天气"),
    MODIFY_GAME_RULE("世界操作", "修改游戏规则"),
    SPAWN_PARTICLE("特效", "生成粒子"),
    PLAY_SOUND("特效", "播放音效"),
    SEND_MESSAGE("特效", "发送聊天消息"),
    EXECUTE_COMMAND("命令", "执行任意指令"),
    REPLACE_ITEM("物品操作", "替换物品"),
    ENCHANT_ITEM("物品操作", "给物品附魔"),
    SPAWN_EXPERIENCE("实体操作", "生成经验球"),
    RUN_OTHER_RULE("控制", "触发其他规则"),
    SET_VARIABLE("变量系统", "设置变量"),
    MODIFY_VARIABLE("变量系统", "修改变量"),
    SET_GAMEMODE("玩家操作", "设置玩家模式"),
    CLEAR_INVENTORY("物品操作", "清除背包"),
    CLEAR_ITEM("物品操作", "清除指定物品"),
    LOOP("控制", "循环执行"),
    SET_FLYING("玩家操作", "设置飞行"),
    SET_WALK_SPEED("玩家操作", "行走速度"),
    SET_FLY_SPEED("玩家操作", "飞行速度"),
    MODIFY_MAX_HEALTH("实体操作", "修改最大生命"),
    REPAIR_ITEM("物品操作", "修复物品"),
    SPAWN_LIGHTNING("实体操作", "召唤闪电"),
    SET_INVISIBLE("玩家操作", "设置隐身"),
    SET_INVINCIBLE("玩家操作", "无敌"),
    SATURATE_FOOD("玩家操作", "恢复饱食"),
    SET_STEP_HEIGHT("玩家操作", "设置步高"),
    SET_RESISTANCE("玩家操作", "设置伤害减免"),
    SWAP_HAND_ITEMS("物品操作", "交换主副手"),
    RANDOM_TELEPORT("实体操作", "随机传送"),
    SET_AIR("实体操作", "设置空气值"),
    MODIFY_ATTRIBUTE("实体操作", "修改实体属性"),
    FREEZE_ENTITY("实体操作", "冻结实体"),
    PLACE_STRUCTURE("结构操作", "放置结构"),
    MASS_REPLACE_BLOCKS("方块操作", "移山填海"),
    CLEAVE_LAND("方块操作", "劈大陆"),
    MOD_ADAPTER_EFFECT("适配器效果", "模组适配效果"),

    SMFS_SET_SPIRIT_RESISTANCE("神秘复苏", "设置灵异抗性"),
    SMFS_SET_SPIRIT_DAMAGE("神秘复苏", "设置灵异伤害"),
    SMFS_SET_CURRENT_SPIRIT("神秘复苏", "设置当前灵异强度"),
    SMFS_SET_MAX_SPIRIT("神秘复苏", "设置最大灵异强度"),
    SMFS_SET_SANITY("神秘复苏", "设置理智值"),
    SMFS_SET_REVIVAL_FACTOR("神秘复苏", "设置复苏因子"),
    SMFS_SET_MAIN_GHOST_SLOT("神秘复苏", "设置主鬼槽位"),
    SMFS_CYCLE_MAIN_GHOST("神秘复苏", "切换主鬼"),
    SMFS_SET_NBT_ATTRIBUTE("神秘复苏", "设置任意属性"),

    SMFS_ACTIVATE_DOMAIN("神秘复苏", "开启/关闭鬼域"),
    SMFS_SET_DOMAIN_TYPE("神秘复苏", "设置鬼域颜色"),
    SMFS_SET_DOMAIN_LEVEL("神秘复苏", "设置鬼域等级"),

    SMFS_SET_GHOST_EYE_LEVEL("神秘复苏", "设置鬼等级"),
    SMFS_TOGGLE_GHOST_EYE("神秘复苏", "开关鬼"),

    SMFS_LOCK_SLOT("神秘复苏", "锁定/解锁槽位"),
    SMFS_CLEAR_SLOT("神秘复苏", "清空槽位"),
    SMFS_SWAP_SLOTS("神秘复苏", "交换槽位"),
    SMFS_SET_SLOT_GHOST("神秘复苏", "设置驾驭");

    private final String category;
    private final String displayName;

    ShenEffectType(String category, String displayName) {
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
