package dev.wuason.toastapi.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorsTest {

    @Test
    @DisplayName("Null input returns empty text component")
    void nullInput() {
        assertEquals("{\"text\":\"\"}", Colors.toJsonTextComponent(null));
    }

    @Test
    @DisplayName("Empty input returns empty text component")
    void emptyInput() {
        assertEquals("{\"text\":\"\"}", Colors.toJsonTextComponent(""));
    }

    @Test
    @DisplayName("Plain text without codes")
    void plainText() {
        String input = "Hello World";
        String expected = "{\"text\":\"Hello World\"}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Escaping quotes, backslash and newline")
    void escaping() {
        String input = "Hello \"World\" \\ test\nNext"; // contains quote, backslash, newline
        String expected = "{\"text\":\"Hello \\\"World\\\" \\\\ test\\nNext\"}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Single legacy color code")
    void singleColor() {
        String input = "&aHello";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Hello\",\"color\":\"green\"}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Two colors with space between")
    void twoColors() {
        String input = "&aHello &bWorld";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Hello \",\"color\":\"green\"},{\"text\":\"World\",\"color\":\"aqua\"}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Chained style codes (&l&o) on same segment")
    void chainedStyles() {
        String input = "&l&oBold";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Bold\",\"bold\":true,\"italic\":true}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Color then style (bold)")
    void colorThenStyle() {
        String input = "&a&lGreenBold";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"GreenBold\",\"color\":\"green\",\"bold\":true}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Hex color lowercase applied")
    void hexColor() {
        String input = "&#12ABefTest";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Test\",\"color\":\"#12abef\"}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Invalid hex treated literally")
    void invalidHex() {
        String input = "&#12AGZZTest"; // G and Z invalid
        String expected = "{\"text\":\"&#12AGZZTest\"}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Reset after color+bold")
    void reset() {
        String input = "&a&lTest&rReset";
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Test\",\"color\":\"green\",\"bold\":true},{\"text\":\"Reset\"}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Multiple style flags accumulation")
    void multipleStyles() {
        String input = "&mStrike&nUnder"; // strikethrough then underline
        // actual order in output: underlined then strikethrough
        String expected = "{\"text\":\"\",\"extra\":[{\"text\":\"Strike\",\"strikethrough\":true},{\"text\":\"Under\",\"underlined\":true,\"strikethrough\":true}]}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }

    @Test
    @DisplayName("Unknown code treated literally")
    void unknownCode() {
        String input = "&zHello"; // z not recognized
        String expected = "{\"text\":\"&zHello\"}";
        assertEquals(expected, Colors.toJsonTextComponent(input));
    }
}
