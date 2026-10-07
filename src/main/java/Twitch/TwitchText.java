package Twitch;

import net.md_5.bungee.api.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class TwitchText {

    static final String PREFIX = ChatColor.of("#9146FF") + "" + ChatColor.BOLD + "Twitch " + ChatColor.DARK_GRAY + "» ";
    static final ChatColor TEXT = ChatColor.of("#E6D6FF");
    static final ChatColor GRAY = ChatColor.of("#A0A0A0");
    static final ChatColor ERROR = ChatColor.of("#FF7F7F");
    static final ChatColor OK = ChatColor.of("#98FB98");
    static final ChatColor HIGHLIGHT = ChatColor.of("#FFD166");

    private TwitchText() {}

    // "12 días y 5 horas", "3 horas y 20 minutos" o "5 minutos"
    static String duration(long millis) {
        long minutes = Math.max(1, (millis + 59_999) / 60_000);
        long days = minutes / (24 * 60);
        long hours = (minutes / 60) % 24;
        long mins = minutes % 60;
        if (days > 0) return days + (days == 1 ? " día" : " días") + (hours > 0 ? " y " + hours + (hours == 1 ? " hora" : " horas") : "");
        if (hours > 0) return hours + (hours == 1 ? " hora" : " horas") + (mins > 0 ? " y " + mins + (mins == 1 ? " minuto" : " minutos") : "");
        return mins + (mins == 1 ? " minuto" : " minutos");
    }

    static String date(long millis) {
        return millis <= 0 ? "-" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(new Date(millis));
    }

    static String ago(long millis) {
        if (millis <= 0) return "nunca";
        long elapsed = System.currentTimeMillis() - millis;
        return elapsed < 60_000 ? "hace un momento" : "hace " + duration(elapsed);
    }
}
