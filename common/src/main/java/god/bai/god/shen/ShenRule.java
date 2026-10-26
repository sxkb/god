package god.bai.god.shen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShenRule {
    public ShenEventType eventType;
    public ShenTargetType targetType;
    public ShenEffectType effectType;
    public List<ShenEventType> eventTypes = new ArrayList<>();
    public List<ShenTargetType> targetTypes = new ArrayList<>();
    public List<ShenEffectEntry> effects = new ArrayList<>();
    public List<ShenCondition> conditions = new ArrayList<>();
    public int priority = 0;
    public String ruleId = "";
    public String name = "";
    public boolean conditionLogic = false;
    public Map<String, String> params = new HashMap<>();

    public ShenRule() {}

    public ShenRule(ShenEventType eventType, ShenTargetType targetType, ShenEffectType effectType) {
        this.eventType = eventType;
        this.targetType = targetType;
        this.effectType = effectType;
        this.eventTypes.add(eventType);
        this.targetTypes.add(targetType);
        this.effects.add(new ShenEffectEntry(effectType));
    }

    public void copyFrom(ShenRule other) {
        this.eventType = other.eventType;
        this.targetType = other.targetType;
        this.effectType = other.effectType;
        this.eventTypes = new ArrayList<>(other.eventTypes);
        this.targetTypes = new ArrayList<>(other.targetTypes);
        this.effects = new ArrayList<>();
        for (ShenEffectEntry entry : other.effects) {
            this.effects.add(new ShenEffectEntry(entry.effectType, new HashMap<>(entry.params), entry.delayTicks));
        }
        this.conditions = new ArrayList<>();
        for (ShenCondition cond : other.conditions) {
            this.conditions.add(new ShenCondition(cond.type, cond.operator, cond.value));
        }
        this.priority = other.priority;
        this.ruleId = other.ruleId;
        this.name = other.name;
        this.conditionLogic = other.conditionLogic;
        this.params = new HashMap<>(other.params);
    }

    public String getParam(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public double getParamDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(params.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getParamInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(params.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getParamBool(String key, boolean defaultValue) {
        String val = params.get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    public double resolveNumber(String key, double defaultValue, double currentValue) {
        double input = getParamDouble(key, defaultValue);
        String op = getParam(key + "_op", "set");
        switch (op) {
            case "add":
                return currentValue + input;
            case "subtract":
                return currentValue - input;
            case "multiply":
                return currentValue * input;
            case "divide":
                return input != 0 ? currentValue / input : currentValue;
            default:
                return input;
        }
    }

    public String getSummary() {
        if (!name.isEmpty()) return name;
        String eventSummary = eventTypes.size() > 1 ? "事件(" + eventTypes.size() + ")" : "当" + eventType.getDisplayName() + "时";
        String summary = eventSummary;
        if (!conditions.isEmpty()) {
            summary += " 若" + getConditionSummary();
        }
        if (!effects.isEmpty()) {
            String targetSummary = targetTypes.size() > 1 ? "目标(" + targetTypes.size() + ")" : targetType.getDisplayName();
            summary += " → 对" + targetSummary;
            if (effects.size() == 1) {
                summary += effects.get(0).effectType.getDisplayName();
            } else {
                summary += "执行" + effects.size() + "个效果";
            }
        }
        return summary;
    }

    private String getConditionSummary() {
        if (conditions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String connector = conditionLogic ? "或" : "且";
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(connector);
            sb.append(conditions.get(i).getSummary());
        }
        return sb.toString();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("eventType", eventType.name());
        nbt.putString("targetType", targetType.name());
        nbt.putString("effectType", effectType != null ? effectType.name() : "");
        nbt.putBoolean("conditionLogic", conditionLogic);
        nbt.putInt("priority", priority);
        if (ruleId != null && !ruleId.isEmpty()) {
            nbt.putString("ruleId", ruleId);
        }
        if (name != null && !name.isEmpty()) {
            nbt.putString("name", name);
        }

        if (eventTypes.size() > 1) {
            ListTag eventList = new ListTag();
            for (ShenEventType et : eventTypes) {
                eventList.add(StringTag.valueOf(et.name()));
            }
            nbt.put("eventTypes", eventList);
        }

        if (targetTypes.size() > 1) {
            ListTag targetList = new ListTag();
            for (ShenTargetType tt : targetTypes) {
                targetList.add(StringTag.valueOf(tt.name()));
            }
            nbt.put("targetTypes", targetList);
        }

        if (!conditions.isEmpty()) {
            ListTag conditionList = new ListTag();
            for (ShenCondition condition : conditions) {
                conditionList.add(condition.toNbt());
            }
            nbt.put("conditions", conditionList);
        }

        if (!effects.isEmpty()) {
            ListTag effectsList = new ListTag();
            for (ShenEffectEntry entry : effects) {
                effectsList.add(entry.toNbt());
            }
            nbt.put("effects", effectsList);
        }

        if (!params.isEmpty()) {
            CompoundTag paramNbt = new CompoundTag();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramNbt.putString(entry.getKey(), entry.getValue());
            }
            nbt.put("params", paramNbt);
        }

        return nbt;
    }

    public static ShenRule fromNbt(CompoundTag nbt) {
        ShenRule rule = new ShenRule();
        rule.eventType = ShenEventType.valueOf(nbt.getString("eventType"));
        rule.targetType = ShenTargetType.valueOf(nbt.getString("targetType"));
        rule.conditionLogic = nbt.getBoolean("conditionLogic");
        rule.priority = nbt.getInt("priority");
        rule.ruleId = nbt.getString("ruleId");
        rule.name = nbt.getString("name");

        rule.eventTypes.add(rule.eventType);
        if (nbt.contains("eventTypes")) {
            ListTag eventList = nbt.getList("eventTypes", Tag.TAG_STRING);
            rule.eventTypes.clear();
            for (int i = 0; i < eventList.size(); i++) {
                rule.eventTypes.add(ShenEventType.valueOf(eventList.getString(i)));
            }
        }

        rule.targetTypes.add(rule.targetType);
        if (nbt.contains("targetTypes")) {
            ListTag targetList = nbt.getList("targetTypes", Tag.TAG_STRING);
            rule.targetTypes.clear();
            for (int i = 0; i < targetList.size(); i++) {
                rule.targetTypes.add(ShenTargetType.valueOf(targetList.getString(i)));
            }
        }

        if (nbt.contains("effects")) {
            ListTag effectsList = nbt.getList("effects", Tag.TAG_COMPOUND);
            for (int i = 0; i < effectsList.size(); i++) {
                rule.effects.add(ShenEffectEntry.fromNbt(effectsList.getCompound(i)));
            }
            if (!rule.effects.isEmpty()) {
                rule.effectType = rule.effects.get(0).effectType;
            }
        } else {
            // 向后兼容：旧格式只有单个 effectType + params
            String effectTypeStr = nbt.getString("effectType");
            if (effectTypeStr != null && !effectTypeStr.isEmpty()) {
                try {
                    rule.effectType = ShenEffectType.valueOf(effectTypeStr);
                    rule.effects.add(new ShenEffectEntry(rule.effectType));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (nbt.contains("conditions")) {
            ListTag conditionList = nbt.getList("conditions", Tag.TAG_COMPOUND);
            for (int i = 0; i < conditionList.size(); i++) {
                rule.conditions.add(ShenCondition.fromNbt(conditionList.getCompound(i)));
            }
        }

        if (nbt.contains("params")) {
            CompoundTag paramNbt = nbt.getCompound("params");
            for (String key : paramNbt.getAllKeys()) {
                rule.params.put(key, paramNbt.getString(key));
            }
            // 向后兼容：旧格式的 params 迁移到第一个 effect entry
            if (!rule.effects.isEmpty() && !rule.params.isEmpty()) {
                rule.effects.get(0).params.putAll(rule.params);
            }
        }

        return rule;
    }

    public static class ShenCondition {
        public ShenConditionType type;
        public String operator;
        public String value;
        public Map<String, String> params = new HashMap<>();

        public ShenCondition() {}

        public ShenCondition(ShenConditionType type, String operator, String value) {
            this.type = type;
            this.operator = operator;
            this.value = value;
        }

        public String getSummary() {
            return type.getDisplayName() + operator + value;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("conditionType", type.name());
            nbt.putString("operator", operator);
            nbt.putString("value", value);
            if (!params.isEmpty()) {
                CompoundTag paramNbt = new CompoundTag();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    paramNbt.putString(entry.getKey(), entry.getValue());
                }
                nbt.put("params", paramNbt);
            }
            return nbt;
        }

        public static ShenCondition fromNbt(CompoundTag nbt) {
            ShenCondition condition = new ShenCondition();
            condition.type = ShenConditionType.valueOf(nbt.getString("conditionType"));
            condition.operator = nbt.getString("operator");
            condition.value = nbt.getString("value");
            if (nbt.contains("params")) {
                CompoundTag paramNbt = nbt.getCompound("params");
                for (String key : paramNbt.getAllKeys()) {
                    condition.params.put(key, paramNbt.getString(key));
                }
            }
            return condition;
        }
    }

    public static class ShenEffectEntry {
        public ShenEffectType effectType;
        public Map<String, String> params;
        public int delayTicks;

        public ShenEffectEntry() {
            this.params = new HashMap<>();
            this.delayTicks = 0;
        }

        public ShenEffectEntry(ShenEffectType effectType) {
            this.effectType = effectType;
            this.params = new HashMap<>();
            this.delayTicks = 0;
        }

        public ShenEffectEntry(ShenEffectType effectType, Map<String, String> params, int delayTicks) {
            this.effectType = effectType;
            this.params = new HashMap<>(params);
            this.delayTicks = delayTicks;
        }

        public String getParam(String key, String defaultValue) {
            return params.getOrDefault(key, defaultValue);
        }

        public double getParamDouble(String key, double defaultValue) {
            try {
                return Double.parseDouble(params.getOrDefault(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public int getParamInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(params.getOrDefault(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public boolean getParamBool(String key, boolean defaultValue) {
            String val = params.get(key);
            if (val == null) return defaultValue;
            return "true".equalsIgnoreCase(val) || "1".equals(val);
        }

        public double resolveNumber(String key, double defaultValue, double currentValue) {
            double input = getParamDouble(key, defaultValue);
            String op = getParam(key + "_op", "set");
            switch (op) {
                case "add":
                    return currentValue + input;
                case "subtract":
                    return currentValue - input;
                case "multiply":
                    return currentValue * input;
                case "divide":
                    return input != 0 ? currentValue / input : currentValue;
                default:
                    return input;
            }
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("effectType", effectType.name());
            nbt.putInt("delayTicks", delayTicks);
            if (!params.isEmpty()) {
                CompoundTag paramNbt = new CompoundTag();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    paramNbt.putString(entry.getKey(), entry.getValue());
                }
                nbt.put("params", paramNbt);
            }
            return nbt;
        }

        public static ShenEffectEntry fromNbt(CompoundTag nbt) {
            ShenEffectEntry entry = new ShenEffectEntry();
            entry.effectType = ShenEffectType.valueOf(nbt.getString("effectType"));
            entry.delayTicks = nbt.getInt("delayTicks");
            if (nbt.contains("params")) {
                CompoundTag paramNbt = nbt.getCompound("params");
                for (String key : paramNbt.getAllKeys()) {
                    entry.params.put(key, paramNbt.getString(key));
                }
            }
            return entry;
        }
    }
}
