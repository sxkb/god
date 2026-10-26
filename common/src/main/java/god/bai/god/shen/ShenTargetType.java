package god.bai.god.shen;

import net.minecraft.network.chat.Component;

public enum ShenTargetType {
    EVENT_SOURCE("事件源", "触发事件的实体"),
    ATTACKED_TARGET("攻击目标", "被攻击的生物"),
    TYPE_FILTER("类型筛选", "按类型筛选实体"),
    NEARBY_RANGE("附近范围", "事件源周围X格"),
    LINE_OF_SIGHT("视线指向", "准星指向的目标"),
    SPECIFIC_ENTITY("特定实体", "通过UUID或名称指定"),
    ALL_PLAYERS("全局目标", "所有在线玩家"),
    RANDOM_ONE("随机选择", "从目标中随机选一个"),
    STRUCTURE_BOUNDS("结构", "结构范围内实体"),
    REGION("区域", "指定区域");

    private final String category;
    private final String displayName;

    ShenTargetType(String category, String displayName) {
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
