package dev.wuason.toastapi.utils;

import java.util.ArrayList;
import java.util.List;

public final class Colors {
    /**
     * Converts a String with format codes (&x and &#RRGGBB) to a Text Component JSON.
     *
     * Supported rules:
     * - Colors (&0-&9, &a-&f) => named color (black, dark_blue, etc.). Applying a color resets styles.
     * - Hex (&#RRGGBB) => color "#rrggbb" (lowercase). Applying a hex color resets styles.
     * - Styles: &k (obfuscated), &l (bold), &m (strikethrough), &n (underlined), &o (italic)
     * - Reset: &r (resets color and styles)
     *
     * If there are no codes, returns {"text":"..."} with the escaped text.
     * If there are codes, returns {"text":"", "extra":[ { "text":"...", "color":"...", "bold":true, ... }, ... ]}.
     *
     * @param input Input text with format codes using '&' as prefix and hex as '&#RRGGBB'
     * @return Text Component JSON
     */
    public static String toJsonTextComponent(String input) {
        if (input == null || input.isEmpty()) {
            return "{\"text\":\"\"}";
        }

        List<Segment> segments = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        Style current = new Style();
        boolean usedCodes = false;

        final int len = input.length();
        int i = 0;
        while (i < len) {
            char ch = input.charAt(i);

            if (ch == '&') {
                if (i + 1 < len && input.charAt(i + 1) == '#') {
                    if (i + 7 < len) {
                        String hex = input.substring(i + 2, i + 8);
                        if (isHex6(hex)) {
                            if (currentText.length() > 0) {
                                segments.add(new Segment(currentText.toString(), current.copy()));
                                currentText.setLength(0);
                            }
                            current.color = "#" + hex.toLowerCase();
                            current.resetFormatting();
                            usedCodes = true;
                            i += 8;
                            continue;
                        }
                    }
                }

                if (i + 1 < len) {
                    char code = Character.toLowerCase(input.charAt(i + 1));

                    String colorName = mapColor(code);
                    if (colorName != null) {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.color = colorName;
                        current.resetFormatting();
                        usedCodes = true;
                        i += 2;
                        continue;
                    }

                    boolean recognized = true;
                    if (code == 'k') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.obfuscated = true;
                    } else if (code == 'l') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.bold = true;
                    } else if (code == 'm') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.strikethrough = true;
                    } else if (code == 'n') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.underlined = true;
                    } else if (code == 'o') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current.italic = true;
                    } else if (code == 'r') {
                        if (currentText.length() > 0) {
                            segments.add(new Segment(currentText.toString(), current.copy()));
                            currentText.setLength(0);
                        }
                        current = new Style();
                    } else {
                        recognized = false;
                    }

                    if (recognized) {
                        usedCodes = true;
                        i += 2;
                        continue;
                    }
                }

                currentText.append('&');
                i++;
                continue;
            }

            currentText.append(ch);
            i++;
        }

        if (currentText.length() > 0) {
            segments.add(new Segment(currentText.toString(), current.copy()));
        }

        if (!usedCodes) {
            return "{\"text\":\"" + escapeJson(input) + "\"}";
        }

        if (segments.isEmpty()) {
            return "{\"text\":\"\"}";
        }

        StringBuilder out = new StringBuilder();
        out.append("{\"text\":\"\",\"extra\":[");
        for (int idx = 0; idx < segments.size(); idx++) {
            Segment seg = segments.get(idx);
            if (idx > 0) out.append(',');

            out.append('{');
            out.append("\"text\":\"").append(escapeJson(seg.text)).append("\"");
            if (seg.style.color != null) {
                out.append(",\"color\":\"").append(seg.style.color).append("\"");
            }
            if (seg.style.bold) out.append(",\"bold\":true");
            if (seg.style.italic) out.append(",\"italic\":true");
            if (seg.style.underlined) out.append(",\"underlined\":true");
            if (seg.style.strikethrough) out.append(",\"strikethrough\":true");
            if (seg.style.obfuscated) out.append(",\"obfuscated\":true");
            out.append('}');
        }
        out.append("]}");
        return out.toString();
    }

    private static String mapColor(char code) {
        switch (code) {
            case '0': return "black";
            case '1': return "dark_blue";
            case '2': return "dark_green";
            case '3': return "dark_aqua";
            case '4': return "dark_red";
            case '5': return "dark_purple";
            case '6': return "gold";
            case '7': return "gray";
            case '8': return "dark_gray";
            case '9': return "blue";
            case 'a': return "green";
            case 'b': return "aqua";
            case 'c': return "red";
            case 'd': return "light_purple";
            case 'e': return "yellow";
            case 'f': return "white";
            default: return null;
        }
    }

    private static boolean isHex6(String s) {
        if (s.length() != 6) return false;
        for (int i = 0; i < 6; i++) {
            char c = s.charAt(i);
            boolean digit = (c >= '0' && c <= '9');
            boolean lower = (c >= 'a' && c <= 'f');
            boolean upper = (c >= 'A' && c <= 'F');
            if (!(digit || lower || upper)) return false;
        }
        return true;
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder((int) (s.length() * 1.1));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static final class Style {
        String color = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        Style copy() {
            Style s = new Style();
            s.color = this.color;
            s.bold = this.bold;
            s.italic = this.italic;
            s.underlined = this.underlined;
            s.strikethrough = this.strikethrough;
            s.obfuscated = this.obfuscated;
            return s;
        }

        void resetFormatting() {
            this.bold = false;
            this.italic = false;
            this.underlined = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }
    }

    private static final class Segment {
        final String text;
        final Style style;

        Segment(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }
}
