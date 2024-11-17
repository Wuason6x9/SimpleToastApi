package dev.wuason.toastapi.nms.v1_19_R1;

import dev.wuason.toastapi.nms.EToastType;
import dev.wuason.toastapi.nms.IToastWrapper;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ToastImpl implements IToastWrapper {

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        net.minecraft.world.item.ItemStack iconNMS = CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));
        if (icon != null) {
            iconNMS = CraftItemStack.asNMSCopy(icon);
        }
        DisplayInfo displayInfo = new DisplayInfo(iconNMS, Component.literal(title), Component.literal("."), null, FrameType.valueOf(toastType.toString()), true, false, true);
        AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
        ResourceLocation id = new ResourceLocation(namespace, path);
        Criterion criterion = new Criterion(new ImpossibleTrigger.TriggerInstance());
        HashMap<String, Criterion> criteria = new HashMap<>() {{
            put("impossible", criterion);
        }};
        String[][] requirements = {{"impossible"}};
        Advancement advancement = new Advancement(id, null, displayInfo, advancementRewards, criteria, requirements);
        Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
        AdvancementProgress advancementProgress = new AdvancementProgress();
        advancementProgress.update(criteria, requirements);
        advancementProgress.getCriterion("impossible").grant();
        advancementsToGrant.put(id, advancementProgress);
        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, new ArrayList<>() {{
            add(advancement);
        }}, new HashSet<>(), advancementsToGrant);
        serverPlayer.connection.send(packet);
        ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, new ArrayList<>(), new HashSet<>() {{
            add(id);
        }}, new HashMap<>());
        serverPlayer.connection.send(packet2);
    }

}
