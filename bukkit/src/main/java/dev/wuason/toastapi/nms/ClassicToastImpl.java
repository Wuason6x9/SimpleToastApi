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
 * Uses static caching for reflection to optimize performance.
 */
public class ClassicToastImpl implements IToastWrapper {

    private final EMinecraftVersion.NMSVersion nmsVersion;
    
    // Static cached reflection objects for performance
    private static final Class<?> CRAFT_PLAYER_CLASS;
    private static final Class<?> CRAFT_ITEM_STACK_CLASS;
    private static final Class<?> COMPONENT_CLASS;
    private static final Class<?> COMPONENT_SERIALIZER_CLASS;
    private static final Class<?> FRAME_TYPE_CLASS;
    private static final Class<?> DISPLAY_INFO_CLASS;
    private static final Class<?> ADVANCEMENT_REWARDS_CLASS;
    private static final Class<?> RESOURCE_LOCATION_CLASS;
    private static final Class<?> IMPOSSIBLE_TRIGGER_CLASS;
    private static final Class<?> TRIGGER_INSTANCE_CLASS;
    private static final Class<?> CRITERION_CLASS;
    private static final Class<?> ADVANCEMENT_CLASS;
    private static final Class<?> ADVANCEMENT_PROGRESS_CLASS;
    private static final Class<?> PACKET_CLASS;
    
    private static final Method GET_HANDLE_METHOD;
    private static final Method AS_NMS_COPY_METHOD;
    private static final Method FROM_JSON_METHOD;
    private static final Method LITERAL_METHOD;
    private static final Method VALUE_OF_FRAME_TYPE_METHOD;
    private static final Method UPDATE_PROGRESS_METHOD;
    private static final Method GET_CRITERION_METHOD;
    private static final Method GRANT_METHOD;
    private static final Method SEND_METHOD;
    
    private static final Constructor<?> DISPLAY_INFO_CONSTRUCTOR;
    private static final Constructor<?> RESOURCE_LOCATION_CONSTRUCTOR;
    private static final Constructor<?> TRIGGER_INSTANCE_CONSTRUCTOR;
    private static final Constructor<?> CRITERION_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_CONSTRUCTOR;
    private static final Constructor<?> ADVANCEMENT_PROGRESS_CONSTRUCTOR;
    private static final Constructor<?> PACKET_CONSTRUCTOR;
    
    private static final Field ADVANCEMENT_REWARDS_EMPTY_FIELD;
    private static final Field CONNECTION_FIELD;
    
    static {
        try {
            // Initialize classes
            CRAFT_PLAYER_CLASS = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            CRAFT_ITEM_STACK_CLASS = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            COMPONENT_CLASS = Class.forName("net.minecraft.network.chat.Component");
            COMPONENT_SERIALIZER_CLASS = Class.forName("net.minecraft.network.chat.Component$Serializer");
            FRAME_TYPE_CLASS = Class.forName("net.minecraft.advancements.FrameType");
            DISPLAY_INFO_CLASS = Class.forName("net.minecraft.advancements.DisplayInfo");
            ADVANCEMENT_REWARDS_CLASS = Class.forName("net.minecraft.advancements.AdvancementRewards");
            RESOURCE_LOCATION_CLASS = Class.forName("net.minecraft.resources.ResourceLocation");
            IMPOSSIBLE_TRIGGER_CLASS = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger");
            TRIGGER_INSTANCE_CLASS = Class.forName("net.minecraft.advancements.critereon.ImpossibleTrigger$TriggerInstance");
            CRITERION_CLASS = Class.forName("net.minecraft.advancements.Criterion");
            ADVANCEMENT_CLASS = Class.forName("net.minecraft.advancements.Advancement");
            ADVANCEMENT_PROGRESS_CLASS = Class.forName("net.minecraft.advancements.AdvancementProgress");
            PACKET_CLASS = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket");
            
            // Initialize methods
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            AS_NMS_COPY_METHOD = CRAFT_ITEM_STACK_CLASS.getMethod("asNMSCopy", ItemStack.class);
            FROM_JSON_METHOD = COMPONENT_SERIALIZER_CLASS.getMethod("fromJson", String.class);
            LITERAL_METHOD = COMPONENT_CLASS.getMethod("literal", String.class);
            VALUE_OF_FRAME_TYPE_METHOD = FRAME_TYPE_CLASS.getMethod("valueOf", String.class);
            UPDATE_PROGRESS_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("update", Map.class, String[][].class);
            GET_CRITERION_METHOD = ADVANCEMENT_PROGRESS_CLASS.getMethod("getCriterion", String.class);
            GRANT_METHOD = Class.forName("net.minecraft.advancements.CriterionProgress").getMethod("grant");
            SEND_METHOD = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl").getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
            
            // Initialize constructors
            DISPLAY_INFO_CONSTRUCTOR = DISPLAY_INFO_CLASS.getConstructor(
                Class.forName("net.minecraft.world.item.ItemStack"),
                COMPONENT_CLASS, COMPONENT_CLASS, Optional.class, FRAME_TYPE_CLASS,
                boolean.class, boolean.class, boolean.class);
            RESOURCE_LOCATION_CONSTRUCTOR = RESOURCE_LOCATION_CLASS.getConstructor(String.class, String.class);
            TRIGGER_INSTANCE_CONSTRUCTOR = TRIGGER_INSTANCE_CLASS.getDeclaredConstructor();
            TRIGGER_INSTANCE_CONSTRUCTOR.setAccessible(true);
            CRITERION_CONSTRUCTOR = CRITERION_CLASS.getConstructor(TRIGGER_INSTANCE_CLASS);
            ADVANCEMENT_CONSTRUCTOR = ADVANCEMENT_CLASS.getConstructor(
                RESOURCE_LOCATION_CLASS, ADVANCEMENT_CLASS, DISPLAY_INFO_CLASS,
                ADVANCEMENT_REWARDS_CLASS, Map.class, String[][].class);
            ADVANCEMENT_PROGRESS_CONSTRUCTOR = ADVANCEMENT_PROGRESS_CLASS.getDeclaredConstructor();
            PACKET_CONSTRUCTOR = PACKET_CLASS.getConstructor(boolean.class, Collection.class, Set.class, Map.class);
            
            // Initialize fields
            ADVANCEMENT_REWARDS_EMPTY_FIELD = ADVANCEMENT_REWARDS_CLASS.getField("EMPTY");
            CONNECTION_FIELD = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ClassicToastImpl reflection cache", e);
        }
    }
    
    public ClassicToastImpl(EMinecraftVersion.NMSVersion nmsVersion) {
        this.nmsVersion = nmsVersion;
    }

    @Override
    public void sendToast(ItemStack icon, Player player, String title, EToastType toastType, String namespace, String path) {
        try {
            // Get server player via cached reflection
            Object serverPlayer = GET_HANDLE_METHOD.invoke(player);
            
            // Create NMS ItemStack using cached reflection
            Object iconNMS = AS_NMS_COPY_METHOD.invoke(null, icon != null ? icon : new ItemStack(Material.AIR));
            
            // Create title component - v1_17-v1_19 uses simple fromJson
            Object titleComponent = FROM_JSON_METHOD.invoke(null, title);
            Object subtitleComponent = LITERAL_METHOD.invoke(null, ".");
            
            // Create display info - use FrameType for these versions
            Object frameType = VALUE_OF_FRAME_TYPE_METHOD.invoke(null, toastType.toString());
            Object displayInfo = DISPLAY_INFO_CONSTRUCTOR.newInstance(iconNMS, titleComponent, subtitleComponent,
                Optional.empty(), frameType, true, false, true);
            
            // Create advancement components using cached reflection
            Object advancementRewards = ADVANCEMENT_REWARDS_EMPTY_FIELD.get(null);
            Object id = RESOURCE_LOCATION_CONSTRUCTOR.newInstance(namespace, path);
            
            // Create impossible criterion using cached constructors
            Object triggerInstance = TRIGGER_INSTANCE_CONSTRUCTOR.newInstance();
            Object criterion = CRITERION_CONSTRUCTOR.newInstance(triggerInstance);
            
            HashMap<String, Object> criteria = new HashMap<>();
            criteria.put("impossible", criterion);
            
            // Create advancement - older versions use String[][] for requirements
            String[][] requirements = {{"impossible"}};
            Object advancement = ADVANCEMENT_CONSTRUCTOR.newInstance(id, null, displayInfo, 
                advancementRewards, criteria, requirements);
            
            // Create and setup advancement progress using cached methods
            Object advancementProgress = ADVANCEMENT_PROGRESS_CONSTRUCTOR.newInstance();
            UPDATE_PROGRESS_METHOD.invoke(advancementProgress, criteria, requirements);
            Object criterionProgress = GET_CRITERION_METHOD.invoke(advancementProgress, "impossible");
            GRANT_METHOD.invoke(criterionProgress);
            
            // Setup advancement map
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(id, advancementProgress);
            
            // Send packets using cached constructors and methods
            Object packet1 = PACKET_CONSTRUCTOR.newInstance(false, Collections.singletonList(advancement), 
                new HashSet<>(), advancementsToGrant);
            
            Object connection = CONNECTION_FIELD.get(serverPlayer);
            SEND_METHOD.invoke(connection, packet1);
            
            Object packet2 = PACKET_CONSTRUCTOR.newInstance(false, new ArrayList<>(), 
                Collections.singleton(id), new HashMap<>());
            SEND_METHOD.invoke(connection, packet2);
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to send toast for version " + nmsVersion, e);
        }
    }
}