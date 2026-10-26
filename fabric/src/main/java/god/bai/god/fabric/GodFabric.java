package god.bai.god.fabric;

import god.bai.GodBootstrap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class GodFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        GodBootstrap.init(new FabricPlatform());

        FabricNetwork.registerCommon();
        FabricEvents.registerCommon();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            GodBootstrap.registerCommands(dispatcher));

        GodBootstrap.LOGGER.info("言出法随 Fabric 入口初始化完成");
    }
}
