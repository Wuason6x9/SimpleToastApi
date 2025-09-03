package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
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

/**
 * Toast implementation for Minecraft versions 1.20-1.21.2.
 * Handles v1_20_R1, v1_20_R2, v1_20_R3, v1_20_R4, v1_21_R1.
 */
public class ModernToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    private final boolean usesAdvancementRequirements;
    private final boolean usesAdvancementHolder;
    private final boolean hasExtraAdvancementParam;
    
    public ModernToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
        this.usesAdvancementRequirements = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R3);
        this.usesAdvancementHolder = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R3);
        this.hasExtraAdvancementParam = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R1);
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        // Create NMS ItemStack
        net.minecraft.world.item.ItemStack iconNMS = CraftItemStack.asNMSCopy(
            icon != null ? icon : new ItemStack(Material.PAPER));
        if (icon != null) {
            iconNMS = CraftItemStack.asNMSCopy(icon);
        }
        
        // Create title component with registry access (required for v1_20+)
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        Component titleComponent = Objects.requireNonNull(
            Component.Serializer.fromJson(title, craftServer.getServer().registryAccess()));
        Component subtitleComponent = Component.literal(".");
        
        // Create display info using AdvancementType (v1_20+ uses AdvancementType instead of FrameType)
        AdvancementType advancementType = AdvancementType.valueOf(toastType.toString());
        
        Optional<DisplayInfo> displayInfo = Optional.of(new DisplayInfo(iconNMS, titleComponent, 
            subtitleComponent, Optional.empty(), advancementType, true, false, true));
        
        // Create advancement components
        AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
        ResourceLocation id = new ResourceLocation(namespace, path);
        
        // Create criterion
        Criterion<ImpossibleTrigger.TriggerInstance> criterion;
        if (usesAdvancementRequirements) {
            // v1_20_R3+ uses new Criterion constructor with trigger
            criterion = new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance());
        } else {
            // v1_20_R1-R2 still uses simple constructor
            criterion = new Criterion<>(new ImpossibleTrigger.TriggerInstance());
        }
        
        HashMap<String, Criterion<?>> criteria = new HashMap<>();
        criteria.put("impossible", criterion);
        
        // Create advancement based on version
        Advancement advancement;
        if (usesAdvancementRequirements) {
            // v1_20_R3+ uses AdvancementRequirements
            List<List<String>> requirements = Collections.singletonList(
                Collections.singletonList("impossible"));
            AdvancementRequirements advancementRequirements = new AdvancementRequirements(requirements);
            
            advancement = new Advancement(Optional.empty(), displayInfo, advancementRewards, 
                criteria, advancementRequirements, false);
        } else {
            // v1_20_R1-R2 use String[][] requirements
            String[][] requirements = {{"impossible"}};
            if (hasExtraAdvancementParam) {
                advancement = new Advancement(id, null, displayInfo.get(), advancementRewards, 
                    criteria, requirements, false);
            } else {
                advancement = new Advancement(id, null, displayInfo.get(), advancementRewards, 
                    criteria, requirements);
            }
        }
        
        // Create advancement progress
        AdvancementProgress advancementProgress = new AdvancementProgress();
        if (usesAdvancementRequirements) {
            AdvancementRequirements advancementRequirements = new AdvancementRequirements(
                Collections.singletonList(Collections.singletonList("impossible")));
            advancementProgress.update(advancementRequirements);
        } else {
            String[][] requirements = {{"impossible"}};
            advancementProgress.update(criteria, requirements);
        }
        advancementProgress.getCriterion("impossible").grant();
        
        // Setup advancement map
        Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
        advancementsToGrant.put(id, advancementProgress);
        
        // Send packets
        List<Object> advancementList = new ArrayList<>();
        if (usesAdvancementHolder) {
            // v1_20_R3+ uses AdvancementHolder
            advancementList.add(new AdvancementHolder(id, advancement));
        } else {
            advancementList.add(advancement);
        }
        
        ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false, 
            advancementList, new HashSet<>(), advancementsToGrant);
        serverPlayer.connection.send(packet1);
        
        ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, 
            new ArrayList<>(), Collections.singleton(id), new HashMap<>());
        serverPlayer.connection.send(packet2);
    }
}