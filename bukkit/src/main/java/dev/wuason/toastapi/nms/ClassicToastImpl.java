package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Toast implementation for Minecraft versions 1.17-1.19.
 * Handles v1_17_R1, v1_18_R1, v1_18_R2, v1_19_R1, v1_19_R2, v1_19_R3.
 */
public class ClassicToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    private final String versionString;
    
    // Cached reflection objects
    private static Class<?> craftPlayerClass;
    private static Class<?> craftItemStackClass;
    private static Method asNMSCopyMethod;
    private static Method getHandleMethod;
    
    public ClassicToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
        this.versionString = nmsVersion.getVersionName();
        initializeReflection();
    }
    
    private void initializeReflection() {
        try {
            // Cache commonly used classes and methods
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".entity.CraftPlayer");
            craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + versionString + ".inventory.CraftItemStack");
            
            asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize reflection for version " + versionString, e);
        }
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        try {
            // Get server player using cached reflection
            ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod.invoke(player);
            
            // Create NMS ItemStack
            net.minecraft.world.item.ItemStack iconNMS = (net.minecraft.world.item.ItemStack) 
                asNMSCopyMethod.invoke(null, icon != null ? icon : new ItemStack(Material.AIR));
            
            // Create title component - v1_17-v1_19 uses simple fromJson
            Component titleComponent = Objects.requireNonNull(Component.Serializer.fromJson(title));
            Component subtitleComponent = Component.literal(".");
            
            // Create display info - use FrameType for these versions
            FrameType frameType = FrameType.valueOf(toastType.toString());
            DisplayInfo displayInfo = new DisplayInfo(iconNMS, titleComponent, subtitleComponent, 
                Optional.empty(), frameType, true, false, true);
            
            // Create advancement components
            AdvancementRewards advancementRewards = AdvancementRewards.EMPTY;
            ResourceLocation id = new ResourceLocation(namespace, path);
            
            // Create impossible criterion
            Criterion<ImpossibleTrigger.TriggerInstance> criterion = new Criterion<>(new ImpossibleTrigger.TriggerInstance());
            HashMap<String, Criterion<?>> criteria = new HashMap<>();
            criteria.put("impossible", criterion);
            
            // Create advancement - older versions use String[][] for requirements
            String[][] requirements = {{"impossible"}};
            Advancement advancement = new Advancement(id, null, displayInfo, advancementRewards, criteria, requirements);
            
            // Create and setup advancement progress
            AdvancementProgress advancementProgress = new AdvancementProgress();
            advancementProgress.update(criteria, requirements);
            advancementProgress.getCriterion("impossible").grant();
            
            // Setup advancement map
            Map<ResourceLocation, AdvancementProgress> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets
            ClientboundUpdateAdvancementsPacket packet1 = new ClientboundUpdateAdvancementsPacket(false, 
                Collections.singletonList(advancement), new HashSet<>(), advancementsToGrant);
            serverPlayer.connection.send(packet1);
            
            ClientboundUpdateAdvancementsPacket packet2 = new ClientboundUpdateAdvancementsPacket(false, 
                new ArrayList<>(), Collections.singleton(id), new HashMap<>());
            serverPlayer.connection.send(packet2);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send toast for version " + versionString, e);
        }
    }
}