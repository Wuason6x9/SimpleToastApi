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

package dev.wuason.toastapi.nms.v1_20_R1;

import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ToastImpl implements IToastWrapper {

    private static final String IMPOSSIBLE_KEY = "impossible";
    private static final Component TOAST_DESCRIPTION = Component.literal(".");

    private static Component parseComponent(String json) {
        return Objects.requireNonNull(
                Component.Serializer.fromJson(json),
                "Invalid component JSON: " + json
        );
    }

    private static FrameType toFrameType(EToastType toastType) {
        return switch (toastType) {
            case TASK -> FrameType.TASK;
            case CHALLENGE -> FrameType.CHALLENGE;
            case GOAL -> FrameType.GOAL;
        };
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(toastType, "toastType cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(path, "path cannot be null");

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ResourceLocation advancementId = new ResourceLocation(namespace, path);

        net.minecraft.world.item.ItemStack nmsIcon = resolveIcon(icon);
        DisplayInfo displayInfo = createDisplayInfo(nmsIcon, title, toastType);

        Map<String, Criterion> criteria = createCriteria();
        String[][] requirements = createRequirements();

        Advancement advancement = new Advancement(
                advancementId,
                null,
                displayInfo,
                AdvancementRewards.EMPTY,
                criteria,
                requirements,
                false
        );

        AdvancementProgress progress = buildGrantedProgress(criteria, requirements);

        sendGrantPacket(serverPlayer, advancement, advancementId, progress);
        sendRevokePacket(serverPlayer, advancementId);
    }

    private DisplayInfo createDisplayInfo(net.minecraft.world.item.ItemStack icon, String title, EToastType toastType) {
        return new DisplayInfo(
                icon,
                parseComponent(title),
                TOAST_DESCRIPTION,
                null,
                toFrameType(toastType),
                true,
                false,
                true
        );
    }

    private net.minecraft.world.item.ItemStack resolveIcon(ItemStack icon) {
        if (icon != null) {
            return CraftItemStack.asNMSCopy(icon);
        }
        return CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));
    }

    private Map<String, Criterion> createCriteria() {
        Criterion criterion = new Criterion(new ImpossibleTrigger.TriggerInstance());
        return Map.of(IMPOSSIBLE_KEY, criterion);
    }

    private String[][] createRequirements() {
        return new String[][]{{IMPOSSIBLE_KEY}};
    }

    private AdvancementProgress buildGrantedProgress(Map<String, Criterion> criteria, String[][] requirements) {
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(criteria, requirements);
        progress.getCriterion(IMPOSSIBLE_KEY).grant();
        return progress;
    }

    private void sendGrantPacket(ServerPlayer serverPlayer,
                                 Advancement advancement,
                                 ResourceLocation advancementId,
                                 AdvancementProgress progress) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(advancement),
                Set.of(),
                Map.of(advancementId, progress)
        );
        serverPlayer.connection.send(packet);
    }

    private void sendRevokePacket(ServerPlayer serverPlayer, ResourceLocation advancementId) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(),
                Set.of(advancementId),
                Map.of()
        );
        serverPlayer.connection.send(packet);
    }
}