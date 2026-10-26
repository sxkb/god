package god.bai.god.shen.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ModAdapterManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ADAPTER_DIR = "config/god/adapters";
    private static final List<ModAdapterEntry> loadedAdapters = new ArrayList<>();
    private static final Map<String, NbtWatchEntry> nbtWatchMap = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, String>> previousNbtValues = new HashMap<>();

    private static final Map<String, Consumer<LivingEntity>> eventCallbacks = new LinkedHashMap<>();

    public static class ModAdapterEntry {
        public String modId;
        public String displayName;
        public List<AdapterEvent> customEvents = new ArrayList<>();
        public List<AdapterCondition> customConditions = new ArrayList<>();
        public List<AdapterEffect> customEffects = new ArrayList<>();
        public List<NbtWatchEntry> nbtWatches = new ArrayList<>();
    }

    public static class NbtWatchEntry {
        public String id;
        public String nbtPath;
        public String chineseName;
        public String type;
        public String modId;
    }

    public static class AdapterEvent {
        public String id;
        public String chineseName;
        public String nbtWatchPath;
        public String triggerCondition;
        public String triggerValue;
        public String modId;
    }

    public static class AdapterCondition {
        public String id;
        public String chineseName;
        public String nbtPath;
        public String type;
        public String modId;
    }

    public static class AdapterEffect {
        public String id;
        public String chineseName;
        public String nbtPath;
        public String type;
        public String mode;
        public String modId;
    }

    public static void loadAllAdapters() {
        loadedAdapters.clear();
        nbtWatchMap.clear();
        eventCallbacks.clear();
        previousNbtValues.clear();

        god.bai.god.shen.compat.NbtPathRegistry.load();
        rebuildVirtualAdapter();

        Path adapterPath = Paths.get(ADAPTER_DIR);
        if (!Files.exists(adapterPath)) {
            try { Files.createDirectories(adapterPath); } catch (IOException ignored) {}
            generateExampleAdapters();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(adapterPath, "*.json")) {
            for (Path path : stream) {
                try {
                    String json = Files.readString(path);
                    JsonObject root = GSON.fromJson(json, JsonObject.class);
                    ModAdapterEntry entry = parseAdapter(root);
                    if (entry != null) {
                        loadedAdapters.add(entry);
                        registerAdapter(entry);
                        System.out.println("[GuiyuCompat] Loaded adapter: " + entry.modId);
                    }
                } catch (Exception e) {
                    System.err.println("[GuiyuCompat] Failed to load adapter: " + path + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[GuiyuCompat] Failed to scan adapter directory: " + e.getMessage());
        }
    }

    private static ModAdapterEntry parseAdapter(JsonObject root) {
        ModAdapterEntry entry = new ModAdapterEntry();
        entry.modId = root.get("modId").getAsString();
        entry.displayName = root.has("displayName") ? root.get("displayName").getAsString() : entry.modId;

        if (root.has("nbtWatchPaths")) {
            JsonObject watches = root.getAsJsonObject("nbtWatchPaths");
            for (Map.Entry<String, JsonElement> e : watches.entrySet()) {
                JsonObject watchObj = e.getValue().getAsJsonObject();
                NbtWatchEntry watch = new NbtWatchEntry();
                watch.id = e.getKey();
                watch.nbtPath = watchObj.get("nbtPath").getAsString();
                watch.chineseName = watchObj.get("chineseName").getAsString();
                watch.type = watchObj.has("type") ? watchObj.get("type").getAsString() : "number";
                watch.modId = entry.modId;
                entry.nbtWatches.add(watch);
            }
        }

        if (root.has("customEvents")) {
            JsonArray events = root.getAsJsonArray("customEvents");
            for (JsonElement e : events) {
                JsonObject evtObj = e.getAsJsonObject();
                AdapterEvent evt = new AdapterEvent();
                evt.id = evtObj.get("id").getAsString();
                evt.chineseName = evtObj.get("chineseName").getAsString();
                evt.nbtWatchPath = evtObj.get("nbtWatchPath").getAsString();
                evt.triggerCondition = evtObj.has("triggerCondition") ? evtObj.get("triggerCondition").getAsString() : "changed";
                evt.triggerValue = evtObj.has("triggerValue") ? evtObj.get("triggerValue").getAsString() : "";
                evt.modId = entry.modId;
                entry.customEvents.add(evt);
            }
        }

        if (root.has("customConditions")) {
            JsonArray conditions = root.getAsJsonArray("customConditions");
            for (JsonElement e : conditions) {
                JsonObject condObj = e.getAsJsonObject();
                AdapterCondition cond = new AdapterCondition();
                cond.id = condObj.get("id").getAsString();
                cond.chineseName = condObj.get("chineseName").getAsString();
                cond.nbtPath = condObj.get("nbtPath").getAsString();
                cond.type = condObj.has("type") ? condObj.get("type").getAsString() : "number";
                cond.modId = entry.modId;
                entry.customConditions.add(cond);
            }
        }

        if (root.has("customEffects")) {
            JsonArray effects = root.getAsJsonArray("customEffects");
            for (JsonElement e : effects) {
                JsonObject effObj = e.getAsJsonObject();
                AdapterEffect eff = new AdapterEffect();
                eff.id = effObj.get("id").getAsString();
                eff.chineseName = effObj.get("chineseName").getAsString();
                eff.nbtPath = effObj.get("nbtPath").getAsString();
                eff.type = effObj.has("type") ? effObj.get("type").getAsString() : "number";
                eff.mode = effObj.has("mode") ? effObj.get("mode").getAsString() : "set";
                eff.modId = entry.modId;
                entry.customEffects.add(eff);
            }
        }

        return entry;
    }

    private static void registerAdapter(ModAdapterEntry entry) {
        for (NbtWatchEntry watch : entry.nbtWatches) {
            String fullId = entry.modId + ":" + watch.id;
            nbtWatchMap.put(fullId, watch);
        }
    }

    private static final String VIRTUAL_ADAPTER_ID = "_player_watched";

    private static void rebuildVirtualAdapter() {
        loadedAdapters.removeIf(a -> VIRTUAL_ADAPTER_ID.equals(a.modId));

        ModAdapterEntry virtual = new ModAdapterEntry();
        virtual.modId = VIRTUAL_ADAPTER_ID;
        virtual.displayName = "玩家注册";

        for (god.bai.god.shen.compat.NbtPathRegistry.WatchedPath wp :
                god.bai.god.shen.compat.NbtPathRegistry.getAll()) {
            NbtWatchEntry watch = new NbtWatchEntry();
            watch.id = wp.nbtPath;
            watch.nbtPath = wp.nbtPath;
            watch.chineseName = wp.displayName;
            watch.type = "number";
            watch.modId = VIRTUAL_ADAPTER_ID;
            virtual.nbtWatches.add(watch);

            AdapterEvent evt = new AdapterEvent();
            evt.id = "changed_" + sanitizeId(wp.nbtPath);
            evt.chineseName = wp.displayName + "变化";
            evt.nbtWatchPath = VIRTUAL_ADAPTER_ID + ":" + wp.nbtPath;
            evt.triggerCondition = "changed";
            evt.modId = VIRTUAL_ADAPTER_ID;
            virtual.customEvents.add(evt);

            AdapterCondition cond = new AdapterCondition();
            cond.id = "compare_" + sanitizeId(wp.nbtPath);
            cond.chineseName = wp.displayName + "比较";
            cond.nbtPath = wp.nbtPath;
            cond.type = "number";
            cond.modId = VIRTUAL_ADAPTER_ID;
            virtual.customConditions.add(cond);

            AdapterEffect eff = new AdapterEffect();
            eff.id = "modify_" + sanitizeId(wp.nbtPath);
            eff.chineseName = "修改" + wp.displayName;
            eff.nbtPath = wp.nbtPath;
            eff.type = "number";
            eff.mode = "set";
            eff.modId = VIRTUAL_ADAPTER_ID;
            virtual.customEffects.add(eff);
        }

        if (!virtual.nbtWatches.isEmpty()) {
            loadedAdapters.add(virtual);
            registerAdapter(virtual);
        }
    }

    private static String sanitizeId(String nbtPath) {
        return nbtPath.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    public static void registerWatchedPath(String nbtPath, String displayName) {
        NbtWatchEntry watch = new NbtWatchEntry();
        watch.id = nbtPath;
        watch.nbtPath = nbtPath;
        watch.chineseName = displayName;
        watch.type = "number";
        watch.modId = VIRTUAL_ADAPTER_ID;
        nbtWatchMap.put(VIRTUAL_ADAPTER_ID + ":" + nbtPath, watch);
        rebuildVirtualAdapter();
    }

    public static void unregisterWatchedPath(String nbtPath) {
        nbtWatchMap.remove(VIRTUAL_ADAPTER_ID + ":" + nbtPath);
        rebuildVirtualAdapter();
    }

    private static void generateExampleAdapters() {
        Path adapterPath = Paths.get(ADAPTER_DIR);
    }

    public static List<ModAdapterEntry> getLoadedAdapters() {
        return Collections.unmodifiableList(loadedAdapters);
    }

    public static List<AdapterEvent> getAllAdapterEvents() {
        List<AdapterEvent> all = new ArrayList<>();
        for (ModAdapterEntry entry : loadedAdapters) {
            all.addAll(entry.customEvents);
        }
        return all;
    }

    public static List<AdapterCondition> getAllAdapterConditions() {
        List<AdapterCondition> all = new ArrayList<>();
        for (ModAdapterEntry entry : loadedAdapters) {
            all.addAll(entry.customConditions);
        }
        return all;
    }

    public static List<AdapterEffect> getAllAdapterEffects() {
        List<AdapterEffect> all = new ArrayList<>();
        for (ModAdapterEntry entry : loadedAdapters) {
            all.addAll(entry.customEffects);
        }
        return all;
    }

    public static NbtWatchEntry getNbtWatch(String id) {
        return nbtWatchMap.get(id);
    }

    public static String[][] buildAdapterOptions(String type) {
        List<String[]> result = new ArrayList<>();
        switch (type) {
            case "event":
                for (ModAdapterEntry entry : loadedAdapters) {
                    for (AdapterEvent evt : entry.customEvents) {
                        result.add(new String[]{
                            "[" + entry.displayName + "] " + evt.chineseName,
                            entry.modId + ":" + evt.id
                        });
                    }
                }
                break;
            case "condition":
                for (ModAdapterEntry entry : loadedAdapters) {
                    for (AdapterCondition cond : entry.customConditions) {
                        result.add(new String[]{
                            "[" + entry.displayName + "] " + cond.chineseName,
                            entry.modId + ":" + cond.id
                        });
                    }
                }
                break;
            case "effect":
                for (ModAdapterEntry entry : loadedAdapters) {
                    for (AdapterEffect eff : entry.customEffects) {
                        result.add(new String[]{
                            "[" + entry.displayName + "] " + eff.chineseName,
                            entry.modId + ":" + eff.id
                        });
                    }
                }
                break;
        }
        return result.toArray(new String[0][0]);
    }

    public static void checkEntityNbtChanges(LivingEntity entity) {
        if (entity == null) return;
        if (entity.isRemoved()) return;
        if (entity.level() == null) return;

        UUID uuid = entity.getUUID();
        CompoundTag entityNbt = entity.saveWithoutId(new CompoundTag());
        Map<String, String> prevValues = previousNbtValues.computeIfAbsent(uuid, k -> new HashMap<>());

        for (Map.Entry<String, NbtWatchEntry> watchEntry : nbtWatchMap.entrySet()) {
            String fullId = watchEntry.getKey();
            NbtWatchEntry watch = watchEntry.getValue();
            String currentStr = readNbtPath(entityNbt, watch.nbtPath);
            String prevStr = prevValues.get(fullId);

            if (currentStr == null) continue;

            boolean changed = !currentStr.equals(prevStr);
            prevValues.put(fullId, currentStr);

            if (!changed && !"true".equals(currentStr)) continue;

            for (ModAdapterEntry adapter : loadedAdapters) {
                for (AdapterEvent evt : adapter.customEvents) {
                    String evtFullId = adapter.modId + ":" + evt.nbtWatchPath;
                    if (!evtFullId.equals(fullId)) continue;

                    boolean triggered = false;
                    if ("changed".equals(evt.triggerCondition)) {
                        triggered = changed;
                    } else if (changed && evt.triggerCondition != null && !evt.triggerValue.isEmpty()) {
                        try {
                            double cur = Double.parseDouble(currentStr);
                            double trig = Double.parseDouble(evt.triggerValue);
                            switch (evt.triggerCondition) {
                                case ">=": triggered = cur >= trig; break;
                                case "<=": triggered = cur <= trig; break;
                                case "==": triggered = Math.abs(cur - trig) < 0.001; break;
                                case ">": triggered = cur > trig; break;
                                case "<": triggered = cur < trig; break;
                                case "!=": triggered = Math.abs(cur - trig) >= 0.001; break;
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    if (triggered) {
                        god.bai.god.shen.ShenEventBus.fire(
                            new god.bai.god.shen.ShenEvent(
                                god.bai.god.shen.ShenEventType.MOD_ADAPTER_EVENT,
                                entity, entity.blockPosition(), entity.level()
                            ) {
                                { this.params.put("adapterEventId", evtFullId); }
                            }
                        );
                    }
                }
            }
        }
    }

    public static String readNbtPath(CompoundTag root, String path) {
        String[] parts = path.split("\\.");
        Tag current = root;
        for (String part : parts) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(part);
                if (current == null) return null;
            } else if (current instanceof ListTag list) {
                try {
                    int index = Integer.parseInt(part);
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        if (current instanceof NumericTag num) {
            return num.getAsNumber().toString();
        }
        return current.getAsString();
    }

    public static boolean writeNbtPath(CompoundTag root, String path, String value, String type, String mode) {
        if (path.isEmpty()) return false;
        String[] parts = path.split("\\.");
        Tag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(parts[i]);
                if (current == null) return false;
            } else {
                return false;
            }
        }

        String lastKey = parts[parts.length - 1];
        if (current instanceof CompoundTag compound) {
            Tag existing = compound.get(lastKey);
            double curVal = 0;
            if (existing instanceof NumericTag num) {
                curVal = num.getAsDouble();
            }
            double newVal;
            try { newVal = Double.parseDouble(value); } catch (NumberFormatException e) { return false; }

            switch (mode) {
                case "add": newVal = curVal + newVal; break;
                case "subtract": newVal = curVal - newVal; break;
                case "toggle": newVal = (curVal > 0) ? 0 : 1; break;
                default: break;
            }

            switch (type) {
                case "number":
                    compound.putDouble(lastKey, newVal);
                    return true;
                case "string":
                    compound.putString(lastKey, value);
                    return true;
                case "boolean":
                    compound.putBoolean(lastKey, "true".equalsIgnoreCase(value) || "1".equals(value));
                    return true;
            }
        }
        return false;
    }

    public static boolean evaluateCondition(String condFullId, String operator, String compareValue, LivingEntity entity) {
        String[] parts = condFullId.split(":", 2);
        if (parts.length < 2) return false;
        String modId = parts[0];
        String condId = parts[1];

        AdapterCondition cond = null;
        for (ModAdapterEntry entry : loadedAdapters) {
            if (entry.modId.equals(modId)) {
                for (AdapterCondition c : entry.customConditions) {
                    if (c.id.equals(condId)) {
                        cond = c;
                        break;
                    }
                }
                break;
            }
        }
        if (cond == null) return false;

        CompoundTag entityNbt = entity.saveWithoutId(new CompoundTag());
        String currentStr = readNbtPath(entityNbt, cond.nbtPath);
        if (currentStr == null) return false;

        if ("boolean".equals(cond.type)) {
            boolean curBool = "true".equalsIgnoreCase(currentStr) || "1".equals(currentStr);
            boolean compBool = "true".equalsIgnoreCase(compareValue) || "1".equals(compareValue);
            return "==".equals(operator) ? curBool == compBool : curBool != compBool;
        }

        if ("string".equals(cond.type)) {
            return "==".equals(operator) ? currentStr.equals(compareValue) : !currentStr.equals(compareValue);
        }

        try {
            double cur = Double.parseDouble(currentStr);
            double comp = Double.parseDouble(compareValue);
            switch (operator) {
                case ">=": return cur >= comp;
                case "<=": return cur <= comp;
                case "==": return Math.abs(cur - comp) < 0.001;
                case ">": return cur > comp;
                case "<": return cur < comp;
                case "!=": return Math.abs(cur - comp) >= 0.001;
                default: return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean executeEffect(String effFullId, String value, LivingEntity entity) {
        String[] parts = effFullId.split(":", 2);
        if (parts.length < 2) return false;
        String modId = parts[0];
        String effId = parts[1];

        AdapterEffect eff = null;
        for (ModAdapterEntry entry : loadedAdapters) {
            if (entry.modId.equals(modId)) {
                for (AdapterEffect e : entry.customEffects) {
                    if (e.id.equals(effId)) {
                        eff = e;
                        break;
                    }
                }
                break;
            }
        }
        if (eff == null) return false;

        CompoundTag entityNbt = entity.saveWithoutId(new CompoundTag());
        boolean result = writeNbtPath(entityNbt, eff.nbtPath, value, eff.type, eff.mode);
        if (result) {
            entity.load(entityNbt);
        }
        return result;
    }
}
