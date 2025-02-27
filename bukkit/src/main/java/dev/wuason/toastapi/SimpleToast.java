package dev.wuason.toastapi;

import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
     * Sends a toast notification to a player with the specified parameters.
     *
     * @param icon      The icon (ItemStack) to display in the toast notification.
     * @param player    The player who will receive the toast notification.
     * @param title     The title text to be displayed in the toast notification.
     * @param toastType The type of toast notification (TASK, CHALLENGE, or GOAL).
     * @param namespace The namespace for the toast notification.
     * @param path      The path for the toast notification.
     */
    public static void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        toastWrapper.sendToast(icon, player, title, toastType, namespace, path);
    }

    /**
     * Sends a toast notification to a player using the specified parameters.
     * This overload does not include an icon, which defaults to null.
     *
     * @param player    The player who will receive the toast notification.
     * @param title     The title of the toast notification.
     * @param toastType The type of the toast (e.g., TASK, CHALLENGE, GOAL).
     * @param namespace The namespace used for the toast.
     * @param path      The path used for the toast.
     */
    public static void sendToast(Player player, String title, EToastType toastType, String namespace, String path) {
        sendToast(null, player, title, toastType, namespace, path);
    }

    /**
     * Sends a toast notification to a specified player with the provided title and type.
     *
     * @param player    the player to whom the toast notification will be sent
     * @param title     the title of the toast notification
     * @param toastType the type of the toast notification; must be one of the enum constants defined in {@link EToastType}
     */
    public static void sendToast(Player player, String title, EToastType toastType) {
        sendToast(null, player, title, toastType);
    }

    /**
     * Sends a toast notification to a specified player with a given title.
     *
     * @param player The player to whom the toast notification is to be sent.
     * @param title  The title of the toast notification.
     */
    public static void sendToast(Player player, String title) {
        sendToast(null, player, title, EToastType.TASK);
    }

    /**
     * Sends a toast notification to the specified player with the provided icon, title, and toast type.
     *
     * @param icon      The ItemStack icon to display in the toast. Can be null.
     * @param player    The player to whom the toast will be sent.
     * @param title     The title of the toast notification.
     * @param toastType The type of the toast (TASK, CHALLENGE, GOAL).
     */
    public static void sendToast(ItemStack icon, Player player, String title, EToastType toastType) {
        sendToast(icon, player, title, toastType, "simpletoastapi", "toastannounce");
    }
}