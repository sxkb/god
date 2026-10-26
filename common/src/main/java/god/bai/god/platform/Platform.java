package god.bai.god.platform;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * 加载器平台抽象：业务代码只依赖本接口，Fabric / Forge 各提供一份实现。
 * <p>这是整套多加载器结构里唯一需要按加载器分别维护的部分。</p>
 */
public interface Platform {

    /** 配置目录（config/） */
    Path configDir();

    /** 某个模组是否已加载（用于神秘复苏等软依赖） */
    boolean isModLoaded(String modId);

    /** 当前是否为客户端逻辑端 */
    boolean isClient();

    /** 该玩家是否安装了本模组、可以接收本模组的包 */
    boolean canSendTo(ServerPlayer player);

    /** 客户端 -> 服务端 */
    void sendToServer(String type, String payload);

    /** 服务端 -> 指定玩家 */
    void sendToPlayer(ServerPlayer player, String type, String payload);
}
