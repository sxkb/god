package god.bai.god.shen;

import god.bai.god.platform.Platforms;
import god.bai.god.shen.ui.ShenClientPacketHandler;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 言出法随网络层（与加载器无关）。
 *
 * <p>5 种消息统一为「类型标记 + JSON 字符串」的单一载荷，由各加载器的传输层负责收发：
 * open_screen / request_laws / sync_laws / save_law / delete_law。</p>
 */
public class ShenNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("god-shen-network");
    public static final Gson GSON = new Gson();

    public static final String CHANNEL_PATH = "main";

    public static final String OPEN_SCREEN = "open_shen_screen";
    public static final String REQUEST_LAWS_C2S = "request_shen_laws";
    public static final String SYNC_LAWS_S2C = "sync_shen_laws";
    public static final String SAVE_LAW_C2S = "save_shen_law";
    public static final String DELETE_LAW_C2S = "delete_shen_law";

    // ============ 服务端处理 C2S ============
    public static void handleServerMessage(ServerPlayer player, String type, String payload) {
        if (player == null || type == null) return;
        switch (type) {
            case REQUEST_LAWS_C2S -> {
                List<ShenLaw> laws = ShenDataManager.loadLaws(player);
                sendSyncLaws(player, laws);
            }
            case SAVE_LAW_C2S -> {
                ShenLaw law = GSON.fromJson(payload, ShenLaw.class);
                if (law == null) return;
                player.server.execute(() -> {
                    ShenDataManager.saveLaw(player, law);
                    ShenEventBus.registerLaw(law);
                    List<ShenLaw> allLaws = ShenDataManager.loadLaws(player);
                    sendSyncLaws(player, allLaws);
                    LOGGER.info("已保存法则 {} 给玩家 {}", law.name, player.getName().getString());
                });
            }
            case DELETE_LAW_C2S -> {
                String lawName = payload;
                player.server.execute(() -> {
                    List<ShenLaw> laws = ShenDataManager.loadLaws(player);
                    laws.removeIf(l -> l.name.equals(lawName));
                    ShenDataManager.saveLaws(player, laws);
                    ShenEventBus.unregisterByName(lawName);
                    List<ShenLaw> allLaws = ShenDataManager.loadLaws(player);
                    sendSyncLaws(player, allLaws);
                    LOGGER.info("已删除法则 {} 给玩家 {}", lawName, player.getName().getString());
                });
            }
            default -> {
            }
        }
    }

    // ============ 客户端处理 S2C ============
    public static void handleClientMessage(String type, String payload) {
        ShenClientPacketHandler.handle(type, payload);
    }

    // ============ 服务端发送 ============
    public static void sendOpenScreen(ServerPlayer player) {
        sendToPlayer(player, OPEN_SCREEN, "false");
    }

    public static void sendOpenScreenLimited(ServerPlayer player) {
        sendToPlayer(player, OPEN_SCREEN, "true");
    }

    public static void sendSyncLaws(ServerPlayer player, List<ShenLaw> laws) {
        sendToPlayer(player, SYNC_LAWS_S2C, GSON.toJson(laws));
    }

    private static void sendToPlayer(ServerPlayer player, String type, String payload) {
        if (player == null) return;
        if (!Platforms.get().canSendTo(player)) return;
        Platforms.get().sendToPlayer(player, type, payload);
    }

    // ============ 客户端发送 ============
    public static void sendRequestLaws() {
        Platforms.get().sendToServer(REQUEST_LAWS_C2S, "");
    }

    public static void sendSaveLaw(ShenLaw law) {
        Platforms.get().sendToServer(SAVE_LAW_C2S, GSON.toJson(law));
    }

    public static void sendDeleteLaw(String lawName) {
        Platforms.get().sendToServer(DELETE_LAW_C2S, lawName);
    }

    /** 兼容旧入口：各加载器入口负责注册，无需显式调用 */
    public static void registerServer() {
    }

    public static void registerClient() {
    }
}
