package god.bai.god.fabric;

import net.fabricmc.api.ClientModInitializer;

public class GodFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetwork.registerClient();
    }
}
