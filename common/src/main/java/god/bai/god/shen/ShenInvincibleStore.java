package god.bai.god.shen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无敌状态存档存储：绑定主世界存档（data/god_invincible.dat），
 * 跨维度、跨服务器重启持久保留。只有规则手动开启/关闭时才会变化。
 */
public class ShenInvincibleStore extends SavedData {
    private static final String DATA_NAME = "god_invincible";

    private final Set<UUID> invincible = ConcurrentHashMap.newKeySet();

    /**
     * 获取全局无敌存档（挂载在主世界，任意维度传入均可访问同一份数据）
     */
    public static ShenInvincibleStore get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage()
            .computeIfAbsent(ShenInvincibleStore::load, ShenInvincibleStore::new, DATA_NAME);
    }

    public static ShenInvincibleStore load(CompoundTag tag) {
        ShenInvincibleStore store = new ShenInvincibleStore();
        if (tag.contains("uuids")) {
            ListTag list = tag.getList("uuids", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    store.invincible.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                    // 跳过损坏条目
                }
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID uuid : invincible) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("uuids", list);
        return tag;
    }

    public boolean contains(UUID uuid) {
        return invincible.contains(uuid);
    }

    public void add(UUID uuid) {
        invincible.add(uuid);
        setDirty();
    }

    public void remove(UUID uuid) {
        invincible.remove(uuid);
        setDirty();
    }
}
