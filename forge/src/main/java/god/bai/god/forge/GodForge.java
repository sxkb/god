package god.bai.god.forge;

import god.bai.GodBootstrap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(GodBootstrap.MOD_ID)
public class GodForge {

    public GodForge() {
        GodBootstrap.init(new ForgePlatform());
        ForgeNetwork.register();
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        MinecraftForge.EVENT_BUS.register(this);
        GodBootstrap.LOGGER.info("言出法随 Forge 入口初始化完成");
    }

    // 注册 /god shen 命令（无权限要求，任何玩家可用）
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GodBootstrap.registerCommands(event.getDispatcher());
    }
}
