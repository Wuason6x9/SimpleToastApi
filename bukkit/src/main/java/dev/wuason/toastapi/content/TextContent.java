package dev.wuason.toastapi.content;

import dev.wuason.toastapi.utils.Colors;

public class TextContent implements IContent {
    private final String text;

    public TextContent(String text) {
        this.text = text;
    }

    @Override
    public String getContent() {
        return Colors.toJsonTextComponent(text);
    }
}
