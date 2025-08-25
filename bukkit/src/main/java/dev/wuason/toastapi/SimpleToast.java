package dev.wuason.toastapi;

import dev.wuason.toastapi.content.IContent;
import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point utility for sending toast notifications to players using different content sources.
 * <p>
 * All textual content is provided via {@link IContent} implementations which must return
 * a valid JSON text component string understood by the client.
 * Provided overloads allow specifying (optionally) the icon, toast type, namespace and path.
 * Convenience variants fall back to sensible defaults:
 * <ul>
 *   <li>Namespace: <code>"simpletoastapi"</code></li>
 *   <li>Path: <code>"toastannounce"</code></li>
 *   <li>Toast type: {@link EToastType#TASK}</li>
 * </ul>
 */
public class SimpleToast {
    private static final IToastWrapper toastWrapper;

    static {
        EMinecraftVersion version = EMinecraftVersion.getServerVersionSelected();
        try {
            toastWrapper = (IToastWrapper) Class.forName(String.format("dev.wuason.toastapi.nms.v%s.ToastImpl", version.getNMSVersion().getVersionName())).getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Fatal error occurred", e);
        }
    }

    /**
     * Sends a toast notification with full parameter control.
     *
     * @param icon      Optional icon (may be null) shown in the toast.
     * @param player    Target player (not null).
     * @param title     Content provider returning JSON text component (not null).
     * @param toastType Visual style / frame of the toast.
     * @param namespace Advancement/notification namespace identifier.
     * @param path      Advancement/notification path identifier.
     */
    public static void sendToast(@Nullable ItemStack icon, @NotNull Player player, @NotNull IContent title, @NotNull EToastType toastType, @NotNull String namespace, @NotNull String path) {
        toastWrapper.sendToast(icon, player, title.getContent(), toastType, namespace, path);
    }

    /**
     * Sends a toast without an icon, retaining control over all other parameters.
     *
     * @param player    Target player.
     * @param title     JSON content wrapper.
     * @param toastType Toast type/frame.
     * @param namespace Namespace identifier.
     * @param path      Path identifier.
     */
    public static void sendToast(Player player, IContent title, EToastType toastType, String namespace, String path) {
        sendToast(null, player, title, toastType, namespace, path);
    }

    /**
     * Sends a toast specifying an icon, player and content with default namespace/path.
     *
     * @param icon      Icon item (nullable allowed but use other overload if null).
     * @param player    Target player.
     * @param title     Content provider.
     * @param toastType Toast type/frame.
     */
    public static void sendToast(ItemStack icon, Player player, IContent title, EToastType toastType) {
        sendToast(icon, player, title, toastType, "simpletoastapi", "toastannounce");
    }

    /**
     * Sends a toast specifying an icon, player and content with default toast type (TASK) and default namespace/path.
     *
     * @param icon   Icon item to display.
     * @param player Target player.
     * @param title  Content provider.
     */
    public static void sendToast(ItemStack icon, Player player, IContent title) {
        sendToast(icon, player, title, EToastType.TASK);
    }

    /**
     * Sends a toast without icon and with default namespace/path.
     *
     * @param player    Target player.
     * @param title     Content provider.
     * @param toastType Toast type/frame.
     */
    public static void sendToast(Player player, IContent title, EToastType toastType) {
        sendToast(null, player, title, toastType);
    }

    /**
     * Sends a toast without icon using default toast type (TASK) and default namespace/path.
     *
     * @param player Target player.
     * @param title  Content provider.
     */
    public static void sendToast(Player player, IContent title) {
        sendToast(null, player, title, EToastType.TASK);
    }
}