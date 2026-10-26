package god.bai.god.shen.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局 NBT 路径注册表
 */
public class NbtPathRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String REGISTRY_FILE = "config/god/watched_nbt.json";

    private static final Map<String, WatchedPath> paths = new LinkedHashMap<>();
    private static boolean loaded = false;

    public static class WatchedPath {
        public String nbtPath;
        public String displayName;
        public String registeredBy;
        public long registeredAt;

        public WatchedPath() {}

        public WatchedPath(String nbtPath, String displayName, String registeredBy) {
            this.nbtPath = nbtPath;
            this.displayName = displayName;
            this.registeredBy = registeredBy;
            this.registeredAt = System.currentTimeMillis();
        }
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        paths.clear();

        Path file = Paths.get(REGISTRY_FILE);
        if (!Files.exists(file)) {
            ensureDir();
            return;
        }

        try {
            String json = Files.readString(file);
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            if (arr != null) {
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    WatchedPath wp = new WatchedPath();
                    wp.nbtPath = obj.get("nbtPath").getAsString();
                    wp.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : wp.nbtPath;
                    wp.registeredBy = obj.has("registeredBy") ? obj.get("registeredBy").getAsString() : "unknown";
                    wp.registeredAt = obj.has("registeredAt") ? obj.get("registeredAt").getAsLong() : 0;
                    paths.put(wp.nbtPath, wp);
                }
            }
        } catch (Exception e) {
            System.err.println("[NbtPathRegistry] 加载失败: " + e.getMessage());
        }
    }

    public static void save() {
        ensureDir();
        JsonArray arr = new JsonArray();
        for (WatchedPath wp : paths.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("nbtPath", wp.nbtPath);
            obj.addProperty("displayName", wp.displayName);
            obj.addProperty("registeredBy", wp.registeredBy);
            obj.addProperty("registeredAt", wp.registeredAt);
            arr.add(obj);
        }
        try {
            Files.writeString(Paths.get(REGISTRY_FILE), GSON.toJson(arr));
        } catch (IOException e) {
            System.err.println("[NbtPathRegistry] 保存失败: " + e.getMessage());
        }
    }

    private static void ensureDir() {
        try { Files.createDirectories(Paths.get(REGISTRY_FILE).getParent()); } catch (IOException ignored) {}
    }

    public static boolean register(String nbtPath, String displayName, String playerName) {
        load();
        if (paths.containsKey(nbtPath)) {
            return false;
        }
        paths.put(nbtPath, new WatchedPath(nbtPath, displayName, playerName));
        save();

        ModAdapterManager.registerWatchedPath(nbtPath, displayName);

        System.out.println("[NbtPathRegistry] " + playerName + " 注册了 NBT 路径: " + nbtPath);
        return true;
    }

    public static boolean unregister(String nbtPath) {
        load();
        if (paths.remove(nbtPath) != null) {
            save();
            ModAdapterManager.unregisterWatchedPath(nbtPath);
            return true;
        }
        return false;
    }

    public static boolean isRegistered(String nbtPath) {
        load();
        return paths.containsKey(nbtPath);
    }

    public static Collection<WatchedPath> getAll() {
        load();
        return Collections.unmodifiableCollection(paths.values());
    }

    public static String[][] buildDropdownOptions() {
        load();
        List<String[]> result = new ArrayList<>();
        for (WatchedPath wp : paths.values()) {
            result.add(new String[]{wp.displayName, wp.nbtPath});
        }
        return result.toArray(new String[0][0]);
    }

    public static String getDisplayName(String nbtPath) {
        load();
        WatchedPath wp = paths.get(nbtPath);
        return wp != null ? wp.displayName : nbtPath;
    }

    public static int getCount() {
        load();
        return paths.size();
    }

    public static void clearAll() {
        paths.clear();
        save();
    }
}
