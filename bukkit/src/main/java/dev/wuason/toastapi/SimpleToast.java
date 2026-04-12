/*
 *     Copyright (C) 2026 Wuason6x9 and RubenArtz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.wuason.toastapi;

import dev.wuason.toastapi.content.IContent;
import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import dev.wuason.toastapi.protocol.PlayerVersionProvider;
import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point utility for sending toast notifications to players.
 *
 * <p>All textual content is provided via {@link IContent} implementations which must return
 * a valid JSON text component string understood by the client.
 *
 * <p>Provided overloads allow specifying (optionally) the icon, toast type, namespace and path.
 * Convenience variants fall back to sensible defaults:
 * <ul>
 *   <li>Namespace: {@code "simpletoastapi"}</li>
 *   <li>Path: {@code "toastannounce"}</li>
 *   <li>Toast type: {@link EToastType#TASK}</li>
 * </ul>
 *
 * <p>Players connecting with a protocol version below {@code 754} (Minecraft 1.16.5)
 * will not receive a toast, as the advancement packet required is not available on those clients.
 */
public class SimpleToast {

    private static final String DEFAULT_NAMESPACE = "simpletoastapi";
    private static final String DEFAULT_PATH = "toastannounce";

    /**
     * Minimum protocol version that supports the advancement packet used to display toasts.
     * Protocol {@code 754} corresponds to Minecraft 1.16.5.
     */
    private static final int MIN_TOAST_PROTOCOL = 754;

    private SimpleToast() {
    }

    /**
     * Sends a toast notification with full parameter control.
     *
     * @param icon      Optional item icon displayed in the toast. Pass {@code null} for no icon.
     * @param player    Target player. Must not be {@code null}.
     * @param title     Content provider returning a valid JSON text component. Must not be {@code null}.
     * @param toastType Visual frame style of the toast (TASK, GOAL or CHALLENGE).
     * @param namespace Advancement namespace used internally to identify this toast packet.
     * @param path      Advancement path used internally to identify this toast packet.
     */
    public static void sendToast(@Nullable ItemStack icon,
                                 @NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType,
                                 @NotNull String namespace,
                                 @NotNull String path) {
        IToastWrapper wrapper = resolveWrapper(player);
        if (wrapper == null) {
            return;
        }
        wrapper.sendToast(icon, player, title.getContent(), toastType, namespace, path);
    }

    /**
     * Sends a toast without an icon, retaining control over all other parameters.
     *
     * @param player    Target player. Must not be {@code null}.
     * @param title     Content provider returning a valid JSON text component. Must not be {@code null}.
     * @param toastType Visual frame style of the toast.
     * @param namespace Advancement namespace used internally to identify this toast packet.
     * @param path      Advancement path used internally to identify this toast packet.
     */
    public static void sendToast(@NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType,
                                 @NotNull String namespace,
                                 @NotNull String path) {
        sendToast(null, player, title, toastType, namespace, path);
    }

    /**
     * Sends a toast with a custom icon and toast type, using the default namespace and path.
     *
     * @param icon      Optional item icon displayed in the toast. Pass {@code null} for no icon.
     * @param player    Target player. Must not be {@code null}.
     * @param title     Content provider returning a valid JSON text component. Must not be {@code null}.
     * @param toastType Visual frame style of the toast.
     */
    public static void sendToast(@Nullable ItemStack icon,
                                 @NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType) {
        sendToast(icon, player, title, toastType, DEFAULT_NAMESPACE, DEFAULT_PATH);
    }

    /**
     * Sends a toast with a custom icon using default toast type (TASK),
     * default namespace and default path.
     *
     * @param icon   Optional item icon displayed in the toast. Pass {@code null} for no icon.
     * @param player Target player. Must not be {@code null}.
     * @param title  Content provider returning a valid JSON text component. Must not be {@code null}.
     */
    public static void sendToast(@Nullable ItemStack icon,
                                 @NotNull Player player,
                                 @NotNull IContent title) {
        sendToast(icon, player, title, EToastType.TASK);
    }

    /**
     * Sends a toast without an icon, with a custom toast type and default namespace/path.
     *
     * @param player    Target player. Must not be {@code null}.
     * @param title     Content provider returning a valid JSON text component. Must not be {@code null}.
     * @param toastType Visual frame style of the toast.
     */
    public static void sendToast(@NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType) {
        sendToast(null, player, title, toastType);
    }

    /**
     * Sends a toast without an icon using default toast type (TASK),
     * default namespace and default path.
     *
     * @param player Target player. Must not be {@code null}.
     * @param title  Content provider returning a valid JSON text component. Must not be {@code null}.
     */
    public static void sendToast(@NotNull Player player, @NotNull IContent title) {
        sendToast(null, player, title, EToastType.TASK);
    }

    /**
     * Resolves the {@link IToastWrapper} for the current server version.
     *
     * <p>If the client's protocol version is below {@link #MIN_TOAST_PROTOCOL},
     * {@code null} is returned and no packet is sent.
     * Otherwise, the wrapper is loaded for the server's NMS version.
     *
     * @param player the target player, used to read the client protocol version.
     * @return the resolved {@link IToastWrapper}, or {@code null} if the client
     * is too old or the server version has no NMS implementation.
     */
    @Nullable
    private static IToastWrapper resolveWrapper(@NotNull Player player) {
        int protocol = PlayerVersionProvider.get().getProtocol(player);
        if (protocol < MIN_TOAST_PROTOCOL) {
            return null;
        }

        EMinecraftVersion serverVersion = EMinecraftVersion.getServerVersionSelected();

        try {
            String className = String.format(
                    "dev.wuason.toastapi.nms.v%s.ToastImpl",
                    serverVersion.getNMSVersion().getVersionName()
            );
            return (IToastWrapper) Class.forName(className)
                    .getDeclaredConstructors()[0]
                    .newInstance();
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SimpleToastApi] Failed to load wrapper: " + e.getMessage());
            return null;
        }
    }
}