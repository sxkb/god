package god.bai.god.shen;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ShenSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("god-shen");
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        god.bai.god.shen.compat.smfs.SmfsAdapter.initialize();
        god.bai.god.shen.compat.smfs.SmfsIntegration.initialize();
        god.bai.god.shen.compat.ModAdapterManager.loadAllAdapters();

        god.bai.god.shen.compat.smfs.SmfsIntegration.registerEventListeners();

        LOGGER.info("言出法随系统已激活");
    }

    /**
     * 言出法随对全部玩家开放，不再依赖鬼域模组的权限体系。
     */
    public static boolean canUseShen(ServerPlayer player) {
        return player != null;
    }

    public static void loadPlayerLaws(ServerPlayer player) {
        if (!canUseShen(player)) return;
        List<ShenLaw> laws = ShenDataManager.loadLaws(player);
        for (ShenLaw law : laws) {
            ShenEventBus.registerLaw(law);
        }
    }

    public static void unloadPlayerLaws(ServerPlayer player) {
        List<ShenLaw> laws = ShenDataManager.loadLaws(player);
        for (ShenLaw law : laws) {
            ShenEventBus.unregisterLaw(law);
        }
    }
}
