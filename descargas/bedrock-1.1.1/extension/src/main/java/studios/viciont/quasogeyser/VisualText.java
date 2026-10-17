package studios.viciont.quasogeyser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Conversiones exclusivas del texto que sale hacia Bedrock. */
final class VisualText {
    private static final Pattern COLORS = Pattern.compile("(?i)§[0-9a-z]");
    private static final String[] MENUS = {"misiones", "misiones_extra", "misiones_trabajo",
            "menu", "habilidades", "trabajos", "tiendas"};
    private final Map<Character, String> ranks = new HashMap<>();

    VisualText() throws Exception {
        Properties properties = new Properties();
        try (var in = new InputStreamReader(VisualText.class.getResourceAsStream("/ranks.properties"), StandardCharsets.UTF_8)) {
            properties.load(in);
        }
        for (String key : properties.stringPropertyNames())
            ranks.put((char) Integer.parseInt(key, 16), properties.getProperty(key));
    }

    String ranks(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder out = new StringBuilder();
        String formatting = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                out.append('§').append(code);
                if (code == 'r') formatting = "";
                else if ("klo".indexOf(code) >= 0) formatting += "§" + code;
                else formatting = "§" + code; // Incluye los colores exclusivos de Bedrock.
                continue;
            }
            String replacement = ranks.get(c);
            if (replacement == null) out.append(c);
            else out.append("§r§f").append(replacement).append("§r").append(formatting);
        }
        return out.toString();
    }

    static String menuTitle(String title) {
        if (title == null) return null;
        for (int i = 0; i < MENUS.length; i++) {
            if (title.indexOf((char) (0x3202 + i)) >= 0) return "[quaso:" + MENUS[i] + "]";
        }
        return title;
    }

    static String plain(String text) { return text == null ? "" : COLORS.matcher(text).replaceAll("").strip(); }
}
