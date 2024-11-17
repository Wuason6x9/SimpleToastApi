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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ToastImpl implements IToastWrapper {

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        EntityPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        net.minecraft.server.v1_16_R3.ItemStack iconNMS = CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));
        if (icon != null) {
            iconNMS = CraftItemStack.asNMSCopy(icon);
        }
        AdvancementDisplay displayInfo = new AdvancementDisplay(iconNMS, CraftChatMessage.fromStringOrNull(title), CraftChatMessage.fromStringOrNull("."), null, AdvancementFrameType.valueOf(toastType.toString()), true, false, true);
        AdvancementRewards advancementRewards = AdvancementRewards.a;
        MinecraftKey id = new MinecraftKey(namespace, path);
        Criterion criterion = new Criterion(new CriterionTriggerImpossible.a());
        HashMap<String, Criterion> criteria = new HashMap<>() {{
            put("impossible", criterion);
        }};
        String[][] requirements = {{"impossible"}};
        Advancement advancement = new Advancement(id, null, displayInfo, advancementRewards, criteria, requirements);
        Map<MinecraftKey, AdvancementProgress> advancementsToGrant = new HashMap<>();
        AdvancementProgress advancementProgress = new AdvancementProgress();
        advancementProgress.a(criteria, requirements);
        advancementProgress.getCriterionProgress("impossible").b();
        advancementsToGrant.put(id, advancementProgress);
        PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, new ArrayList<>() {{
            add(advancement);
        }}, new HashSet<>(), advancementsToGrant);
        serverPlayer.playerConnection.networkManager.sendPacket(packet);
        PacketPlayOutAdvancements packet2 = new PacketPlayOutAdvancements(false, new ArrayList<>(), new HashSet<>() {{
            add(id);
        }}, new HashMap<>());
        serverPlayer.playerConnection.networkManager.sendPacket(packet2);
    }

}
