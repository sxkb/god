package god.bai.god.forge;

import god.bai.god.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatform implements Platform {

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean canSendTo(ServerPlayer player) {
        return ForgeNetwork.canSendTo(player);
    }

    @Override
    public void sendToServer(String type, String payload) {
        ForgeNetwork.sendToServer(type, payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, String type, String payload) {
        ForgeNetwork.sendToPlayer(player, type, payload);
    }
}
