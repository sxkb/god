package god.bai.god.support;

/**
 * 强制传送标记（线程级）。
 *
 * <p>言出法随的传送效果在执行玩家传送前置位，供需要放行强制传送的逻辑判断。</p>
 */
public class ForceTeleportHelper {
    private static final ThreadLocal<Boolean> FORCE_TELEPORT = ThreadLocal.withInitial(() -> false);

    public static void setForceTeleport(boolean value) {
        FORCE_TELEPORT.set(value);
    }

    public static boolean isForceTeleporting() {
        return FORCE_TELEPORT.get();
    }
}
