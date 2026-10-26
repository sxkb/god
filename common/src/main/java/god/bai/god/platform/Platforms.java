package god.bai.god.platform;

/**
 * 平台实现的全局持有者。由各加载器入口在初始化时注入。
 */
public final class Platforms {

    private static Platform impl;

    private Platforms() {
    }

    public static void set(Platform platform) {
        impl = platform;
    }

    public static Platform get() {
        if (impl == null) {
            throw new IllegalStateException("Platform 尚未初始化：请检查加载器入口是否调用了 GodBootstrap.init()");
        }
        return impl;
    }
}
