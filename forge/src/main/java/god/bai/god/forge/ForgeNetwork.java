package god.bai.god.forge;

import god.bai.god.shen.ShenNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ConnectionData;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Forge 网络传输层：单通道 god:main，载荷为「类型 + JSON」。
 * <p>业务分发在 {@link ShenNetwork}，本类只负责搬运。</p>
 */
public final class ForgeNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation("god", "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        CHANNEL_NAME,
        () -> PROTOCOL_VERSION,
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private ForgeNetwork() {
    }

    /** 触发静态初始化并完成消息注册 */
    public static void register() {
        if (!registered) {
            registered = true;
        }
    }

    private static boolean registered = false;

    static {
        CHANNEL.messageBuilder(ShenMessage.class, 0)
            .encoder(ShenMessage::encode)
            .decoder(ShenMessage::decode)
            .consumerMainThread(ForgeNetwork::handle)
            .add();
    }

    public static class ShenMessage {
        public final String type;
        public final String payload;

        public ShenMessage(String type, String payload) {
            this.type = type;
            this.payload = payload;
        }

        public static void encode(ShenMessage msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.type);
            buf.writeUtf(msg.payload);
        }

        public static ShenMessage decode(FriendlyByteBuf buf) {
            return new ShenMessage(buf.readUtf(32767), buf.readUtf(32767));
        }
    }

    private static void handle(ShenMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                ShenNetwork.handleServerMessage(ctx.getSender(), msg.type, msg.payload);
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShenNetwork.handleClientMessage(msg.type, msg.payload));
            }
        });
        ctx.setPacketHandled(true);
    }

    public static boolean canSendTo(ServerPlayer player) {
        if (player == null) return false;
        ConnectionData data = NetworkHooks.getConnectionData(player.connection.connection);
        return data != null && data.getChannels().containsKey(CHANNEL_NAME);
    }

    public static void sendToServer(String type, String payload) {
        CHANNEL.sendToServer(new ShenMessage(type, payload));
    }

    public static void sendToPlayer(ServerPlayer player, String type, String payload) {
        if (!canSendTo(player)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ShenMessage(type, payload));
    }
}
