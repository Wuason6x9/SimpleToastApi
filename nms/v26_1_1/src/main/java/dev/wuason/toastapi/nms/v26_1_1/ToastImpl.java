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

package dev.wuason.toastapi.nms.v26_1_1;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import net.minecraft.advancements.*;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ToastImpl implements IToastWrapper {

    private static final String IMPOSSIBLE_KEY = "impossible";
    private static final String DEFAULT_NAMESPACE = "minecraft";
    private static final String EMPTY_MODEL_PATH = "air";

    private static Component parseComponent(String json) {
        JsonElement element = JsonParser.parseString(json);
        return ComponentSerialization.CODEC
                .parse(JsonOps.INSTANCE, element)
                .resultOrPartial()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid component JSON: " + json));
    }

    private static AdvancementType toAdvancementType(EToastType toastType) {
        return switch (toastType) {
            case TASK -> AdvancementType.TASK;
            case CHALLENGE -> AdvancementType.CHALLENGE;
            case GOAL -> AdvancementType.GOAL;
        };
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title,
                          EToastType toastType, String namespace, String path) {

        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(toastType, "toastType cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(path, "path cannot be null");

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        Identifier advancementId = Identifier.fromNamespaceAndPath(namespace, path);

        net.minecraft.world.item.ItemStack nmsStack = resolveIcon(icon);
        ItemStackTemplate iconTemplate = ItemStackTemplate.fromNonEmptyStack(nmsStack);

        DisplayInfo displayInfo = new DisplayInfo(
                iconTemplate,
                parseComponent(title),
                Component.literal("."),
                Optional.empty(),
                toAdvancementType(toastType),
                true, false, true
        );

        Advancement advancement = buildAdvancement(displayInfo);
        AdvancementHolder holder = new AdvancementHolder(advancementId, advancement);
        AdvancementProgress progress = buildGrantedProgress();

        sendGrantPacket(serverPlayer, holder, advancementId, progress);
        sendRevokePacket(serverPlayer, advancementId);
    }

    private net.minecraft.world.item.ItemStack resolveIcon(ItemStack icon) {
        if (icon != null) {
            return CraftItemStack.asNMSCopy(icon);
        }
        net.minecraft.world.item.ItemStack fallback =
                CraftItemStack.asNMSCopy(new ItemStack(Material.STICK));

        DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponents.ITEM_MODEL,
                        Identifier.fromNamespaceAndPath(DEFAULT_NAMESPACE, EMPTY_MODEL_PATH))
                .build();
        fallback.applyComponents(patch);
        return fallback;
    }

    private Advancement buildAdvancement(DisplayInfo displayInfo) {
        Map<String, Criterion<?>> criteria =
                Map.of(IMPOSSIBLE_KEY,
                        new Criterion<>(new ImpossibleTrigger(),
                                new ImpossibleTrigger.TriggerInstance()));

        AdvancementRequirements requirements =
                new AdvancementRequirements(List.of(List.of(IMPOSSIBLE_KEY)));

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
        AdvancementRequirements requirements =
                new AdvancementRequirements(List.of(List.of(IMPOSSIBLE_KEY)));

        AdvancementProgress progress = new AdvancementProgress();
        progress.update(requirements);
        progress.getCriterion(IMPOSSIBLE_KEY).grant();
        return progress;
    }

    private void sendGrantPacket(ServerPlayer serverPlayer,
                                 AdvancementHolder holder,
                                 Identifier id,
                                 AdvancementProgress progress) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
                Set.of(),
                Map.of(id, progress),
                true
        );
        serverPlayer.connection.send(packet);
    }

    private void sendRevokePacket(ServerPlayer serverPlayer, Identifier id) {
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(),
                Set.of(id),
                Map.of(),
                true
        );
        serverPlayer.connection.send(packet);
    }
}