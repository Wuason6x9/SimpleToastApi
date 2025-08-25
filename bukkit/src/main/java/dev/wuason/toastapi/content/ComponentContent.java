package dev.wuason.toastapi.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class ComponentContent implements IContent {
    private final Component component;

    public ComponentContent(Component component) {
        this.component = component;
    }

    @Override
    public String getContent() {
        return GsonComponentSerializer.gson().serialize(component);
    }
}
