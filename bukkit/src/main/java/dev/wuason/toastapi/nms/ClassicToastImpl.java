package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Toast implementation for Minecraft versions 1.17-1.19.
 * Handles v1_17_R1, v1_18_R1, v1_18_R2, v1_19_R1, v1_19_R2, v1_19_R3.
 * 
 * These versions share the same API structure and don't use versioned CraftBukkit packages.
 */
public class ClassicToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    
    public ClassicToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        try {
            // Get server player via reflection
            Object serverPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
                .getMethod("getHandle")
                .invoke(player);
            
            // Create NMS ItemStack
            Object iconNMS = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                .getMethod("asNMSCopy", ItemStack.class)
                .invoke(null, icon != null ? icon : new ItemStack(Material.AIR));
            
            // Create title component - v1_17-v1_19 uses simple fromJson
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> serializerClass = Class.forName("net.minecraft.network.chat.Component$Serializer");
            Object titleComponent = serializerClass.getMethod("fromJson", String.class).invoke(null, title);
            Object subtitleComponent = componentClass.getMethod("literal", String.class).invoke(null, ".");
            
            // Create display info - use FrameType for these versions
            Class<?> frameTypeClass = Class.forName("net.minecraft.advancements.FrameType");
            Object frameType = frameTypeClass.getMethod("valueOf", String.class).invoke(null, toastType.toString());
            
            Class<?> displayInfoClass = Class.forName("net.minecraft.advancements.DisplayInfo");
            Constructor<?> displayInfoConstructor = displayInfoClass.getConstructor(
                Class.forName("net.minecraft.world.item.ItemStack"),
                componentClass, componentClass, Optional.class, frameTypeClass,
                boolean.class, boolean.class, boolean.class);
            Object displayInfo = displayInfoConstructor.newInstance(iconNMS, titleComponent, subtitleComponent,
                Optional.empty(), frameType, true, false, true);
            
            // Create advancement components
            Object advancementRewards = Class.forName("net.minecraft.advancements.AdvancementRewards")
                .getField("EMPTY").get(null);
            
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            Object id = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance(namespace, path);
            
            // Create impossible criterion
            Class<?> impossibleTriggerClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger");
            Class<?> triggerInstanceClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance");
            Constructor<?> triggerInstanceConstructor = triggerInstanceClass.getDeclaredConstructor();
            triggerInstanceConstructor.setAccessible(true);
            Object triggerInstance = triggerInstanceConstructor.newInstance();
            
            Class<?> criterionClass = Class.forName("net.minecraft.advancements.Criterion");
            Constructor<?> criterionConstructor = criterionClass.getConstructor(triggerInstanceClass);
            Object criterion = criterionConstructor.newInstance(triggerInstance);
            
            HashMap<String, Object> criteria = new HashMap<>();
            criteria.put("impossible", criterion);
            
            // Create advancement - older versions use String[][] for requirements
            String[][] requirements = {{"impossible"}};
            Class<?> advancementClass = Class.forName("net.minecraft.advancements.Advancement");
            Constructor<?> advancementConstructor = advancementClass.getConstructor(
                resourceLocationClass, advancementClass, displayInfoClass,
                Class.forName("net.minecraft.advancements.AdvancementRewards"),
                Map.class, String[][].class);
            Object advancement = advancementConstructor.newInstance(id, null, displayInfo, 
                advancementRewards, criteria, requirements);
            
            // Create and setup advancement progress
            Class<?> advancementProgressClass = Class.forName("net.minecraft.advancements.AdvancementProgress");
            Object advancementProgress = advancementProgressClass.getDeclaredConstructor().newInstance();
            
            advancementProgressClass.getMethod("update", Map.class, String[][].class)
                .invoke(advancementProgress, criteria, requirements);
            Object criterionProgress = advancementProgressClass.getMethod("getCriterion", String.class)
                .invoke(advancementProgress, "impossible");
            criterionProgress.getClass().getMethod("grant").invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            Constructor<?> packetConstructor = packetClass.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            
            Object packet1 = packetConstructor.newInstance(false, Collections.singletonList(advancement), 
                new HashSet<>(), advancementsToGrant);
            
            Object connection = serverPlayer.getClass().getField("connection").get(serverPlayer);
            connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"))
                .invoke(connection, packet1);
            
            Object packet2 = packetConstructor.newInstance(false, new ArrayList<>(), 
                Collections.singleton(id), new HashMap<>());
            connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"))
                .invoke(connection, packet2);
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to send toast for version " + nmsVersion, e);
        }
    }
}