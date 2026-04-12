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

package dev.wuason.toastapi.nms.v1_21_R1;

import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ToastImpl implements IToastWrapper {

    private static final String IMPOSSIBLE_KEY = "impossible";
    private static final Component TOAST_DESCRIPTION = Component.literal(".");

    private static Component parseComponent(String json) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        return Objects.requireNonNull(
                Component.Serializer.fromJson(json, craftServer.getServer().registryAccess()),
                "Invalid component JSON: " + json
        );
    }

    private static AdvancementType toAdvancementType(EToastType toastType) {
        return switch (toastType) {
            case TASK -> AdvancementType.TASK;
            case CHALLENGE -> AdvancementType.CHALLENGE;
            case GOAL -> AdvancementType.GOAL;
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
        ResourceLocation advancementId = ResourceLocation.fromNamespaceAndPath(namespace, path);

        net.minecraft.world.item.ItemStack nmsIcon = resolveIcon(icon);
        DisplayInfo displayInfo = createDisplayInfo(nmsIcon, title, toastType);
        Advancement advancement = buildAdvancement(displayInfo);
        AdvancementHolder holder = new AdvancementHolder(advancementId, advancement);
        AdvancementProgress progress = buildGrantedProgress();

        sendGrantPacket(serverPlayer, holder, advancementId, progress);
        sendRevokePacket(serverPlayer, advancementId);
    }

    private DisplayInfo createDisplayInfo(net.minecraft.world.item.ItemStack icon, String title, EToastType toastType) {
        return new DisplayInfo(
                icon,
                parseComponent(title),
                TOAST_DESCRIPTION,
                Optional.empty(),
                toAdvancementType(toastType),
                true,
                false,
                true
        );
    }

    private net.minecraft.world.item.ItemStack resolveIcon(ItemStack icon) {
        if (icon != null) {
            return CraftItemStack.asNMSCopy(icon);
        }
        return CraftItemStack.asNMSCopy(new ItemStack(Material.PAPER));
    }

    private Advancement buildAdvancement(DisplayInfo displayInfo) {
        Criterion<ImpossibleTrigger.TriggerInstance> impossibleCriterion =
                new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance());

        Map<String, Criterion<?>> criteria = Map.of(IMPOSSIBLE_KEY, impossibleCriterion);
        AdvancementRequirements requirements = new AdvancementRequirements(List.of(List.of(IMPOSSIBLE_KEY)));

        return new Advancement(
                Optional.empty(),
                Optional.of(displayInfo),
                AdvancementRewards.EMPTY,
                criteria,
                requirements,
                false
        );
    }

    private AdvancementProgress buildGrantedProgress() {
        AdvancementRequirements requirements = new AdvancementRequirements(List.of(List.of(IMPOSSIBLE_KEY)));
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(requirements);
        progress.getCriterion(IMPOSSIBLE_KEY).grant();
        return progress;
    }

    private void sendGrantPacket(ServerPlayer serverPlayer,
                                 AdvancementHolder holder,
                                 ResourceLocation advancementId,
                                 AdvancementProgress progress) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
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
