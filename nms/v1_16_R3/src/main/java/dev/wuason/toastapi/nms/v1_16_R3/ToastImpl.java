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

package dev.wuason.toastapi.nms.v1_16_R3;

import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ToastImpl implements IToastWrapper {

    private static final String IMPOSSIBLE_KEY = "impossible";
    private static final String TOAST_DESCRIPTION = ".";

    private static AdvancementFrameType toFrameType(EToastType toastType) {
        return switch (toastType) {
            case TASK -> AdvancementFrameType.TASK;
            case CHALLENGE -> AdvancementFrameType.CHALLENGE;
            case GOAL -> AdvancementFrameType.GOAL;
        };
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(toastType, "toastType cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(path, "path cannot be null");

        EntityPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        MinecraftKey advancementId = new MinecraftKey(namespace, path);

        net.minecraft.server.v1_16_R3.ItemStack nmsIcon = resolveIcon(icon);
        AdvancementDisplay displayInfo = createDisplayInfo(nmsIcon, title, toastType);

        Map<String, Criterion> criteria = createCriteria();
        String[][] requirements = createRequirements();

        Advancement advancement = new Advancement(
                advancementId,
                null,
                displayInfo,
                AdvancementRewards.a,
                criteria,
                requirements
        );

        AdvancementProgress progress = buildGrantedProgress(criteria, requirements);

        sendGrantPacket(serverPlayer, advancement, advancementId, progress);
        sendRevokePacket(serverPlayer, advancementId);
    }

    private AdvancementDisplay createDisplayInfo(net.minecraft.server.v1_16_R3.ItemStack icon, String title, EToastType toastType) {
        return new AdvancementDisplay(
                icon,
                CraftChatMessage.fromJSON(title),
                CraftChatMessage.fromStringOrNull(TOAST_DESCRIPTION),
                null,
                toFrameType(toastType),
                true,
                false,
                true
        );
    }

    private net.minecraft.server.v1_16_R3.ItemStack resolveIcon(ItemStack icon) {
        if (icon != null) {
            return CraftItemStack.asNMSCopy(icon);
        }
        return CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));
    }

    private Map<String, Criterion> createCriteria() {
        Criterion criterion = new Criterion(new CriterionTriggerImpossible.a());
        return Map.of(IMPOSSIBLE_KEY, criterion);
    }

    private String[][] createRequirements() {
        return new String[][]{{IMPOSSIBLE_KEY}};
    }

    private AdvancementProgress buildGrantedProgress(Map<String, Criterion> criteria, String[][] requirements) {
        AdvancementProgress progress = new AdvancementProgress();
        progress.a(criteria, requirements);
        progress.getCriterionProgress(IMPOSSIBLE_KEY).b();
        return progress;
    }

    private void sendGrantPacket(EntityPlayer serverPlayer,
                                 Advancement advancement,
                                 MinecraftKey advancementId,
                                 AdvancementProgress progress) {
        PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(
                false,
                List.of(advancement),
                Set.of(),
                Map.of(advancementId, progress)
        );
        serverPlayer.playerConnection.sendPacket(packet);
    }

    private void sendRevokePacket(EntityPlayer serverPlayer, MinecraftKey advancementId) {
        PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(
                false,
                List.of(),
                Set.of(advancementId),
                Map.of()
        );
        serverPlayer.playerConnection.sendPacket(packet);
    }
}
