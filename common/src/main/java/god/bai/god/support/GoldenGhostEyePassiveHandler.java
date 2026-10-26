package god.bai.god.support;

import net.minecraft.world.entity.player.Player;

/**
 * 金色鬼眼被动（鬼域模组功能）兼容占位实现。
 *
 * <p>言出法随独立移植后不再包含鬼域模组的金色鬼眼体系，
 * 因此这里恒定返回 false；若后续需要与鬼域联动，可在此处按需接入。</p>
 */
public final class GoldenGhostEyePassiveHandler {

    private GoldenGhostEyePassiveHandler() {
    }

    public static boolean isGoldenGhostEyeMain(Player player) {
        return false;
    }
}
