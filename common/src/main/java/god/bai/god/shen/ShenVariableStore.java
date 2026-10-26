package god.bai.god.shen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShenVariableStore extends SavedData {
    private static final String KEY = "god_shen_variables";
    private final Map<String, String> globalVars = new HashMap<>();
    private final Map<String, Map<String, String>> playerVars = new HashMap<>();

    public static ShenVariableStore get(MinecraftServer server) {
        DimensionDataStorage manager = server.overworld().getDataStorage();
        return manager.computeIfAbsent(ShenVariableStore::fromNbt, ShenVariableStore::new, KEY);
    }

    public void setGlobal(String key, String value) {
        globalVars.put(key, value);
        setDirty();
    }

    public String getGlobal(String key) {
        return globalVars.getOrDefault(key, "0");
    }

    public void setPlayer(UUID playerId, String key, String value) {
        playerVars.computeIfAbsent(playerId.toString(), k -> new HashMap<>()).put(key, value);
        setDirty();
    }

    public String getPlayer(UUID playerId, String key) {
        Map<String, String> vars = playerVars.get(playerId.toString());
        return vars != null ? vars.getOrDefault(key, "0") : "0";
    }

    public void modifyGlobal(String key, double amount) {
        double current = Double.parseDouble(getGlobal(key));
        setGlobal(key, String.valueOf(current + amount));
    }

    public void modifyPlayer(UUID playerId, String key, double amount) {
        double current = Double.parseDouble(getPlayer(playerId, key));
        setPlayer(playerId, key, String.valueOf(current + amount));
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag global = new CompoundTag();
        for (Map.Entry<String, String> e : globalVars.entrySet()) {
            global.putString(e.getKey(), e.getValue());
        }
        nbt.put("global", global);

        CompoundTag players = new CompoundTag();
        for (Map.Entry<String, Map<String, String>> pe : playerVars.entrySet()) {
            CompoundTag pvars = new CompoundTag();
            for (Map.Entry<String, String> e : pe.getValue().entrySet()) {
                pvars.putString(e.getKey(), e.getValue());
            }
            players.put(pe.getKey(), pvars);
        }
        nbt.put("players", players);
        return nbt;
    }

    public static ShenVariableStore fromNbt(CompoundTag nbt) {
        ShenVariableStore store = new ShenVariableStore();
        if (nbt.contains("global")) {
            CompoundTag global = nbt.getCompound("global");
            for (String key : global.getAllKeys()) {
                store.globalVars.put(key, global.getString(key));
            }
        }
        if (nbt.contains("players")) {
            CompoundTag players = nbt.getCompound("players");
            for (String playerId : players.getAllKeys()) {
                CompoundTag pvars = players.getCompound(playerId);
                Map<String, String> vars = new HashMap<>();
                for (String key : pvars.getAllKeys()) {
                    vars.put(key, pvars.getString(key));
                }
                store.playerVars.put(playerId, vars);
            }
        }
        return store;
    }
}
