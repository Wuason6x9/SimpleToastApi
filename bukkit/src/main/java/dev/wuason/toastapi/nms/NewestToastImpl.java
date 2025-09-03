package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
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
 * Toast implementation for Minecraft versions 1.21.3+.
 * Handles v1_21_R2, v1_21_R3, v1_21_R4, v1_21_R5 and future versions.
 */
public class NewestToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    
    public NewestToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        
        // Create NMS ItemStack with special handling for null icons
        net.minecraft.world.item.ItemStack iconNMS;
        if (icon == null) {
            // v1_21_R2+ uses STICK with invisible appearance for null icons
            iconNMS = CraftItemStack.asNMSCopy(new ItemStack(Material.STICK));
            
            // Apply data component patch to make icon invisible
            DataComponentPatch dataComponentPatch = DataComponentPatch.builder()
                .set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("minecraft", "air"))
                .build();
            iconNMS.applyComponents(dataComponentPatch);
        } else {
            iconNMS = CraftItemStack.asNMSCopy(icon);
        }
        
        // Create title component with registry access
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        Component titleComponent = Objects.requireNonNull(
            Component.Serializer.fromJson(title, craftServer.getServer().registryAccess()));
        Component subtitleComponent = Component.literal(".");
        
        // Create display info
        AdvancementType advancementType = AdvancementType.valueOf(toastType.toString());
        Optional<DisplayInfo> displayInfo = Optional.of(new DisplayInfo(iconNMS, titleComponent, 
            subtitleComponent, Optional.empty(), advancementType, true, false, true));
        
        // Create advancement components
        AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
        
        // Use modern ResourceLocation factory method
        Optional<ResourceLocation> id = Optional.of(ResourceLocation.fromNamespaceAndPath(namespace, path));
        
        // Create criterion with full constructor
        Criterion<ImpossibleTrigger.TriggerInstance> criterion = new Criterion<>(
            new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance());
        
        HashMap<String, Criterion<?>> criteria = new HashMap<>();
        criteria.put("impossible", criterion);
        
        // Create advancement requirements
        List<List<String>> requirements = Collections.singletonList(
            Collections.singletonList("impossible"));
        AdvancementRequirements advancementRequirements = new AdvancementRequirements(requirements);
        
        // Create advancement
        Advancement advancement = new Advancement(Optional.empty(), displayInfo, advancementRewards, 
            criteria, advancementRequirements, false);
        
        // Create advancement progress
        AdvancementProgress advancementProgress = new AdvancementProgress();
        advancementProgress.update(advancementRequirements);
        advancementProgress.getCriterion("impossible").grant();
        
        // Setup advancement map
        Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
        advancementsToGrant.put(id.get(), advancementProgress);
        
        // Send packets using AdvancementHolder
        List<AdvancementHolder> advancementList = Collections.singletonList(
            new AdvancementHolder(id.get(), advancement));
        
        ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false, 
            advancementList, new HashSet<>(), advancementsToGrant);
        serverPlayer.connection.send(packet1);
        
        ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, 
            new ArrayList<>(), Collections.singleton(id.get()), new HashMap<>());
        serverPlayer.connection.send(packet2);
    }
}