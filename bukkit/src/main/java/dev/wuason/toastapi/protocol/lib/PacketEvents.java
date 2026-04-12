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

package dev.wuason.toastapi.protocol.lib;

import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.wuason.toastapi.protocol.PlayerVersion;
import org.bukkit.entity.Player;

public class PacketEvents implements PlayerVersion {

    @Override
    public int getProtocol(Player player) {
        PlayerManager manager = com.github.retrooper.packetevents.PacketEvents.getAPI().getPlayerManager();
        ClientVersion clientVersion = manager.getClientVersion(player);
        return clientVersion.getProtocolVersion();
    }
}
