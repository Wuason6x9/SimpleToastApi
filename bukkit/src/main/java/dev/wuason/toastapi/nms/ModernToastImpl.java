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
 * Uses static caching for reflection to optimize performance.
 */
public class ModernToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    private final boolean usesAdvancementRequirements;
    private final boolean usesAdvancementHolder;
    private final boolean hasExtraAdvancementParam;
    
    // Static cached reflection objects for performance
    private static final Class<?> CRAFT_PLAYER_CLASS;
    private static final Class<?> CRAFT_ITEM_STACK_CLASS;
    private static final Class<?> COMPONENT_CLASS;
    private static final Class<?> COMPONENT_SERIALIZER_CLASS;
    private static final Class<?> ADVANCEMENT_TYPE_CLASS;
    private static final Class<?> DISPLAY_INFO_CLASS;
    private static final Class<?> ADVANCEMENT_REWARDS_CLASS;
    private static final Class<?> RESOURCE_LOCATION_CLASS;
    private static final Class<?> IMPOSSIBLE_TRIGGER_CLASS;
    private static final Class<?> TRIGGER_INSTANCE_CLASS;
    private static final Class<?> CRITERION_CLASS;
    private static final Class<?> ADVANCEMENT_CLASS;
    private static final Class<?> ADVANCEMENT_PROGRESS_CLASS;
    private static final Class<?> ADVANCEMENT_REQUIREMENTS_CLASS;
    private static final Class<?> ADVANCEMENT_HOLDER_CLASS;
    private static final Class<?> PACKET_CLASS;
    
    private static final Method GET_HANDLE_METHOD;
    private static final Method AS_NMS_COPY_METHOD;
    private static final Method GET_SERVER_METHOD;
    private static final Method REGISTRY_ACCESS_METHOD;
    private static final Method FROM_JSON_WITH_REGISTRY_METHOD;
    private static final Method LITERAL_METHOD;
    private static final Method VALUE_OF_ADVANCEMENT_TYPE_METHOD;
    private static final Method UPDATE_PROGRESS_OLD_METHOD;
    private static final Method UPDATE_PROGRESS_NEW_METHOD;
    private static final Method GET_CRITERION_METHOD;
    private static final Method GRANT_METHOD;
    private static final Method SEND_METHOD;
    
    private static final Constructor<?> DISPLAY_INFO_CONSTRUCTOR;
    private static final Constructor<?> RESOURCE_LOCATION_CONSTRUCTOR;
    private static final Constructor<?> TRIGGER_INSTANCE_CONSTRUCTOR;
    private static final Constructor<?> CRITERION_OLD_CONSTRUCTOR;
    private static final Constructor<?> CRITERION_NEW_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_OLD_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_OLD_WITH_PARAM_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_NEW_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_PROGRESS_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_HOLDER_CONSTRUCTOR;
    private static final Constructor<?> PACKET_CONSTRUCTOR;
    private static final Constructor<?> IMPOSSIBLE_TRIGGER_CONSTRUCTOR;
    
    private static final Object ADVANCEMENT_REWARDS_EMPTY;
    
    static {
        try {
            // Initialize classes
            CRAFT_PLAYER_CLASS = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            CRAFT_ITEM_STACK_CLASS = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            COMPONENT_CLASS = Class.forName("net.minecraft.network.chat.Component");
            COMPONENT_SERIALIZER_CLASS = Class.forName("net.minecraft.network.chat.Component$Serializer");
            ADVANCEMENT_TYPE_CLASS = Class.forName("net.minecraft.advancements.AdvancementType");
            DISPLAY_INFO_CLASS = Class.forName("net.minecraft.advancements.DisplayInfo");
            ADVANCEMENT_REWARDS_CLASS = Class.forName("net.minecraft.advancements.AdvancementRewards");
            RESOURCE_LOCATION_CLASS = Class.forName("net.minecraft.resources.ResourceLocation");
            IMPOSSIBLE_TRIGGER_CLASS = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger");
            TRIGGER_INSTANCE_CLASS = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance");
            CRITERION_CLASS = Class.forName("net.minecraft.advancements.Criterion");
            ADVANCEMENT_CLASS = Class.forName("net.minecraft.advancements.Advancement");
            ADVANCEMENT_PROGRESS_CLASS = Class.forName("net.minecraft.advancements.AdvancementProgress");
            ADVANCEMENT_REQUIREMENTS_CLASS = Class.forName("net.minecraft.advancements.AdvancementRequirements");
            ADVANCEMENT_HOLDER_CLASS = Class.forName("net.minecraft.advancements.AdvancementHolder");
            PACKET_CLASS = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            
            // Initialize methods
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            AS_NMS_COPY_METHOD = CRAFT_ITEM_STACK_CLASS.getMethod("asNMSCopy", ItemStack.class);
            GET_SERVER_METHOD = Class.forName("org.bukkit.craftbukkit.CraftServer").getMethod("getServer");
            REGISTRY_ACCESS_METHOD = Class.forName("net.minecraft.server.MinecraftServer").getMethod("registryAccess");
            FROM_JSON_WITH_REGISTRY_METHOD = COMPONENT_SERIALIZER_CLASS.getMethod("fromJson", String.class, Class.forName("net.minecraft.core.RegistryAccess"));
            LITERAL_METHOD = COMPONENT_CLASS.getMethod("literal", String.class);
            VALUE_OF_ADVANCEMENT_TYPE_METHOD = ADVANCEMENT_TYPE_CLASS.getMethod("valueOf", String.class);
            UPDATE_PROGRESS_OLD_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("update", Map.class, String[][].class);
            UPDATE_PROGRESS_NEW_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("update", ADVANCEMENT_REQUIREMENTS_CLASS);
            GET_CRITERION_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("getCriterion", String.class);
            GRANT_METHOD = Class.forName("net.minecraft.advancements.CriterionProgress").getMethod("grant");
            SEND_METHOD = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
            
            // Initialize constructors
            DISPLAY_INFO_CONSTRUCTOR = DISPLAY_INFO_CLASS.getConstructor(
                Class.forName("net.minecraft.world.item.ItemStack"),
                COMPONENT_CLASS, COMPONENT_CLASS, Optional.class, ADVANCEMENT_TYPE_CLASS,
                boolean.class, boolean.class, boolean.class);
            RESOURCE_LOCATION_CONSTRUCTOR = RESOURCE_LOCATION_CLASS.getConstructor(String.class, String.class);
            TRIGGER_INSTANCE_CONSTRUCTOR = TRIGGER_INSTANCE_CLASS.getDeclaredConstructor();
            CRITERION_OLD_CONSTRUCTOR = CRITERION_CLASS.getConstructor(TRIGGER_INSTANCE_CLASS);
            CRITERION_NEW_CONSTRUCTOR = CRITERION_CLASS.getConstructor(IMPOSSIBLE_TRIGGER_CLASS, TRIGGER_INSTANCE_CLASS);
            ADVANCEMENT_OLD_CONSTRUCTOR = ADVANCEMENT_CLASS.getConstructor(
                RESOURCE_LOCATION_CLASS, ADVANCEMENT_CLASS, DISPLAY_INFO_CLASS,
                ADVANCEMENT_REWARDS_CLASS, Map.class, String[][].class);
            ADVANCEMENT_OLD_WITH_PARAM_CONSTRUCTOR = ADVANCEMENT_CLASS.getConstructor(
                RESOURCE_LOCATION_CLASS, ADVANCEMENT_CLASS, DISPLAY_INFO_CLASS,
                ADVANCEMENT_REWARDS_CLASS, Map.class, String[][].class, boolean.class);
            ADVANCEMENT_NEW_CONSTRUCTOR = ADVANCEMENT_CLASS.getConstructor(
                Optional.class, Optional.class, ADVANCEMENT_REWARDS_CLASS,
                Map.class, ADVANCEMENT_REQUIREMENTS_CLASS, boolean.class);
            ADVANCEMENT_PROGRESS_CONSTRUCTOR = ADVANCEMENT_PROGRESS_CLASS.getDeclaredConstructor();
            ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR = ADVANCEMENT_REQUIREMENTS_CLASS.getConstructor(List.class);
            ADVANCEMENT_HOLDER_CONSTRUCTOR = ADVANCEMENT_HOLDER_CLASS.getConstructor(RESOURCE_LOCATION_CLASS, ADVANCEMENT_CLASS);
            PACKET_CONSTRUCTOR = PACKET_CLASS.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            IMPOSSIBLE_TRIGGER_CONSTRUCTOR = IMPOSSIBLE_TRIGGER_CLASS.getDeclaredConstructor();
            
            // Initialize static objects
            ADVANCEMENT_REWARDS_EMPTY = ADVANCEMENT_REWARDS_CLASS.getField("EMPTY").get(null);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ModernToastImpl reflection cache", e);
        }
    }
    
    public ModernToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
        this.usesAdvancementRequirements = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R3);
        this.usesAdvancementHolder = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R3);
        this.hasExtraAdvancementParam = nmsVersion.isAtLeast(EMinecraftVersion.NMSVersion.V1_20_R1);
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        try {
            // Get server player via cached reflection
            Object serverPlayer = GET_HANDLE_METHOD.invoke(player);
            
            // Create NMS ItemStack using cached reflection
            Object iconNMS = AS_NMS_COPY_METHOD.invoke(null, icon != null ? icon : new ItemStack(Material.PAPER));
            
            // Create title component with registry access (required for v1_20+)
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = GET_SERVER_METHOD.invoke(craftServer);
            Object registryAccess = REGISTRY_ACCESS_METHOD.invoke(minecraftServer);
            
            Object titleComponent = FROM_JSON_WITH_REGISTRY_METHOD.invoke(null, title, registryAccess);
            Object subtitleComponent = LITERAL_METHOD.invoke(null, ".");
            
            // Create display info - use AdvancementType with registry for these versions
            Object advancementType = VALUE_OF_ADVANCEMENT_TYPE_METHOD.invoke(null, toastType.toString());
            Object displayInfo = DISPLAY_INFO_CONSTRUCTOR.newInstance(iconNMS, titleComponent, subtitleComponent,
                Optional.empty(), advancementType, true, false, true);
            
            // Create advancement components using cached objects
            Object id = RESOURCE_LOCATION_CONSTRUCTOR.newInstance(namespace, path);
            
            // Create impossible criterion with version-specific logic using cached constructors
            Object criterion;
            HashMap<String, Object> criteria = new HashMap<>();
            
            if (hasExtraAdvancementParam) {
                // v1_20+ - uses new constructor signature
                Object triggerInstance = TRIGGER_INSTANCE_CONSTRUCTOR.newInstance();
                Object impossibleTrigger = IMPOSSIBLE_TRIGGER_CONSTRUCTOR.newInstance();
                criterion = CRITERION_NEW_CONSTRUCTOR.newInstance(impossibleTrigger, triggerInstance);
            } else {
                // Older versions
                Object triggerInstance = TRIGGER_INSTANCE_CONSTRUCTOR.newInstance();
                criterion = CRITERION_OLD_CONSTRUCTOR.newInstance(triggerInstance);
            }
            
            criteria.put("impossible", criterion);
            
            // Create advancement using cached constructors
            Object advancement;
            
            if (usesAdvancementRequirements) {
                // v1_20_R3+ - uses AdvancementRequirements
                List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
                Object requirements = ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR.newInstance(requirementsList);
                
                advancement = ADVANCEMENT_NEW_CONSTRUCTOR.newInstance(
                    Optional.empty(), Optional.of(displayInfo), ADVANCEMENT_REWARDS_EMPTY, criteria, requirements, false);
            } else {
                // Older versions use String[][]
                String[][] requirements = {{"impossible"}};
                
                if (hasExtraAdvancementParam) {
                    advancement = ADVANCEMENT_OLD_WITH_PARAM_CONSTRUCTOR.newInstance(id, null, displayInfo, 
                        ADVANCEMENT_REWARDS_EMPTY, criteria, requirements, false);
                } else {
                    advancement = ADVANCEMENT_OLD_CONSTRUCTOR.newInstance(id, null, displayInfo, 
                        ADVANCEMENT_REWARDS_EMPTY, criteria, requirements);
                }
            }
            
            // Create and setup advancement progress using cached methods
            Object advancementProgress = ADVANCEMENT_PROGRESS_CONSTRUCTOR.newInstance();
            
            if (usesAdvancementRequirements) {
                List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
                Object requirements = ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR.newInstance(requirementsList);
                UPDATE_PROGRESS_NEW_METHOD.invoke(advancementProgress, requirements);
            } else {
                String[][] requirements = {{"impossible"}};
                UPDATE_PROGRESS_OLD_METHOD.invoke(advancementProgress, criteria, requirements);
            }
            
            Object criterionProgress = GET_CRITERION_METHOD.invoke(advancementProgress, "impossible");
            GRANT_METHOD.invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets using cached constructors
            List<Object> advancementList = new ArrayList<>();
            if (usesAdvancementHolder) {
                // v1_20_R3+ uses AdvancementHolder
                Object advancementHolder = ADVANCEMENT_HOLDER_CONSTRUCTOR.newInstance(id, advancement);
                advancementList.add(advancementHolder);
            } else {
                advancementList.add(advancement);
            }
            
            Object packet1 = PACKET_CONSTRUCTOR.newInstance(false, advancementList, 
                new HashSet<>(), advancementsToGrant);
            
            Object connection = serverPlayer.getClass().getField("connection").get(serverPlayer);
            SEND_METHOD.invoke(connection, packet1);
            
            Object packet2 = PACKET_CONSTRUCTOR.newInstance(false, new ArrayList<>(), 
                Collections.singleton(id), new HashMap<>());
            SEND_METHOD.invoke(connection, packet2);
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to send toast for version " + nmsVersion, e);
        }
    }
}