package god.bai.god.shen.compat.smfs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 神秘复苏深度集成类
 */
public class SmfsIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger("god-smfs-integration");

    // ===== 反射缓存 =====
    private static Class<?> playerEventsClass;
    private static Method getCachedDataMethod;
    private static Method setSpiritAttributesMethod;
    private static Method getSpiritAttributeMethod;
    private static Method getCurrentSpiritMethod;
    private static Method setCurrentSpiritMethod;
    private static Method getMaxSpiritMethod;
    private static Method setMaxSpiritMethod;
    private static Method getGhostSlotItemMethod;
    private static Method isGhostSlotOccupiedMethod;
    private static Method getGhostTypeInSlotMethod;
    private static Method getGhostSlotLevelMethod;
    private static Method getUnlockedGhostSlotMethod;
    private static Method setGhostSlotDataMethod;
    private static Method calculateSpiritDamageMethod;
    private static Method handleSpiritDamageMethod;
    private static Method countOccupiedGhostSlotsMethod;

    private static Class<?> mainGhostManagerClass;
    private static Method getMainGhostSlotMethod;
    private static Method setMainGhostSlotMethod;
    private static Method hasMainGhostMethod;
    private static Method getMainGhostNameMethod;
    private static Method getMainGhostLevelMethod;

    private static Class<?> ghostSkillManagerClass;

    private static boolean initialized = false;

    public static final int ORIGINAL_SLOT_COUNT = 10;
    private static int defaultExtraSlotCount = 1;
    public static int getTotalSlotCount() { return ORIGINAL_SLOT_COUNT + defaultExtraSlotCount; }

    public static int getPlayerTotalSlotCount(Player player) {
        return ORIGINAL_SLOT_COUNT + getPlayerExtraSlotCount(player);
    }

    public static int getPlayerExtraSlotCount(Player player) {
        if (!isSmfsLoaded() || player == null) return 0;
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains("guiyu_extra_slot_count")) {
            return data.getInt("guiyu_extra_slot_count");
        }
        return 0;
    }

    public static void addExtraSlotsToPlayer(Player player, int count) {
        if (!isSmfsLoaded() || player == null) return;
        int current = getPlayerExtraSlotCount(player);
        int newCount = current + Math.max(1, count);
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putInt("guiyu_extra_slot_count", newCount);
            savePlayerData(player, data);
            LOGGER.info("玩家 {} 槽位扩容: {} → {} (总槽位: {})",
                player.getName().getString(), current, newCount, ORIGINAL_SLOT_COUNT + newCount);
        }
    }

    public static void initialize() {
        if (initialized) return;
        try {
            playerEventsClass = Class.forName("com.xie.smfs.common.events.PlayerEvents");
            mainGhostManagerClass = Class.forName("com.xie.smfs.manager.MainGhostManager");
            ghostSkillManagerClass = Class.forName("com.xie.smfs.manager.GhostSkillManager");

            getCachedDataMethod = findMethod(playerEventsClass, "getCachedData", Player.class);
            setSpiritAttributesMethod = findMethod(playerEventsClass, "setSpiritAttributes", Player.class, CompoundTag.class);
            getCurrentSpiritMethod = findMethod(playerEventsClass, "getCurrentSpirit", Player.class);
            setCurrentSpiritMethod = findMethod(playerEventsClass, "setCurrentSpirit", Player.class, float.class);
            getMaxSpiritMethod = findMethod(playerEventsClass, "getMaxSpirit", Player.class);
            setMaxSpiritMethod = findMethod(playerEventsClass, "setMaxSpirit", Player.class, float.class);
            getGhostSlotItemMethod = findMethod(playerEventsClass, "getGhostSlotItem", Player.class, int.class);
            isGhostSlotOccupiedMethod = findMethod(playerEventsClass, "isGhostSlotOccupied", Player.class, int.class);
            getGhostTypeInSlotMethod = findMethod(playerEventsClass, "getGhostTypeInSlot", Player.class, int.class);
            getGhostSlotLevelMethod = findMethod(playerEventsClass, "getGhostSlotLevel", Player.class, int.class);
            getUnlockedGhostSlotMethod = findMethod(playerEventsClass, "getUnlockedGhostSlot", Player.class);
            setGhostSlotDataMethod = findMethod(playerEventsClass, "setGhostSlotData", Player.class, int.class, ItemStack.class);
            countOccupiedGhostSlotsMethod = findMethod(playerEventsClass, "countOccupiedGhostSlots", Player.class);

            getMainGhostSlotMethod = findMethod(mainGhostManagerClass, "getMainGhostSlot", Player.class);
            setMainGhostSlotMethod = findMethod(mainGhostManagerClass, "setMainGhostSlot", Player.class, int.class);
            hasMainGhostMethod = findMethod(mainGhostManagerClass, "hasMainGhost", Player.class);
            getMainGhostNameMethod = findMethod(mainGhostManagerClass, "getMainGhostName", Player.class);
            getMainGhostLevelMethod = findMethod(mainGhostManagerClass, "getMainGhostLevel", Player.class);

            initialized = true;
            LOGGER.info("神秘复苏深度集成已初始化 (槽位扩容: {} → {})", ORIGINAL_SLOT_COUNT, getTotalSlotCount());
        } catch (Exception e) {
            LOGGER.warn("神秘复苏深度集成初始化失败，SMFS 可能未安装: {}", e.getMessage());
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            Method m = clazz.getMethod(name, paramTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            LOGGER.warn("方法未找到: {}.{} ({})", clazz.getSimpleName(), name, e.getMessage());
            return null;
        }
    }

    public static boolean isSmfsLoaded() {
        return initialized && playerEventsClass != null;
    }

    public static ItemStack getGhostSlotItem(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return ItemStack.EMPTY;
        try {
            return (ItemStack) getGhostSlotItemMethod.invoke(null, player, slotIndex);
        } catch (Exception e) {
            LOGGER.error("getGhostSlotItem 失败", e);
            return ItemStack.EMPTY;
        }
    }

    public static boolean isSlotOccupied(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return false;
        try {
            return (boolean) isGhostSlotOccupiedMethod.invoke(null, player, slotIndex);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getGhostTypeInSlot(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return null;
        try {
            return (String) getGhostTypeInSlotMethod.invoke(null, player, slotIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getSlotLevel(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return 0;
        try {
            return (int) getGhostSlotLevelMethod.invoke(null, player, slotIndex);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getFirstEmptySlot(Player player) {
        if (!isSmfsLoaded() || player == null) return -1;
        try {
            return (int) getUnlockedGhostSlotMethod.invoke(null, player);
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean setSlotData(Player player, int slotIndex, ItemStack item) {
        if (!isSmfsLoaded() || player == null) return false;
        try {
            setGhostSlotDataMethod.invoke(null, player, slotIndex, item);
            return true;
        } catch (Exception e) {
            LOGGER.error("setSlotData 失败", e);
            return false;
        }
    }

    public static int countOccupiedSlots(Player player) {
        if (!isSmfsLoaded() || player == null) return 0;
        try {
            return (int) countOccupiedGhostSlotsMethod.invoke(null, player);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean hasGhostType(Player player, String ghostTypeId) {
        if (!isSmfsLoaded() || player == null) return false;
        int total = getPlayerTotalSlotCount(player);
        for (int i = 0; i < total; i++) {
            String type = getGhostTypeInSlot(player, i);
            if (type != null && type.equals(ghostTypeId)) return true;
        }
        return false;
    }

    public static boolean isValidSlotIndex(int slotIndex) {
        return slotIndex >= 0 && slotIndex < ORIGINAL_SLOT_COUNT;
    }

    public static void addExtraSlot(Player player) {
        addExtraSlotsToPlayer(player, 1);
    }

    public static CompoundTag getPlayerData(Player player) {
        if (!isSmfsLoaded() || player == null) return new CompoundTag();
        try {
            return (CompoundTag) getCachedDataMethod.invoke(null, player);
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    public static void savePlayerData(Player player, CompoundTag data) {
        if (!isSmfsLoaded() || player == null) return;
        try {
            setSpiritAttributesMethod.invoke(null, player, data);
        } catch (Exception e) {
            LOGGER.error("savePlayerData 失败", e);
        }
    }

    public static void setSpiritResistance(Player player, float value) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putDouble("spiritResistance", value);
            savePlayerData(player, data);
        }
    }

    public static float getSpiritResistance(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains("spiritResistance")) {
            return (float) data.getDouble("spiritResistance");
        }
        return 0f;
    }

    public static void setSpiritDamage(Player player, float value) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putDouble("spiritDamage", value);
            savePlayerData(player, data);
        }
    }

    public static float getSpiritDamage(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains("spiritDamage")) {
            return (float) data.getDouble("spiritDamage");
        }
        return 0f;
    }

    public static void setCurrentSpirit(Player player, float value) {
        if (!isSmfsLoaded() || player == null) return;
        try {
            setCurrentSpiritMethod.invoke(null, player, value);
        } catch (Exception e) {
            LOGGER.error("setCurrentSpirit 失败", e);
        }
    }

    public static float getCurrentSpirit(Player player) {
        if (!isSmfsLoaded() || player == null) return 0f;
        try {
            return (float) getCurrentSpiritMethod.invoke(null, player);
        } catch (Exception e) {
            return 0f;
        }
    }

    public static void setMaxSpirit(Player player, float value) {
        if (!isSmfsLoaded() || player == null) return;
        try {
            setMaxSpiritMethod.invoke(null, player, value);
        } catch (Exception e) {
            LOGGER.error("setMaxSpirit 失败", e);
        }
    }

    public static float getMaxSpirit(Player player) {
        if (!isSmfsLoaded() || player == null) return 0f;
        try {
            return (float) getMaxSpiritMethod.invoke(null, player);
        } catch (Exception e) {
            return 0f;
        }
    }

    public static void setSanity(Player player, float value) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putDouble("sanity", Math.max(0, Math.min(100, value)));
            savePlayerData(player, data);
        }
    }

    public static float getSanity(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains("sanity")) {
            return (float) data.getDouble("sanity");
        }
        return 100f;
    }

    public static void setRevivalFactor(Player player, double value) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putDouble("revivalFactor", value);
            savePlayerData(player, data);
        }
    }

    public static double getRevivalFactor(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains("revivalFactor")) {
            return data.getDouble("revivalFactor");
        }
        return 0.0;
    }

    public static void setNbtAttribute(Player player, String key, double value) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.putDouble(key, value);
            savePlayerData(player, data);
        }
    }

    public static double getNbtAttribute(Player player, String key, double defaultValue) {
        CompoundTag data = getPlayerData(player);
        if (data != null && data.contains(key)) {
            return data.getDouble(key);
        }
        return defaultValue;
    }

    private static final String DOMAIN_NBT_KEY = "guiyu_ghost_domain";

    private static CompoundTag getDomainData(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data == null) return new CompoundTag();
        if (!data.contains(DOMAIN_NBT_KEY)) {
            data.put(DOMAIN_NBT_KEY, new CompoundTag());
            savePlayerData(player, data);
        }
        return data.getCompound(DOMAIN_NBT_KEY);
    }

    private static void saveDomainData(Player player, CompoundTag domainData) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.put(DOMAIN_NBT_KEY, domainData);
            savePlayerData(player, data);
        }
    }

    public static boolean isGhostDomainActive(Player player) {
        CompoundTag domain = getDomainData(player);
        return domain.getBoolean("active");
    }

    public static void setGhostDomainActive(Player player, boolean active) {
        CompoundTag domain = getDomainData(player);
        domain.putBoolean("active", active);
        saveDomainData(player, domain);
    }

    public static int getGhostDomainColor(Player player) {
        CompoundTag domain = getDomainData(player);
        return domain.getInt("color");
    }

    public static void setGhostDomainColor(Player player, int color) {
        CompoundTag domain = getDomainData(player);
        domain.putInt("color", Math.max(0, Math.min(7, color)));
        saveDomainData(player, domain);
    }

    public static String getGhostDomainColorName(int color) {
        switch (color) {
            case 0: return "红色";
            case 1: return "绿色";
            case 2: return "蓝色";
            case 3: return "灰色";
            case 4: return "金色";
            case 5: return "紫色";
            case 6: return "黑色";
            case 7: return "青色";
            default: return "未知";
        }
    }

    public static int getGhostDomainLevel(Player player) {
        CompoundTag domain = getDomainData(player);
        return domain.getInt("level");
    }

    public static void setGhostDomainLevel(Player player, int level) {
        CompoundTag domain = getDomainData(player);
        domain.putInt("level", Math.max(1, Math.min(10, level)));
        saveDomainData(player, domain);
    }

    private static final String EYE_NBT_KEY = "guiyu_ghost_eye";

    private static CompoundTag getEyeData(Player player) {
        CompoundTag data = getPlayerData(player);
        if (data == null) return new CompoundTag();
        if (!data.contains(EYE_NBT_KEY)) {
            data.put(EYE_NBT_KEY, new CompoundTag());
            savePlayerData(player, data);
        }
        return data.getCompound(EYE_NBT_KEY);
    }

    private static void saveEyeData(Player player, CompoundTag eyeData) {
        CompoundTag data = getPlayerData(player);
        if (data != null) {
            data.put(EYE_NBT_KEY, eyeData);
            savePlayerData(player, data);
        }
    }

    public static boolean isGhostEyeActive(Player player) {
        CompoundTag eye = getEyeData(player);
        return eye.getBoolean("active");
    }

    public static void setGhostEyeActive(Player player, boolean active) {
        CompoundTag eye = getEyeData(player);
        eye.putBoolean("active", active);
        saveEyeData(player, eye);
    }

    public static int getGhostEyeLevel(Player player) {
        CompoundTag eye = getEyeData(player);
        return eye.getInt("level");
    }

    public static void setGhostEyeLevel(Player player, int level) {
        CompoundTag eye = getEyeData(player);
        eye.putInt("level", Math.max(1, Math.min(10, level)));
        saveEyeData(player, eye);
    }

    public static void setSlotLocked(Player player, int slotIndex, boolean locked) {
        if (!isSmfsLoaded() || player == null) return;
        CompoundTag data = getPlayerData(player);
        if (data == null) return;
        if (!data.contains("guiyu_slot_flags")) {
            data.put("guiyu_slot_flags", new CompoundTag());
        }
        CompoundTag flags = data.getCompound("guiyu_slot_flags");
        String key = "slot_" + slotIndex + "_locked";
        flags.putBoolean(key, locked);
        savePlayerData(player, data);
    }

    public static boolean isSlotLocked(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return false;
        CompoundTag data = getPlayerData(player);
        if (data == null || !data.contains("guiyu_slot_flags")) return false;
        CompoundTag flags = data.getCompound("guiyu_slot_flags");
        return flags.getBoolean("slot_" + slotIndex + "_locked");
    }

    public static boolean clearSlot(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return false;
        return setSlotData(player, slotIndex, ItemStack.EMPTY);
    }

    public static boolean swapSlots(Player player, int slotA, int slotB) {
        if (!isSmfsLoaded() || player == null) return false;
        if (slotA < 0 || slotA >= getTotalSlotCount() || slotB < 0 || slotB >= getTotalSlotCount()) return false;
        ItemStack itemA = getGhostSlotItem(player, slotA);
        ItemStack itemB = getGhostSlotItem(player, slotB);
        if (setSlotData(player, slotA, itemB) && setSlotData(player, slotB, itemA)) {
            return true;
        }
        return false;
    }

    public static boolean setGhostInSlot(Player player, int slotIndex, String ghostItemId) {
        if (!isSmfsLoaded() || player == null || ghostItemId == null || ghostItemId.isEmpty()) return false;
        if (slotIndex < 0 || slotIndex >= getPlayerTotalSlotCount(player)) return false;
        try {
            ResourceLocation itemId = ResourceLocation.tryParse(ghostItemId);
            if (itemId == null) return false;
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) return false;
            ItemStack ghostStack = new ItemStack(item);
            return setSlotData(player, slotIndex, ghostStack);
        } catch (Exception e) {
            LOGGER.error("setGhostInSlot 失败: {}", e.getMessage());
            return false;
        }
    }

    public static double getSlotLevelPercent(Player player, int slotIndex) {
        int level = getSlotLevel(player, slotIndex);
        if (level <= 0) return 0;
        return Math.min(1.0, level / 100.0);
    }

    public static int getMainGhostSlot(Player player) {
        if (!isSmfsLoaded() || player == null) return 0;
        try {
            return (int) getMainGhostSlotMethod.invoke(null, player);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setMainGhostSlot(Player player, int slotIndex) {
        if (!isSmfsLoaded() || player == null) return;
        try {
            setMainGhostSlotMethod.invoke(null, player, slotIndex);
        } catch (Exception e) {
            LOGGER.error("setMainGhostSlot 失败", e);
        }
    }

    public static boolean hasMainGhost(Player player) {
        if (!isSmfsLoaded() || player == null) return false;
        try {
            return (boolean) hasMainGhostMethod.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getMainGhostName(Player player) {
        if (!isSmfsLoaded() || player == null) return "无";
        try {
            net.minecraft.network.chat.Component text = (net.minecraft.network.chat.Component) getMainGhostNameMethod.invoke(null, player);
            return text.getString();
        } catch (Exception e) {
            return "无";
        }
    }

    public static int getMainGhostLevel(Player player) {
        if (!isSmfsLoaded() || player == null) return 0;
        try {
            return (int) getMainGhostLevelMethod.invoke(null, player);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setDefaultExtraSlotCount(int count) {
        defaultExtraSlotCount = Math.max(0, count);
        LOGGER.info("默认槽位扩容配置已更新: {} → {}", ORIGINAL_SLOT_COUNT, getTotalSlotCount());
    }

    public static int getExtraSlotCount() {
        return defaultExtraSlotCount;
    }

    public static void cycleMainGhost(Player player) {
        if (!isSmfsLoaded() || player == null) return;
        int current = getMainGhostSlot(player);
        int total = getPlayerTotalSlotCount(player);
        for (int i = 1; i <= total; i++) {
            int next = (current + i) % total;
            if (isSlotOccupied(player, next)) {
                setMainGhostSlot(player, next);
                return;
            }
        }
    }

    public static boolean isGhostPossessed(Player player) {
        return countOccupiedSlots(player) > 0;
    }

    public static double getGhostPossessionPercent(Player player) {
        int total = getTotalSlotCount();
        if (total <= 0) return 0;
        return (double) countOccupiedSlots(player) / total;
    }

    public static List<String> listAllGhostTypes(Player player) {
        List<String> types = new ArrayList<>();
        int total = getPlayerTotalSlotCount(player);
        for (int i = 0; i < total; i++) {
            String type = getGhostTypeInSlot(player, i);
            if (type != null && !type.isEmpty()) {
                types.add(type);
            }
        }
        return types;
    }

    public static List<Map<String, Object>> listAllSlots(Player player) {
        List<Map<String, Object>> slots = new ArrayList<>();
        int total = getPlayerTotalSlotCount(player);
        for (int i = 0; i < total; i++) {
            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("index", i);
            slotInfo.put("occupied", isSlotOccupied(player, i));
            if (isSlotOccupied(player, i)) {
                slotInfo.put("ghostType", getGhostTypeInSlot(player, i));
                slotInfo.put("level", getSlotLevel(player, i));
            }
            slots.add(slotInfo);
        }
        return slots;
    }

    // ---- 状态缓存 ----

    /** 玩家槽位占用缓存 (UUID → slotIndex → occupied) */
    private static final Map<UUID, boolean[]> cachedSlotOccupied = new ConcurrentHashMap<>();
    /** 玩家槽位鬼类型缓存 (UUID → slotIndex → ghostType) */
    private static final Map<UUID, String[]> cachedSlotGhostTypes = new ConcurrentHashMap<>();
    /** 玩家灵异属性缓存 (UUID → attrKey → value) */
    private static final Map<UUID, Map<String, Double>> cachedAttributes = new ConcurrentHashMap<>();
    /** 玩家主鬼槽位缓存 (UUID → slotIndex) */
    private static final Map<UUID, Integer> cachedMainGhostSlot = new ConcurrentHashMap<>();
    private static boolean eventListenersRegistered = false;

    private static final String[] TRACKED_ATTR_KEYS = {
        "spiritResistance", "spiritDamage", "currentSpirit", "maxSpirit",
        "sanity", "revivalFactor"
    };

    public static void registerEventListeners() {
        if (eventListenersRegistered) return;
        if (!isSmfsLoaded()) return;
        eventListenersRegistered = true;
        LOGGER.info("神秘复苏专属事件监听器已注册");
    }

    /** 由各加载器的服务端 tick 事件调用 */
    public static void tickServer(MinecraftServer server) {
        if (!eventListenersRegistered || server == null) return;
        if (server.getTickCount() % 10 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                detectSlotChanges(player);
                detectAttributeChanges(player);
                detectMainGhostChanges(player);
            } catch (Exception ignored) {
            }
        }
    }

    private static void detectSlotChanges(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int total = getPlayerTotalSlotCount(player);
        boolean[] currentOccupied = new boolean[total];
        String[] currentTypes = new String[total];

        for (int i = 0; i < total; i++) {
            currentOccupied[i] = isSlotOccupied(player, i);
            currentTypes[i] = getGhostTypeInSlot(player, i);
        }

        boolean[] prevOccupied = cachedSlotOccupied.get(uuid);
        String[] prevTypes = cachedSlotGhostTypes.get(uuid);

        if (prevOccupied != null && prevTypes != null) {
            for (int i = 0; i < total; i++) {
                boolean changed = (prevOccupied[i] != currentOccupied[i]);
                boolean typeChanged = !changed && prevOccupied[i] && currentOccupied[i]
                    && !Objects.equals(prevTypes[i], currentTypes[i]);

                if (changed || typeChanged) {
                    String oldType = prevOccupied[i] ? prevTypes[i] : "empty";
                    String newType = currentOccupied[i] ? currentTypes[i] : "empty";
                    fireSlotChangeEvent(player, i, oldType, newType);
                }
            }
        }

        cachedSlotOccupied.put(uuid, currentOccupied);
        cachedSlotGhostTypes.put(uuid, currentTypes);
    }

    private static void detectAttributeChanges(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<String, Double> currentAttrs = new HashMap<>();

        for (String key : TRACKED_ATTR_KEYS) {
            currentAttrs.put(key, getAttrValue(player, key));
        }

        Map<String, Double> prevAttrs = cachedAttributes.get(uuid);
        if (prevAttrs != null) {
            for (String key : TRACKED_ATTR_KEYS) {
                double oldVal = prevAttrs.getOrDefault(key, 0.0);
                double newVal = currentAttrs.getOrDefault(key, 0.0);
                if (Math.abs(oldVal - newVal) > 0.001) {
                    fireSpiritAttrChangeEvent(player, key, oldVal, newVal);
                }
            }
        }

        cachedAttributes.put(uuid, currentAttrs);
    }

    private static void detectMainGhostChanges(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int currentSlot = getMainGhostSlot(player);
        Integer prevSlot = cachedMainGhostSlot.get(uuid);

        if (prevSlot != null && prevSlot != currentSlot) {
            fireMainGhostSwitchEvent(player, prevSlot, currentSlot);
        }

        cachedMainGhostSlot.put(uuid, currentSlot);
    }

    private static double getAttrValue(Player player, String key) {
        switch (key) {
            case "spiritResistance": return getSpiritResistance(player);
            case "spiritDamage": return getSpiritDamage(player);
            case "currentSpirit": return getCurrentSpirit(player);
            case "maxSpirit": return getMaxSpirit(player);
            case "sanity": return getSanity(player);
            case "revivalFactor": return getRevivalFactor(player);
            default: return getNbtAttribute(player, key, 0);
        }
    }

    public static void fireSlotChangeEvent(Player player, int slotIndex, String oldGhostType, String newGhostType) {
        if (player == null || player.level().isClientSide()) return;
        try {
            god.bai.god.shen.ShenEvent event = new god.bai.god.shen.ShenEvent(
                god.bai.god.shen.ShenEventType.SMFS_SLOT_CHANGE,
                player, player.blockPosition(), player.level()
            );
            event.params.put("slotIndex", String.valueOf(slotIndex));
            event.params.put("oldGhostType", oldGhostType != null ? oldGhostType : "empty");
            event.params.put("newGhostType", newGhostType != null ? newGhostType : "empty");
            god.bai.god.shen.ShenEventBus.fire(event);
        } catch (Exception e) {
            LOGGER.error("触发槽位变化事件失败", e);
        }
    }

    public static void fireSpiritAttrChangeEvent(Player player, String attrKey, double oldValue, double newValue) {
        if (player == null || player.level().isClientSide()) return;
        try {
            god.bai.god.shen.ShenEvent event = new god.bai.god.shen.ShenEvent(
                god.bai.god.shen.ShenEventType.SMFS_SPIRIT_ATTR_CHANGE,
                player, player.blockPosition(), player.level()
            );
            event.params.put("attrKey", attrKey);
            event.params.put("oldValue", String.valueOf(oldValue));
            event.params.put("newValue", String.valueOf(newValue));
            god.bai.god.shen.ShenEventBus.fire(event);
        } catch (Exception e) {
            LOGGER.error("触发灵异属性变化事件失败", e);
        }
    }

    public static void fireMainGhostSwitchEvent(Player player, int oldSlot, int newSlot) {
        if (player == null || player.level().isClientSide()) return;
        try {
            god.bai.god.shen.ShenEvent event = new god.bai.god.shen.ShenEvent(
                god.bai.god.shen.ShenEventType.SMFS_MAIN_GHOST_SWITCH,
                player, player.blockPosition(), player.level()
            );
            event.params.put("oldSlot", String.valueOf(oldSlot));
            event.params.put("newSlot", String.valueOf(newSlot));
            god.bai.god.shen.ShenEventBus.fire(event);
        } catch (Exception e) {
            LOGGER.error("触发主鬼切换事件失败", e);
        }
    }

    public static void fireGhostSkillUsedEvent(Player player, String skillName, LivingEntity target) {
        if (player == null || player.level().isClientSide()) return;
        try {
            god.bai.god.shen.ShenEvent event = new god.bai.god.shen.ShenEvent(
                god.bai.god.shen.ShenEventType.SMFS_GHOST_SKILL_USED,
                player, target, player.blockPosition(), player.level()
            );
            event.params.put("skillName", skillName);
            event.params.put("target", target != null ? target.getName().getString() : "none");
            god.bai.god.shen.ShenEventBus.fire(event);
        } catch (Exception e) {
            LOGGER.error("触发鬼技能使用事件失败", e);
        }
    }

    public static void setMainGhostSlotWithEvent(Player player, int slotIndex) {
        int oldSlot = getMainGhostSlot(player);
        setMainGhostSlot(player, slotIndex);
        fireMainGhostSwitchEvent(player, oldSlot, slotIndex);
    }

    public static void cycleMainGhostWithEvent(Player player) {
        int oldSlot = getMainGhostSlot(player);
        cycleMainGhost(player);
        int newSlot = getMainGhostSlot(player);
        if (oldSlot != newSlot) {
            fireMainGhostSwitchEvent(player, oldSlot, newSlot);
        }
    }

    public static void setSpiritResistanceWithEvent(Player player, float value) {
        float oldValue = getSpiritResistance(player);
        setSpiritResistance(player, value);
        fireSpiritAttrChangeEvent(player, "spiritResistance", oldValue, value);
    }

    public static void setSpiritDamageWithEvent(Player player, float value) {
        float oldValue = getSpiritDamage(player);
        setSpiritDamage(player, value);
        fireSpiritAttrChangeEvent(player, "spiritDamage", oldValue, value);
    }

    public static void setCurrentSpiritWithEvent(Player player, float value) {
        float oldValue = getCurrentSpirit(player);
        setCurrentSpirit(player, value);
        fireSpiritAttrChangeEvent(player, "currentSpirit", oldValue, value);
    }

    public static void setMaxSpiritWithEvent(Player player, float value) {
        float oldValue = getMaxSpirit(player);
        setMaxSpirit(player, value);
        fireSpiritAttrChangeEvent(player, "maxSpirit", oldValue, value);
    }

    public static void setSanityWithEvent(Player player, float value) {
        float oldValue = getSanity(player);
        setSanity(player, value);
        fireSpiritAttrChangeEvent(player, "sanity", oldValue, value);
    }

    public static void setRevivalFactorWithEvent(Player player, double value) {
        double oldValue = getRevivalFactor(player);
        setRevivalFactor(player, value);
        fireSpiritAttrChangeEvent(player, "revivalFactor", oldValue, value);
    }
}
