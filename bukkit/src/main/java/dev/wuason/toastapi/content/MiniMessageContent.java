package dev.wuason.toastapi.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class MiniMessageContent implements IContent {
    private final String miniMessage;

    public MiniMessageContent(String miniMessage) {
        this.miniMessage = miniMessage;
    }

    @Override
    public String getContent() {
        Component component = MiniMessage.miniMessage().deserialize(miniMessage);
        return GsonComponentSerializer.gson().serialize(component);
    }
}
