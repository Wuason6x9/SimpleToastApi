package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;

/**
 * Factory for creating version-specific toast implementations.
 * Reduces code duplication by grouping similar Minecraft versions.
 */
public class ToastManagerFactory {
    
    private static IToastWrapper instance;
    
    public static IToastWrapper getInstance() {
        if (instance == null) {
            EMinecraftVersion.NMSVersion version = EMinecraftVersion.getServerVersionSelected().getNMSVersion();
            
            try {
                // Group similar versions to reduce duplicate implementations
                if (version == EMinecraftVersion.NMSVersion.V1_16_R3) {
                    // 1.16 - unique API structure
                    instance = (IToastWrapper) Class.forName("dev.wuason.toastapi.nms.v1_16_R3.ToastImpl").getDeclaredConstructors()[0].newInstance();
                } else if (version.isLessThan(EMinecraftVersion.NMSVersion.V1_20_R1)) {
                    // 1.17-1.19 - similar API, can be unified
                    instance = new ClassicToastImpl(version);
                } else if (version.isLessThan(EMinecraftVersion.NMSVersion.V1_21_R2)) {
                    // 1.20-1.21.2 - similar API with registry access
                    instance = new ModernToastImpl(version);
                } else {
                    // 1.21.3+ - newest API with data components
                    instance = new NewestToastImpl(version);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create toast implementation for version " + version, e);
            }
        }
        return instance;
    }
}