package SistemaTumbas;

import net.md_5.bungee.api.ChatColor;

public class TumbaMessages {

    private static final String SYMBOL = "۞";
    private static final String SYMBOL_COLOR = "#CDB4DB";

    private static final String COLOR_INFO = "#B8C0FF";
    private static final String COLOR_SUCCESS = "#A8E6CF";
    private static final String COLOR_ERROR = "#FFADAD";
    private static final String COLOR_WARN = "#FFD6A5";
    private static final String COLOR_ACCENT = "#FDE2E4";

    private static String hex(String hexColor, String text) {
        return ChatColor.of(hexColor) + text;
    }

    private static String prefix() {
        return hex(SYMBOL_COLOR, SYMBOL + " ");
    }

    public static String info(String text) {
        return prefix() + hex(COLOR_INFO, text);
    }

    public static String success(String text) {
        return prefix() + hex(COLOR_SUCCESS, text);
    }

    public static String error(String text) {
        return prefix() + hex(COLOR_ERROR, text);
    }

    public static String warn(String text) {
        return prefix() + hex(COLOR_WARN, text);
    }

    public static String accent(String text) {
        return hex(COLOR_ACCENT, text);
    }
}