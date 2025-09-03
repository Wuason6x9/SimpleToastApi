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
        try {
            // Get server player via reflection
            Object serverPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
                .getMethod("getHandle")
                .invoke(player);
            
            // Create NMS ItemStack with data components for invisible icons
            Object iconNMS;
            if (icon != null) {
                iconNMS = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                    .getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, icon);
            } else {
                // Create invisible icon using data components (v1_21.3+ feature)
                Object paperStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                    .getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, new ItemStack(Material.PAPER));
                
                Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");
                Object hideTooltipComponent = dataComponentsClass.getField("HIDE_TOOLTIP").get(null);
                
                Class<?> dataComponentPatchClass = Class.forName("net.minecraft.core.component.DataComponentPatch");
                Object builderInstance = dataComponentPatchClass.getMethod("builder").invoke(null);
                builderInstance.getClass().getMethod("set", Class.forName("net.minecraft.core.component.DataComponentType"), Object.class)
                    .invoke(builderInstance, hideTooltipComponent, true);
                Object patch = builderInstance.getClass().getMethod("build").invoke(builderInstance);
                
                iconNMS = paperStack.getClass().getMethod("applyComponents", dataComponentPatchClass)
                    .invoke(paperStack, patch);
            }
            
            // Create title component with registry access
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            Object registryAccess = minecraftServer.getClass().getMethod("registryAccess").invoke(minecraftServer);
            
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Class<?> serializerClass = Class.forName("net.minecraft.network.chat.Component$Serializer");
            Object titleComponent = serializerClass.getMethod("fromJson", String.class, 
                Class.forName("net.minecraft.core.RegistryAccess")).invoke(null, title, registryAccess);
            Object subtitleComponent = componentClass.getMethod("literal", String.class).invoke(null, ".");
            
            // Create display info using AdvancementType
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
            
            // Create impossible criterion
            Class<?> impossibleTriggerClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger");
            Class<?> triggerInstanceClass = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance");
            
            Object triggerInstance = triggerInstanceClass.getDeclaredConstructor().newInstance();
            Object impossibleTrigger = impossibleTriggerClass.getDeclaredConstructor().newInstance();
            
            Class<?> criterionClass = Class.forName("net.minecraft.advancements.Criterion");
            Constructor<?> criterionConstructor = criterionClass.getConstructor(impossibleTriggerClass, triggerInstanceClass);
            Object criterion = criterionConstructor.newInstance(impossibleTrigger, triggerInstance);
            
            HashMap<String, Object> criteria = new HashMap<>();
            criteria.put("impossible", criterion);
            
            // Create advancement using AdvancementRequirements (v1_21.3+ structure)
            Class<?> requirementsClass = Class.forName("net.minecraft.advancements.AdvancementRequirements");
            List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
            Object requirements = requirementsClass.getConstructor(List.class).newInstance(requirementsList);
            
            Class<?> advancementClass = Class.forName("net.minecraft.advancements.Advancement");
            Constructor<?> advancementConstructor = advancementClass.getConstructor(
                Optional.class, Optional.class, Class.forName("net.minecraft.advancements.AdvancementRewards"),
                Map.class, requirementsClass, boolean.class);
            Object advancement = advancementConstructor.newInstance(
                Optional.empty(), Optional.of(displayInfo), advancementRewards, criteria, requirements, false);
            
            // Create and setup advancement progress
            Class<?> advancementProgressClass = Class.forName("net.minecraft.advancements.AdvancementProgress");
            Object advancementProgress = advancementProgressClass.getDeclaredConstructor().newInstance();
            
            advancementProgressClass.getMethod("update", requirementsClass)
                .invoke(advancementProgress, requirements);
            
            Object criterionProgress = advancementProgressClass.getMethod("getCriterion", String.class)
                .invoke(advancementProgress, "impossible");
            criterionProgress.getClass().getMethod("grant").invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets using AdvancementHolder (v1_21.3+ structure)
            Class<?> advancementHolderClass = Class.forName("net.minecraft.advancements.AdvancementHolder");
            Object advancementHolder = advancementHolderClass.getConstructor(resourceLocationClass, advancementClass)
                .newInstance(id, advancement);
            
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            Constructor<?> packetConstructor = packetClass.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            
            Object packet1 = packetConstructor.newInstance(false, Collections.singletonList(advancementHolder), 
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