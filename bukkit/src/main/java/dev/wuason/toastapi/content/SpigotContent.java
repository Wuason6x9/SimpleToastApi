package dev.wuason.toastapi.content;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.ChatColor;

public class SpigotContent implements IContent {
    private final String legacyText;

    public SpigotContent(String legacyText) {
        this.legacyText = legacyText;
    }

    @Override
    public String getContent() {
        String withSection = ChatColor.translateAlternateColorCodes('&', legacyText);
        BaseComponent[] components = TextComponent.fromLegacyText(withSection);
        return ComponentSerializer.toString(components);
    }
}
