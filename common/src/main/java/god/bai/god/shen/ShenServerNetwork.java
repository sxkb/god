package god.bai.god.shen;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 言出法随服务端网络处理器。
 * <p>Forge 移植版：统一走 {@link ShenNetwork} 的 SimpleChannel，本类仅保留旧 API 以兼容调用方。</p>
 */
public class ShenServerNetwork {

    public static void registerServer() {
        ShenNetwork.registerServer();
    }

    public static void sendOpenScreen(ServerPlayer player) {
        ShenNetwork.sendOpenScreen(player);
    }

    public static void sendOpenScreenLimited(ServerPlayer player) {
        ShenNetwork.sendOpenScreenLimited(player);
    }

    public static void sendSyncLaws(ServerPlayer player, List<ShenLaw> laws) {
        // 内部通过 ShenNetwork 发送
        // 这里保留签名以兼容；实际实现由 ShenNetwork 提供
        throw new UnsupportedOperationException("请使用 ShenNetwork.sendSyncLaws");
    }
}
