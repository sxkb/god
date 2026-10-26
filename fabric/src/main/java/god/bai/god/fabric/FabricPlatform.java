package god.bai.god.fabric;

import god.bai.god.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatform implements Platform {

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean canSendTo(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, FabricNetwork.CHANNEL);
    }

    @Override
    public void sendToServer(String type, String payload) {
        FabricNetwork.sendToServer(type, payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, String type, String payload) {
        FabricNetwork.sendToPlayer(player, type, payload);
    }
}
