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
import dev.wuason.toastapi.nms.NmsModuleLoader;
import dev.wuason.toastapi.protocol.PlayerVersionProvider;
import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleToast {

    private static final String DEFAULT_NAMESPACE = "simpletoastapi";
    private static final String DEFAULT_PATH = "toastannounce";
    private static final int MIN_TOAST_PROTOCOL = 754;

    private SimpleToast() {
    }

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

    public static void sendToast(@NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType,
                                 @NotNull String namespace,
                                 @NotNull String path) {
        sendToast(null, player, title, toastType, namespace, path);
    }

    public static void sendToast(@Nullable ItemStack icon,
                                 @NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType) {
        sendToast(icon, player, title, toastType, DEFAULT_NAMESPACE, DEFAULT_PATH);
    }

    public static void sendToast(@Nullable ItemStack icon,
                                 @NotNull Player player,
                                 @NotNull IContent title) {
        sendToast(icon, player, title, EToastType.TASK);
    }

    public static void sendToast(@NotNull Player player,
                                 @NotNull IContent title,
                                 @NotNull EToastType toastType) {
        sendToast(null, player, title, toastType);
    }

    public static void sendToast(@NotNull Player player, @NotNull IContent title) {
        sendToast(null, player, title, EToastType.TASK);
    }

    @Nullable
    private static IToastWrapper resolveWrapper(@NotNull Player player) {
        int protocol = PlayerVersionProvider.get().getProtocol(player);
        if (protocol < MIN_TOAST_PROTOCOL) {
            return null;
        }

        EMinecraftVersion serverVersion = EMinecraftVersion.getServerVersionSelected();
        EMinecraftVersion.NMSVersion nmsVersion = serverVersion.getNMSVersion();

        try {
            if (nmsVersion == EMinecraftVersion.NMSVersion.V26_1) {
                return resolveDynamicWrapper("v26_1", "dev.wuason.toastapi.nms.v26_1.ToastImpl");
            }
            if (nmsVersion == EMinecraftVersion.NMSVersion.V26_1_1) {
                return resolveDynamicWrapper("v26_1_1", "dev.wuason.toastapi.nms.v26_1_1.ToastImpl");
            }
            if (nmsVersion == EMinecraftVersion.NMSVersion.V26_1_2) {
                return resolveDynamicWrapper("v26_1_2", "dev.wuason.toastapi.nms.v26_1_2.ToastImpl");
            }

            String className = String.format(
                    "dev.wuason.toastapi.nms.v%s.ToastImpl",
                    nmsVersion.getVersionName()
            );

            return (IToastWrapper) Class.forName(className)
                    .getDeclaredConstructors()[0]
                    .newInstance();
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SimpleToastApi] Failed to load wrapper: " + e.getMessage());
            e.printStackTrace();

            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }

            return null;
        }
    }

    @NotNull
    private static IToastWrapper resolveDynamicWrapper(@NotNull String moduleName,
                                                       @NotNull String implementationClassName) {
        return NmsModuleLoader.load(moduleName, implementationClassName);
    }
}