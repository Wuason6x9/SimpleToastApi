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

package dev.wuason.toastapi.protocol;

import dev.wuason.toastapi.protocol.lib.None;
import dev.wuason.toastapi.protocol.lib.PacketEvents;
import dev.wuason.toastapi.protocol.lib.ProtocolLib;
import dev.wuason.toastapi.protocol.lib.ViaVersion;
import org.bukkit.Bukkit;

/**
 * Resolves the best available {@link PlayerVersion} provider at startup.
 *
 * <p>Priority order:
 * <ol>
 *   <li>ViaVersion — most common for multi-version servers</li>
 *   <li>ProtocolLib — widely used utility library</li>
 *   <li>PacketEvents — modern alternative to ProtocolLib</li>
 *   <li>None — fallback, always returns server protocol</li>
 * </ol>
 */
public class PlayerVersionProvider {

    private static volatile PlayerVersion instance = null;

    private PlayerVersionProvider() {
    }

    /**
     * Returns the resolved {@link PlayerVersion} instance.
     * Resolved once and cached for the lifetime of the server.
     *
     * @return the active {@link PlayerVersion} implementation
     */
    public static PlayerVersion get() {
        if (instance != null) {
            return instance;
        }

        synchronized (PlayerVersionProvider.class) {
            if (instance != null) {
                return instance;
            }
            instance = resolve();
        }

        return instance;
    }

    /**
     * Forces re-resolution of the provider (useful for reloads or tests).
     */
    public static void reset() {
        synchronized (PlayerVersionProvider.class) {
            instance = null;
        }
    }

    private static PlayerVersion resolve() {
        if (isPluginPresent("ViaVersion")) {
            Bukkit.getLogger().info("[SimpleToastApi] Using ViaVersion for client version detection.");
            return new ViaVersion();
        }

        if (isPluginPresent("ProtocolLib")) {
            Bukkit.getLogger().info("[SimpleToastApi] Using ProtocolLib for client version detection.");
            return new ProtocolLib();
        }

        if (isPluginPresent("packetevents")) {
            Bukkit.getLogger().info("[SimpleToastApi] Using PacketEvents for client version detection.");
            return new PacketEvents();
        }

        Bukkit.getLogger().warning(
                "[SimpleToastApi] No protocol library found (ViaVersion / ProtocolLib / PacketEvents). " +
                        "Toast will only work for players using the server version. " +
                        "Install ViaVersion for multi-version support."
        );
        return new None();
    }

    private static boolean isPluginPresent(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}
