package god.bai.god.shen.compat.smfs;

import god.bai.god.shen.compat.ModAdapterManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神秘复苏自动适配器
 */
public class SmfsAdapter {

    private static final String MOD_ID = "smfs";
    private static final String ADAPTER_FILE = "config/god/adapters/smfs_auto.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean initialized = false;
    private static boolean smfsLoaded = false;

    // 缓存发现的实体/效果/物品
    private static final List<String> ghostEntities = new ArrayList<>();
    private static final List<String> smfsEffects = new ArrayList<>();
    private static final List<String> smfsItems = new ArrayList<>();
    private static final Map<String, String> ghostChineseNames = new LinkedHashMap<>();
    private static final Map<String, String> itemChineseNames = new LinkedHashMap<>();
    private static final Map<String, String> effectChineseNames = new LinkedHashMap<>();

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        smfsLoaded = isSmfsLoaded();
        if (!smfsLoaded) {
            System.out.println("[Guiyu-SMFS] 神秘复苏模组未加载，跳过适配");
            return;
        }

        System.out.println("[Guiyu-SMFS] 开始自动索引神秘复苏内容...");

        discoverEntities();
        discoverEffects();
        discoverItems();

        buildAndSaveAdapter();

        registerNbtWatches();

        System.out.println("[Guiyu-SMFS] 神秘复苏自动适配完成 - 实体:" + ghostEntities.size()
            + " 效果:" + smfsEffects.size() + " 物品:" + smfsItems.size());
    }

    public static boolean isSmfsLoaded() {
        try {
            Class.forName("com.xie.smfs.Smfs");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void discoverEntities() {
        ghostEntities.clear();
        ghostChineseNames.clear();

        BuiltInRegistries.ENTITY_TYPE.keySet().stream()
            .filter(id -> id.getNamespace().equals(MOD_ID))
            .forEach(id -> {
                ghostEntities.add(id.toString());
                String chineseName = tryGetChineseName(id);
                ghostChineseNames.put(id.toString(), chineseName);
            });

        System.out.println("[Guiyu-SMFS] 发现 " + ghostEntities.size() + " 个鬼魂实体");
    }

    private static void discoverEffects() {
        smfsEffects.clear();
        effectChineseNames.clear();

        BuiltInRegistries.MOB_EFFECT.keySet().stream()
            .filter(id -> id.getNamespace().equals(MOD_ID))
            .forEach(id -> {
                smfsEffects.add(id.toString());
                String chineseName = tryGetChineseName(id);
                effectChineseNames.put(id.toString(), chineseName);
            });

        System.out.println("[Guiyu-SMFS] 发现 " + smfsEffects.size() + " 个状态效果");
    }

    private static void discoverItems() {
        smfsItems.clear();
        itemChineseNames.clear();

        BuiltInRegistries.ITEM.keySet().stream()
            .filter(id -> id.getNamespace().equals(MOD_ID))
            .forEach(id -> {
                String idStr = id.toString();
                smfsItems.add(idStr);
                String chineseName = tryGetChineseName(id);
                itemChineseNames.put(idStr, chineseName);
            });

        System.out.println("[Guiyu-SMFS] 发现 " + smfsItems.size() + " 个物品");
    }

    private static void buildAndSaveAdapter() {
        JsonObject root = new JsonObject();
        root.addProperty("modId", MOD_ID);
        root.addProperty("displayName", "神秘复苏");

        JsonObject watches = new JsonObject();

        addWatch(watches, "smfs_spiritual_strength", "player.spiritualPower", "灵异强度", "number");
        addWatch(watches, "smfs_sanity", "player.sanity", "理智值", "number");
        addWatch(watches, "smfs_revival_degree", "player.revivalDegree", "复苏程度", "number");
        addWatch(watches, "smfs_ghost_domain", "player.ghostDomain", "鬼域类型", "string");
        addWatch(watches, "smfs_ghost_domain_level", "player.ghostDomainLevel", "鬼域等级", "number");
        addWatch(watches, "smfs_ghost_eye_level", "player.ghostEyeLevel", "鬼眼等级", "number");

        // 每个鬼魂实体的关键属性
        for (String entityId : ghostEntities) {
            addWatch(watches,
                "smfs_" + entityId.replace(":", "_") + "_alive",
                "entity." + entityId + ".isAlive",
                ghostChineseNames.getOrDefault(entityId, entityId) + "存活",
                "boolean");
            addWatch(watches,
                "smfs_" + entityId.replace(":", "_") + "_domain",
                "entity." + entityId + ".ghostDomain",
                ghostChineseNames.getOrDefault(entityId, entityId) + "鬼域",
                "string");
        }

        root.add("nbtWatchPaths", watches);

        JsonArray events = new JsonArray();

        for (String entityId : ghostEntities) {
            JsonObject event = new JsonObject();
            event.addProperty("id", "smfs_entity_spawn_" + entityId.replace(":", "_"));
            event.addProperty("chineseName", ghostChineseNames.getOrDefault(entityId, entityId) + "生成");
            event.addProperty("nbtWatchPath", "entity." + entityId + ".isAlive");
            event.addProperty("triggerCondition", "equals");
            event.addProperty("triggerValue", "true");
            event.addProperty("modId", MOD_ID);
            events.add(event);
        }

        String[] ghostDomains = {"red", "green", "blue", "gray", "golden", "purple", "black", "cyan"};
        String[] domainChinese = {"红色鬼域", "绿色鬼域", "蓝色鬼域", "灰色鬼域", "金色鬼域", "紫色鬼域", "黑色鬼域", "青色鬼域"};
        for (int i = 0; i < ghostDomains.length; i++) {
            for (String entityId : ghostEntities) {
                JsonObject event = new JsonObject();
                event.addProperty("id", "smfs_domain_" + ghostDomains[i] + "_" + entityId.replace(":", "_"));
                event.addProperty("chineseName", ghostChineseNames.getOrDefault(entityId, entityId) + "进入" + domainChinese[i]);
                event.addProperty("nbtWatchPath", "entity." + entityId + ".ghostDomain");
                event.addProperty("triggerCondition", "equals");
                event.addProperty("triggerValue", ghostDomains[i]);
                event.addProperty("modId", MOD_ID);
                events.add(event);
            }
        }

        root.add("customEvents", events);

        JsonArray conditions = new JsonArray();

        addCondition(conditions, "smfs_spiritual_strength", "灵异强度", "player.spiritualPower", "number");
        addCondition(conditions, "smfs_sanity", "理智值", "player.sanity", "number");
        addCondition(conditions, "smfs_revival_degree", "复苏程度", "player.revivalDegree", "number");
        addCondition(conditions, "smfs_ghost_domain", "鬼域类型", "player.ghostDomain", "string");
        addCondition(conditions, "smfs_ghost_domain_level", "鬼域等级", "player.ghostDomainLevel", "number");
        addCondition(conditions, "smfs_ghost_eye_level", "鬼眼等级", "player.ghostEyeLevel", "number");

        for (int i = 0; i < ghostDomains.length; i++) {
            addCondition(conditions,
                "smfs_has_" + ghostDomains[i] + "_domain",
                "拥有" + domainChinese[i],
                "player.ghostDomain", "equals");
        }

        for (String effectId : smfsEffects) {
            addCondition(conditions,
                "smfs_effect_" + effectId.replace(":", "_"),
                "拥有效果:" + effectChineseNames.getOrDefault(effectId, effectId),
                "player.activeEffects." + effectId, "exists");
        }

        root.add("customConditions", conditions);

        JsonArray effects = new JsonArray();

        addEffect(effects, "smfs_spirit_damage", "灵异伤害", "player.spiritualPower", "set", "relative");
        addEffect(effects, "smfs_set_spiritual_strength", "修改灵异强度", "player.spiritualPower", "set", "absolute");
        addEffect(effects, "smfs_set_sanity", "修改理智值", "player.sanity", "set", "absolute");
        addEffect(effects, "smfs_set_revival", "修改复苏程度", "player.revivalDegree", "set", "absolute");
        addEffect(effects, "smfs_set_domain", "设置鬼域类型", "player.ghostDomain", "set", "absolute");

        for (String effectId : smfsEffects) {
            addEffect(effects,
                "smfs_apply_" + effectId.replace(":", "_"),
                "应用:" + effectChineseNames.getOrDefault(effectId, effectId),
                "player.activeEffects." + effectId, "add", "absolute");
        }

        root.add("customEffects", effects);

        try {
            Path adapterPath = Paths.get("config/god/adapters");
            Files.createDirectories(adapterPath);
            Files.writeString(Paths.get(ADAPTER_FILE), GSON.toJson(root));
            System.out.println("[Guiyu-SMFS] 适配器文件已保存: " + ADAPTER_FILE);
        } catch (IOException e) {
            System.err.println("[Guiyu-SMFS] 保存适配器文件失败: " + e.getMessage());
        }
    }

    private static void addWatch(JsonObject watches, String id, String nbtPath, String chineseName, String type) {
        JsonObject watch = new JsonObject();
        watch.addProperty("nbtPath", nbtPath);
        watch.addProperty("chineseName", chineseName);
        watch.addProperty("type", type);
        watches.add(id, watch);
    }

    private static void addCondition(JsonArray conds, String id, String chineseName, String nbtPath, String type) {
        JsonObject cond = new JsonObject();
        cond.addProperty("id", id);
        cond.addProperty("chineseName", chineseName);
        cond.addProperty("nbtPath", nbtPath);
        cond.addProperty("type", type);
        cond.addProperty("modId", MOD_ID);
        conds.add(cond);
    }

    private static void addEffect(JsonArray effs, String id, String chineseName, String nbtPath, String type, String mode) {
        JsonObject eff = new JsonObject();
        eff.addProperty("id", id);
        eff.addProperty("chineseName", chineseName);
        eff.addProperty("nbtPath", nbtPath);
        eff.addProperty("type", type);
        eff.addProperty("mode", mode);
        eff.addProperty("modId", MOD_ID);
        effs.add(eff);
    }

    private static void registerNbtWatches() {
        for (String entityId : ghostEntities) {
            god.bai.god.shen.compat.NbtPathRegistry.register(
                "entity." + entityId + ".isAlive",
                ghostChineseNames.getOrDefault(entityId, entityId) + "存活",
                "SMFS-Adapter"
            );
            god.bai.god.shen.compat.NbtPathRegistry.register(
                "entity." + entityId + ".ghostDomain",
                ghostChineseNames.getOrDefault(entityId, entityId) + "鬼域",
                "SMFS-Adapter"
            );
        }
    }

    private static String tryGetChineseName(ResourceLocation id) {
        String path = id.getPath();

        Map<String, String> knownNames = new LinkedHashMap<>();
        knownNames.put("block_ghost", "堵路鬼");
        knownNames.put("box_ghost", "鬼箱");
        knownNames.put("burn_ghost", "烧伤鬼");
        knownNames.put("clothes_ghost", "鬼衣");
        knownNames.put("crop_ghost", "踩踏鬼");
        knownNames.put("crying_ghost", "哭丧鬼");
        knownNames.put("death_sight_ghost", "死亡之眼鬼");
        knownNames.put("ditou_ghost", "低头鬼");
        knownNames.put("door_ghost", "鬼门");
        knownNames.put("fog_ghost", "迷雾鬼");
        knownNames.put("food_ghost", "饥饿鬼");
        knownNames.put("ganshi_bride_ghost", "干尸新娘");
        knownNames.put("ghost_child", "鬼婴");
        knownNames.put("ghost_entity", "鬼");
        knownNames.put("ghost_master", "驭鬼者");
        knownNames.put("ghost_merchant", "鬼商人");
        knownNames.put("ghost_officer", "鬼差");
        knownNames.put("ghost_pressure", "鬼压人");
        knownNames.put("ghost_slave", "鬼奴");
        knownNames.put("ghost_smoke", "鬼烟");
        knownNames.put("ghost_wind", "鬼风");
        knownNames.put("giant_shadow_ghost", "巨影鬼");
        knownNames.put("gong_ghost", "唢呐鬼");
        knownNames.put("goods_seller_ghost", "货郎鬼");
        knownNames.put("jump_ghost", "跳跳鬼");
        knownNames.put("lost_ghost", "迷路鬼");
        knownNames.put("mineral_ghost", "矿物鬼");
        knownNames.put("plague_ghost", "瘟疫鬼");
        knownNames.put("player_ghost", "玩家鬼");
        knownNames.put("puppet_ghost", "傀儡鬼");
        knownNames.put("qiaomen_ghost", "敲门鬼");
        knownNames.put("shadow_ghost", "影子鬼");
        knownNames.put("silent_ghost", "静悄悄鬼");
        knownNames.put("sneak_ghost", "潜行鬼");
        knownNames.put("step_ghost", "踩脚印鬼");
        knownNames.put("suona_ghost", "唢呐鬼");
        knownNames.put("taitou_ghost", "抬头鬼");
        knownNames.put("trash_ghost", "垃圾鬼");
        knownNames.put("untouchable_ghost", "不可触碰鬼");
        knownNames.put("villager_ghost", "村民鬼");
        knownNames.put("water_ghost", "水鬼");
        knownNames.put("cao_yang", "曹阳");
        knownNames.put("chen_doctor", "陈博士");
        knownNames.put("fang_shi_min", "方世明");
        knownNames.put("feng_quan", "冯全");
        knownNames.put("li_jun", "李军");
        knownNames.put("li_le_ping", "李乐平");
        knownNames.put("wang_xiao_ming", "王小强");
        knownNames.put("yan_li", "严力");
        knownNames.put("yang_jian", "杨戬");
        knownNames.put("ye_zhen", "叶真");
        knownNames.put("zhao_kai_ming", "赵开明");

        Map<String, String> effectNames = new LinkedHashMap<>();
        effectNames.put("red_ghost_domain", "红色鬼域");
        effectNames.put("green_ghost_domain", "绿色鬼域");
        effectNames.put("blue_ghost_domain", "蓝色鬼域");
        effectNames.put("gray_ghost_domain", "灰色鬼域");
        effectNames.put("golden_ghost_domain", "金色鬼域");
        effectNames.put("purple_ghost_domain", "紫色鬼域");
        effectNames.put("black_ghost_domain", "黑色鬼域");
        effectNames.put("cyan_ghost_domain", "青色鬼域");
        effectNames.put("deafness", "失聪");
        effectNames.put("fatal_poison", "致命毒");
        effectNames.put("ghost_knock", "鬼敲门");
        effectNames.put("ghost_pressure", "鬼压人");
        effectNames.put("ghost_suppression", "鬼压制");
        effectNames.put("ghost_wind_effect", "鬼风");
        effectNames.put("illusion_curse", "幻象诅咒");
        effectNames.put("knocking_curse", "敲门诅咒");
        effectNames.put("lost_status", "迷路");
        effectNames.put("mark_curse", "标记诅咒");
        effectNames.put("plague", "瘟疫");
        effectNames.put("purification", "净化");
        effectNames.put("scapegoat_mark", "替死标记");
        effectNames.put("silence", "沉默");
        effectNames.put("spirit_erosion", "灵异侵蚀");
        effectNames.put("spirit_immunity", "灵异免疫");
        effectNames.put("spirit_surge", "灵异激增");
        effectNames.put("starving_ghost_curse", "饿死鬼诅咒");
        effectNames.put("thick_fog", "浓雾");
        effectNames.put("trauma_curse", "创伤诅咒");
        effectNames.put("wish_curse", "愿望诅咒");

        if (knownNames.containsKey(path)) return knownNames.get(path);
        if (effectNames.containsKey(path)) return effectNames.get(path);

        // fallback: 驼峰转中文
        return path.replace('_', ' ');
    }

    public static List<ResourceLocation> getGhostEntityIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (String id : ghostEntities) {
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier != null) ids.add(identifier);
        }
        return ids;
    }

    public static List<ResourceLocation> getEffectIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (String id : smfsEffects) {
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier != null) ids.add(identifier);
        }
        return ids;
    }

    public static Object getGhostAPI() {
        try {
            Class<?> ghostAPIClass = Class.forName("com.xie.smfs.api.GhostAPI");
            return ghostAPIClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getTameableItemAPI() {
        try {
            Class<?> apiClass = Class.forName("com.xie.smfs.api.TameableItemAPI");
            return apiClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasTamedGhost(Player player, String ghostType) {
        return god.bai.god.support.smfs.SmfsAPIWrapper.hasTamedGhost(player, ghostType);
    }

    public static int getSpiritualPower(Player player) {
        return god.bai.god.support.smfs.SmfsAPIWrapper.getSpiritualPower(player);
    }

    public static boolean isGhostDomainActive(Player player) {
        return god.bai.god.support.smfs.SmfsAPIWrapper.isGhostDomainActive(player);
    }

    public static boolean hasGhostDomain(Player player, String domainColor) {
        switch (domainColor.toLowerCase()) {
            case "red": return god.bai.god.support.smfs.SmfsAPIWrapper.hasRedGhostDomain(player);
            case "green": return god.bai.god.support.smfs.SmfsAPIWrapper.hasGreenGhostDomain(player);
            case "blue": return god.bai.god.support.smfs.SmfsAPIWrapper.hasBlueGhostDomain(player);
            case "gray": return god.bai.god.support.smfs.SmfsAPIWrapper.hasGrayGhostDomain(player);
            case "golden": return god.bai.god.support.smfs.SmfsAPIWrapper.hasGoldenGhostDomain(player);
            case "purple": return god.bai.god.support.smfs.SmfsAPIWrapper.hasPurpleGhostDomain(player);
            case "black": return god.bai.god.support.smfs.SmfsAPIWrapper.hasBlackGhostDomain(player);
            case "cyan": return god.bai.god.support.smfs.SmfsAPIWrapper.hasCyanGhostDomain(player);
            default: return false;
        }
    }

    private static Map<String, String> scanGhostEntities() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> id.getNamespace().equals(MOD_ID))
                .forEach(id -> {
                    String chineseName = tryGetChineseName(id);
                    result.put(id.toString(), chineseName);
                });
        } catch (Exception e) {
            // 安全兜底
        }
        return result;
    }

    public static String[][] buildGhostTypeOptions() {
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"§7无", ""});
        Set<String> added = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String idStr = id.toString();
            if (added.contains(idStr)) continue;
            boolean isSmfsEntity = id.getNamespace().equals(MOD_ID)
                || id.getPath().contains("ghost")
                || id.getPath().contains("smfs");
            if (!isSmfsEntity) continue;
            var entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (entityType == null) continue;
            String cnName = Component.translatable(entityType.getDescriptionId()).getString();
            if (cnName.equals(entityType.getDescriptionId())) {
                cnName = tryGetChineseName(id);
            }
            added.add(idStr);
            result.add(new String[]{cnName, idStr});
        }
        return result.toArray(new String[0][0]);
    }

    private static List<String> scanGhostItems() {
        List<String> result = new ArrayList<>();
        try {
            BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(MOD_ID))
                .forEach(id -> result.add(id.toString()));
        } catch (Exception e) {
            // 安全兜底
        }
        return result;
    }

    public static String[][] buildGhostItemOptions() {
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"§7无", ""});

        Set<String> added = new HashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            String idStr = id.toString();
            if (added.contains(idStr)) continue;
            boolean isSmfsItem = id.getNamespace().equals(MOD_ID)
                || id.getPath().contains("ghost")
                || id.getPath().contains("smfs");
            if (!isSmfsItem) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null) continue;
            String cnName = Component.translatable(item.getDescriptionId()).getString();
            if (cnName.equals(item.getDescriptionId())) {
                cnName = tryGetChineseName(id);
            }
            added.add(idStr);
            result.add(new String[]{cnName, idStr});
        }
        if (result.size() <= 1) {
            Map<String, String> entities = scanGhostEntities();
            for (Map.Entry<String, String> entry : entities.entrySet()) {
                result.add(new String[]{entry.getValue(), entry.getKey()});
            }
        }
        return result.toArray(new String[0][0]);
    }

    public static String[][] buildGhostDomainOptions() {
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{"§7无", ""});
        result.add(new String[]{"红色鬼域", "red"});
        result.add(new String[]{"绿色鬼域", "green"});
        result.add(new String[]{"蓝色鬼域", "blue"});
        result.add(new String[]{"灰色鬼域", "gray"});
        result.add(new String[]{"金色鬼域", "golden"});
        result.add(new String[]{"紫色鬼域", "purple"});
        result.add(new String[]{"黑色鬼域", "black"});
        result.add(new String[]{"青色鬼域", "cyan"});
        return result.toArray(new String[0][0]);
    }

    public static String getGhostChineseName(String ghostTypeId) {
        if (ghostTypeId == null || ghostTypeId.isEmpty()) return "无";
        return ghostChineseNames.getOrDefault(ghostTypeId, ghostTypeId);
    }

    public static void refresh() {
        initialized = false;
        ghostEntities.clear();
        smfsEffects.clear();
        smfsItems.clear();
        ghostChineseNames.clear();
        effectChineseNames.clear();
        initialize();
        ModAdapterManager.loadAllAdapters();
    }
}
