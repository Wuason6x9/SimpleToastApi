package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
        try {
            // Get server player via reflection
            Object serverPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
                .getMethod("getHandle")
                .invoke(player);
            
            // Create NMS ItemStack
            Object iconNMS = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                .getMethod("asNMSCopy", ItemStack.class)
                .invoke(null, icon != null ? icon : new ItemStack(Material.PAPER));
            
            // Create title component with registry access (required for v1_20+)
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            Object registryAccess = minecraftServer.getClass().getMethod("registryAccess").invoke(minecraftServer);
            
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> serializerClass = Class.forName("net.minecraft.network.chat.Component$Serializer");
            Object titleComponent = serializerClass.getMethod("fromJson", String.class, 
                Class.forName("net.minecraft.core.RegistryAccess")).invoke(null, title, registryAccess);
            Object subtitleComponent = componentClass.getMethod("literal", String.class).invoke(null, ".");
            
            // Create display info - use AdvancementType with registry for these versions
            Class<?> advancementTypeClass = Class.forName("net.minecraft.advancements.AdvancementType");
            Object advancementType = advancementTypeClass.getMethod("valueOf", String.class).invoke(null, toastType.toString());
            
            Class<?> displayInfoClass = Class.forName("net.minecraft.advancements.DisplayInfo");
            Constructor<?> displayInfoConstructor = displayInfoClass.getConstructor(
                Class.forName("net.minecraft.world.item.ItemStack"),
                componentClass, componentClass, Optional.class, advancementTypeClass,
                boolean.class, boolean.class, boolean.class);
            Object displayInfo = displayInfoConstructor.newInstance(iconNMS, titleComponent, subtitleComponent,
                Optional.empty(), advancementType, true, false, true);
            
            // Create advancement components
            Object advancementRewards = Class.forName("net.minecraft.advancements.AdvancementRewards")
                .getField("EMPTY").get(null);
            
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
            Object id = resourceLocationClass.getConstructor(String.class, String.class)
                .newInstance(namespace, path);
            
            // Create impossible criterion with version-specific logic
            Object criterion;
            HashMap<String, Object> criteria = new HashMap<>();
            
            Class<?> impossibleTriggerClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger");
            Class<?> triggerInstanceClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance");
            
            if (hasExtraAdvancementParam) {
                // v1_20+ - uses new constructor signature
                Object triggerInstance = triggerInstanceClass.getDeclaredConstructor().newInstance();
                Object impossibleTrigger = impossibleTriggerClass.getDeclaredConstructor().newInstance();
                
                Class<?> criterionClass = Class.forName("net.minecraft.advancements.Criterion");
                Constructor<?> criterionConstructor = criterionClass.getConstructor(impossibleTriggerClass, triggerInstanceClass);
                criterion = criterionConstructor.newInstance(impossibleTrigger, triggerInstance);
            } else {
                // Older versions
                Object triggerInstance = triggerInstanceClass.getDeclaredConstructor().newInstance();
                Class<?> criterionClass = Class.forName("net.minecraft.advancements.Criterion");
                Constructor<?> criterionConstructor = criterionClass.getConstructor(triggerInstanceClass);
                criterion = criterionConstructor.newInstance(triggerInstance);
            }
            
            criteria.put("impossible", criterion);
            
            // Create advancement
            Object advancement;
            Class<?> advancementClass = Class.forName("net.minecraft.advancements.Advancement");
            
            if (usesAdvancementRequirements) {
                // v1_20_R3+ - uses AdvancementRequirements
                Class<?> requirementsClass = Class.forName("net.minecraft.advancements.AdvancementRequirements");
                List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
                Object requirements = requirementsClass.getConstructor(List.class).newInstance(requirementsList);
                
                Constructor<?> advancementConstructor = advancementClass.getConstructor(
                    Optional.class, Optional.class, Class.forName("net.minecraft.advancements.AdvancementRewards"),
                    Map.class, requirementsClass, boolean.class);
                advancement = advancementConstructor.newInstance(
                    Optional.empty(), Optional.of(displayInfo), advancementRewards, criteria, requirements, false);
            } else {
                // Older versions use String[][]
                String[][] requirements = {{"impossible"}};
                
                if (hasExtraAdvancementParam) {
                    Constructor<?> advancementConstructor = advancementClass.getConstructor(
                        resourceLocationClass, advancementClass, displayInfoClass,
                        Class.forName("net.minecraft.advancements.AdvancementRewards"),
                        Map.class, String[][].class, boolean.class);
                    advancement = advancementConstructor.newInstance(id, null, displayInfo, 
                        advancementRewards, criteria, requirements, false);
                } else {
                    Constructor<?> advancementConstructor = advancementClass.getConstructor(
                        resourceLocationClass, advancementClass, displayInfoClass,
                        Class.forName("net.minecraft.advancements.AdvancementRewards"),
                        Map.class, String[][].class);
                    advancement = advancementConstructor.newInstance(id, null, displayInfo, 
                        advancementRewards, criteria, requirements);
                }
            }
            
            // Create and setup advancement progress
            Class<?> advancementProgressClass = Class.forName("net.minecraft.advancements.AdvancementProgress");
            Object advancementProgress = advancementProgressClass.getDeclaredConstructor().newInstance();
            
            if (usesAdvancementRequirements) {
                Class<?> requirementsClass = Class.forName("net.minecraft.advancements.AdvancementRequirements");
                List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
                Object requirements = requirementsClass.getConstructor(List.class).newInstance(requirementsList);
                advancementProgressClass.getMethod("update", requirementsClass)
                    .invoke(advancementProgress, requirements);
            } else {
                String[][] requirements = {{"impossible"}};
                advancementProgressClass.getMethod("update", Map.class, String[][].class)
                    .invoke(advancementProgress, criteria, requirements);
            }
            
            Object criterionProgress = advancementProgressClass.getMethod("getCriterion", String.class)
                .invoke(advancementProgress, "impossible");
            criterionProgress.getClass().getMethod("grant").invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets
            List<Object> advancementList = new ArrayList<>();
            if (usesAdvancementHolder) {
                // v1_20_R3+ uses AdvancementHolder
                Class<?> advancementHolderClass = Class.forName("net.minecraft.advancements.AdvancementHolder");
                Object advancementHolder = advancementHolderClass.getConstructor(resourceLocationClass, advancementClass)
                    .newInstance(id, advancement);
                advancementList.add(advancementHolder);
            } else {
                advancementList.add(advancement);
            }
            
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            Constructor<?> packetConstructor = packetClass.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            
            Object packet1 = packetConstructor.newInstance(false, advancementList, 
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