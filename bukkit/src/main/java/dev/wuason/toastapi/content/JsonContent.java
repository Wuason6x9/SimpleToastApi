package dev.wuason.toastapi.content;

public class JsonContent implements IContent {
    private final String json;

    public JsonContent(String json) {
        this.json = json;
    }

    @Override
    public String getContent() {
        return json;
    }
}
