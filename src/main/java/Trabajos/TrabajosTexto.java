package Trabajos;

import net.md_5.bungee.api.ChatColor;

import java.text.NumberFormat;
import java.util.Locale;

// Los colores de los trabajos: café pastel y blanco, con crema, beige, salvia y dorado suave
final class TrabajosTexto {

    static final ChatColor CAFE = ChatColor.of("#C8A27C");
    static final ChatColor CAFE_OSCURO = ChatColor.of("#9C7A5B");
    static final ChatColor CREMA = ChatColor.of("#F5EBDD");
    static final ChatColor BLANCO = ChatColor.of("#FFFFFF");
    static final ChatColor BEIGE = ChatColor.of("#E8D5B7");
    static final ChatColor SALVIA = ChatColor.of("#A8C3A0");
    static final ChatColor DORADO = ChatColor.of("#E6C77A");
    static final ChatColor ROSA = ChatColor.of("#D9A5A0");
    static final ChatColor GRIS = ChatColor.of("#A89F94");

    static final String PREFIJO = CAFE + "" + ChatColor.BOLD + "Trabajos " + CAFE_OSCURO + "» " + CREMA;

    private static final NumberFormat NUMEROS = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-ES"));

    private TrabajosTexto() {}

    static String numero(double valor) {
        return NUMEROS.format(Math.floor(valor));
    }

    static ChatColor color(Trabajo trabajo) {
        return ChatColor.of(trabajo.color());
    }

    static String nombre(Trabajo trabajo) {
        return color(trabajo) + trabajo.icono() + " " + trabajo.nombre();
    }

    // ▰▰▰▱▱▱▱▱▱▱ 30%
    static String barra(double actual, double total) {
        double parte = total <= 0 ? 1 : Math.max(0, Math.min(1, actual / total));
        int llenos = (int) Math.round(parte * 10);
        return SALVIA + "▰".repeat(llenos) + GRIS + "▱".repeat(10 - llenos) + " " + BLANCO + (int) Math.floor(parte * 100) + "%";
    }

    // "5 h 20 min", "12 min" o "menos de un minuto"
    static String tiempo(long millis) {
        long minutos = millis / 60_000;
        if (minutos <= 0) return "menos de un minuto";
        long horas = minutos / 60;
        minutos %= 60;
        if (horas == 0) return minutos + " min";
        return horas + " h" + (minutos > 0 ? " " + minutos + " min" : "");
    }
}
