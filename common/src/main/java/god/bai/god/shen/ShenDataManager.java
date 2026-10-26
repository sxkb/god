package god.bai.god.shen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShenDataManager {
    private static final String STATE_KEY = "god_shen_state";

    public static List<ShenLaw> loadLaws(ServerPlayer player) {
        ShenState state = ShenState.get(player.server);
        return state.getLaws(player.getUUID());
    }

    public static void saveLaws(ServerPlayer player, List<ShenLaw> laws) {
        ShenState state = ShenState.get(player.server);
        state.setLaws(player.getUUID(), laws);
        state.setDirty();
    }

    public static void saveLaw(ServerPlayer player, ShenLaw law) {
        List<ShenLaw> laws = loadLaws(player);

        boolean found = false;
        for (int i = 0; i < laws.size(); i++) {
            if (laws.get(i).name.equals(law.name)) {
                laws.set(i, law);
                found = true;
                break;
            }
        }
        if (!found) {
            laws.add(law);
        }

        saveLaws(player, laws);
    }

    public static void deleteLaw(ServerPlayer player, String lawName) {
        List<ShenLaw> laws = loadLaws(player);
        laws.removeIf(law -> law.name.equals(lawName));
        saveLaws(player, laws);
    }

    public static class ShenState extends SavedData {
        private final Map<UUID, List<ShenLaw>> playerLaws = new HashMap<>();

        public static ShenState get(MinecraftServer server) {
            DimensionDataStorage storage = server.overworld().getDataStorage();
            return storage.computeIfAbsent(ShenState::fromNbt, ShenState::new, STATE_KEY);
        }

        public List<ShenLaw> getLaws(UUID playerUuid) {
            return playerLaws.getOrDefault(playerUuid, new ArrayList<>());
        }

        public void setLaws(UUID playerUuid, List<ShenLaw> laws) {
            playerLaws.put(playerUuid, new ArrayList<>(laws));
        }

        public void removeLaws(UUID playerUuid) {
            playerLaws.remove(playerUuid);
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag playersNbt = new CompoundTag();
            for (Map.Entry<UUID, List<ShenLaw>> entry : playerLaws.entrySet()) {
                ListTag lawList = new ListTag();
                for (ShenLaw law : entry.getValue()) {
                    lawList.add(law.toNbt());
                }
                playersNbt.put(entry.getKey().toString(), lawList);
            }
            nbt.put("playerLaws", playersNbt);
            return nbt;
        }

        public static ShenState fromNbt(CompoundTag nbt) {
            ShenState state = new ShenState();
            if (nbt.contains("playerLaws")) {
                CompoundTag playersNbt = nbt.getCompound("playerLaws");
                for (String key : playersNbt.getAllKeys()) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ListTag lawList = playersNbt.getList(key, Tag.TAG_COMPOUND);
                        List<ShenLaw> laws = new ArrayList<>();
                        for (int i = 0; i < lawList.size(); i++) {
                            try {
                                laws.add(ShenLaw.fromNbt(lawList.getCompound(i)));
                            } catch (Exception ignored) {
                            }
                        }
                        state.playerLaws.put(uuid, laws);
                    } catch (Exception ignored) {
                    }
                }
            }
            return state;
        }
    }
}
