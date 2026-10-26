package god.bai.god.fabric;

import god.bai.god.shen.ShenNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 网络传输层：单通道 god:main，载荷为「类型 + JSON」。
 * <p>真正的业务分发在 {@link ShenNetwork}，本类只负责搬运。</p>
 */
public final class FabricNetwork {

    public static final ResourceLocation CHANNEL = new ResourceLocation("god", "main");

    private FabricNetwork() {
    }

    public static void registerCommon() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            String type = buf.readUtf(32767);
            String payload = buf.readUtf(32767);
            server.execute(() -> ShenNetwork.handleServerMessage(player, type, payload));
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            String type = buf.readUtf(32767);
            String payload = buf.readUtf(32767);
            client.execute(() -> ShenNetwork.handleClientMessage(type, payload));
        });
    }

    public static void sendToServer(String type, String payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(type);
        buf.writeUtf(payload);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    public static void sendToPlayer(ServerPlayer player, String type, String payload) {
        if (!ServerPlayNetworking.canSend(player, CHANNEL)) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(type);
        buf.writeUtf(payload);
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }
}
