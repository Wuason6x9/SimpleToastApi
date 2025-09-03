package dev.wuason.toastapi.nms;

import dev.wuason.toastapi.utils.EMinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Toast implementation for Minecraft versions 1.21.3+.
 * Handles v1_21_R2, v1_21_R3, v1_21_R4, v1_21_R5 and future versions.
 * Uses static caching for reflection to optimize performance.
 */
public class NewestToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    
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
    private static final Class<?> DATA_COMPONENTS_CLASS;
    private static final Class<?> DATA_COMPONENT_PATCH_CLASS;
    private static final Class<?> PACKET_CLASS;
    
    private static final Method GET_HANDLE_METHOD;
    private static final Method AS_NMS_COPY_METHOD;
    private static final Method GET_SERVER_METHOD;
    private static final Method REGISTRY_ACCESS_METHOD;
    private static final Method FROM_JSON_WITH_REGISTRY_METHOD;
    private static final Method LITERAL_METHOD;
    private static final Method VALUE_OF_ADVANCEMENT_TYPE_METHOD;
    private static final Method UPDATE_PROGRESS_METHOD;
    private static final Method GET_CRITERION_METHOD;
    private static final Method GRANT_METHOD;
    private static final Method SEND_METHOD;
    private static final Method PATCH_BUILDER_METHOD;
    private static final Method PATCH_SET_METHOD;
    private static final Method PATCH_BUILD_METHOD;
    private static final Method APPLY_COMPONENTS_METHOD;
    
    private static final Constructor<?> DISPLAY_INFO_CONSTRUCTOR;
    private static final Constructor<?> RESOURCE_LOCATION_CONSTRUCTOR;
    private static final Constructor<?> TRIGGER_INSTANCE_CONSTRUCTOR;
    private static final Constructor<?> IMPOSSIBLE_TRIGGER_CONSTRUCTOR;
    private static final Constructor<?> CRITERION_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_PROGRESS_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_HOLDER_CONSTRUCTOR;
    private static final Constructor<?> PACKET_CONSTRUCTOR;
    
    private static final Object ADVANCEMENT_REWARDS_EMPTY;
    private static final Object HIDE_TOOLTIP_COMPONENT;
    
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
            DATA_COMPONENTS_CLASS = Class.forName("net.minecraft.core.component.DataComponents");
            DATA_COMPONENT_PATCH_CLASS = Class.forName("net.minecraft.core.component.DataComponentPatch");
            PACKET_CLASS = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            
            // Initialize methods
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            AS_NMS_COPY_METHOD = CRAFT_ITEM_STACK_CLASS.getMethod("asNMSCopy", ItemStack.class);
            GET_SERVER_METHOD = Class.forName("org.bukkit.craftbukkit.CraftServer").getMethod("getServer");
            REGISTRY_ACCESS_METHOD = Class.forName("net.minecraft.server.MinecraftServer").getMethod("registryAccess");
            FROM_JSON_WITH_REGISTRY_METHOD = COMPONENT_SERIALIZER_CLASS.getMethod("fromJson", String.class, Class.forName("net.minecraft.core.RegistryAccess"));
            LITERAL_METHOD = COMPONENT_CLASS.getMethod("literal", String.class);
            VALUE_OF_ADVANCEMENT_TYPE_METHOD = ADVANCEMENT_TYPE_CLASS.getMethod("valueOf", String.class);
            UPDATE_PROGRESS_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("update", ADVANCEMENT_REQUIREMENTS_CLASS);
            GET_CRITERION_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("getCriterion", String.class);
            GRANT_METHOD = Class.forName("net.minecraft.advancements.CriterionProgress").getMethod("grant");
            SEND_METHOD = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
            PATCH_BUILDER_METHOD = DATA_COMPONENT_PATCH_CLASS.getMethod("builder");
            PATCH_SET_METHOD = Class.forName("net.minecraft.core.component.DataComponentPatch$Builder").getMethod("set", Class.forName("net.minecraft.core.component.DataComponentType"), Object.class);
            PATCH_BUILD_METHOD = Class.forName("net.minecraft.core.component.DataComponentPatch$Builder").getMethod("build");
            APPLY_COMPONENTS_METHOD = Class.forName("net.minecraft.world.item.ItemStack").getMethod("applyComponents", DATA_COMPONENT_PATCH_CLASS);
            
            // Initialize constructors
            DISPLAY_INFO_CONSTRUCTOR = DISPLAY_INFO_CLASS.getConstructor(
                Class.forName("net.minecraft.world.item.ItemStack"),
                COMPONENT_CLASS, COMPONENT_CLASS, Optional.class, ADVANCEMENT_TYPE_CLASS,
                boolean.class, boolean.class, boolean.class);
            RESOURCE_LOCATION_CONSTRUCTOR = RESOURCE_LOCATION_CLASS.getConstructor(String.class, String.class);
            TRIGGER_INSTANCE_CONSTRUCTOR = TRIGGER_INSTANCE_CLASS.getDeclaredConstructor();
            IMPOSSIBLE_TRIGGER_CONSTRUCTOR = IMPOSSIBLE_TRIGGER_CLASS.getDeclaredConstructor();
            CRITERION_CONSTRUCTOR = CRITERION_CLASS.getConstructor(IMPOSSIBLE_TRIGGER_CLASS, TRIGGER_INSTANCE_CLASS);
            ADVANCEMENT_CONSTRUCTOR = ADVANCEMENT_CLASS.getConstructor(
                Optional.class, Optional.class, ADVANCEMENT_REWARDS_CLASS,
                Map.class, ADVANCEMENT_REQUIREMENTS_CLASS, boolean.class);
            ADVANCEMENT_PROGRESS_CONSTRUCTOR = ADVANCEMENT_PROGRESS_CLASS.getDeclaredConstructor();
            ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR = ADVANCEMENT_REQUIREMENTS_CLASS.getConstructor(List.class);
            ADVANCEMENT_HOLDER_CONSTRUCTOR = ADVANCEMENT_HOLDER_CLASS.getConstructor(RESOURCE_LOCATION_CLASS, ADVANCEMENT_CLASS);
            PACKET_CONSTRUCTOR = PACKET_CLASS.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            
            // Initialize static objects
            ADVANCEMENT_REWARDS_EMPTY = ADVANCEMENT_REWARDS_CLASS.getField("EMPTY").get(null);
            HIDE_TOOLTIP_COMPONENT = DATA_COMPONENTS_CLASS.getField("HIDE_TOOLTIP").get(null);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NewestToastImpl reflection cache", e);
        }
    }
    
    public NewestToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        try {
            // Get server player via cached reflection
            Object serverPlayer = GET_HANDLE_METHOD.invoke(player);
            
            // Create NMS ItemStack with data components for invisible icons using cached reflection
            Object iconNMS;
            if (icon != null) {
                iconNMS = AS_NMS_COPY_METHOD.invoke(null, icon);
            } else {
                // Create invisible icon using data components (v1_21.3+ feature)
                Object paperStack = AS_NMS_COPY_METHOD.invoke(null, new ItemStack(Material.PAPER));
                
                Object builderInstance = PATCH_BUILDER_METHOD.invoke(null);
                PATCH_SET_METHOD.invoke(builderInstance, HIDE_TOOLTIP_COMPONENT, true);
                Object patch = PATCH_BUILD_METHOD.invoke(builderInstance);
                
                iconNMS = APPLY_COMPONENTS_METHOD.invoke(paperStack, patch);
            }
            
            // Create title component with registry access using cached methods
            Object craftServer = Bukkit.getServer();
            Object minecraftServer = GET_SERVER_METHOD.invoke(craftServer);
            Object registryAccess = REGISTRY_ACCESS_METHOD.invoke(minecraftServer);
            
            Object titleComponent = FROM_JSON_WITH_REGISTRY_METHOD.invoke(null, title, registryAccess);
            Object subtitleComponent = LITERAL_METHOD.invoke(null, ".");
            
            // Create display info using AdvancementType with cached constructors
            Object advancementType = VALUE_OF_ADVANCEMENT_TYPE_METHOD.invoke(null, toastType.toString());
            Object displayInfo = DISPLAY_INFO_CONSTRUCTOR.newInstance(iconNMS, titleComponent, subtitleComponent,
                Optional.empty(), advancementType, true, false, true);
            
            // Create advancement components using cached objects
            Object id = RESOURCE_LOCATION_CONSTRUCTOR.newInstance(namespace, path);
            
            // Create impossible criterion using cached constructors
            Object triggerInstance = TRIGGER_INSTANCE_CONSTRUCTOR.newInstance();
            Object impossibleTrigger = IMPOSSIBLE_TRIGGER_CONSTRUCTOR.newInstance();
            Object criterion = CRITERION_CONSTRUCTOR.newInstance(impossibleTrigger, triggerInstance);
            
            HashMap<String, Object> criteria = new HashMap<>();
            criteria.put("impossible", criterion);
            
            // Create advancement using AdvancementRequirements (v1_21.3+ structure) with cached constructor
            List<List<String>> requirementsList = Collections.singletonList(Collections.singletonList("impossible"));
            Object requirements = ADVANCEMENT_REQUIREMENTS_CONSTRUCTOR.newInstance(requirementsList);
            
            Object advancement = ADVANCEMENT_CONSTRUCTOR.newInstance(
                Optional.empty(), Optional.of(displayInfo), ADVANCEMENT_REWARDS_EMPTY, criteria, requirements, false);
            
            // Create and setup advancement progress using cached methods
            Object advancementProgress = ADVANCEMENT_PROGRESS_CONSTRUCTOR.newInstance();
            UPDATE_PROGRESS_METHOD.invoke(advancementProgress, requirements);
            
            Object criterionProgress = GET_CRITERION_METHOD.invoke(advancementProgress, "impossible");
            GRANT_METHOD.invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets using AdvancementHolder (v1_21.3+ structure) with cached constructors
            Object advancementHolder = ADVANCEMENT_HOLDER_CONSTRUCTOR.newInstance(id, advancement);
            
            Object packet1 = PACKET_CONSTRUCTOR.newInstance(false, Collections.singletonList(advancementHolder), 
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