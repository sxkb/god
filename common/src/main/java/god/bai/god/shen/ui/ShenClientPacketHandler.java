package god.bai.god.shen.ui;

import god.bai.god.shen.ShenLaw;
import god.bai.god.shen.ShenNetwork;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 客户端专用网络处理器。
 * <p>只处理服务端发往客户端的 S2C 消息（打开界面 / 同步法则）。
 * 本类引用 net.minecraft.client 下的类，绝不能由专用服务器加载；
 * 各加载器只在客户端网络分支里调用它。</p>
 */
public final class ShenClientPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("god-shen-network");

    private ShenClientPacketHandler() {
    }

    public static void handle(String type, String payload) {
        if (type == null) return;
        Minecraft client = Minecraft.getInstance();
        switch (type) {
            case ShenNetwork.OPEN_SCREEN -> {
                boolean limited = Boolean.parseBoolean(payload);
                client.execute(() -> {
                    LOGGER.info("收到打开言出法随界面请求{}", limited ? "（阉割版）" : "");
                    if (client.screen == null) {
                        client.setScreen(new ShenScreen(limited));
                        LOGGER.info("成功打开言出法随界面");
                    }
                });
            }
            case ShenNetwork.SYNC_LAWS_S2C -> {
                List<ShenLaw> laws = ShenNetwork.GSON.fromJson(payload, new TypeToken<List<ShenLaw>>() {
                }.getType());
                client.execute(() -> {
                    if (client.screen instanceof ShenScreen) {
                        ((ShenScreen) client.screen).onSyncLaws(laws);
                    }
                });
            }
            default -> {
            }
        }
    }
}
