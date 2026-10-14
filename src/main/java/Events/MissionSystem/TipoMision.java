package Events.MissionSystem;

import java.util.Locale;
import java.util.function.IntBinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Los tres tipos de misión, cada uno con sus dos colores pastel (action bars, menú y anuncios) y cómo se escribe en los
// comandos: la normal con su número (1), la extra con el número de su misión y "ex" (1ex; si tiene varias, 1ex2, 1ex3...)
// y la de trabajo con "tra" (170tra)
public enum TipoMision {
    NORMAL("#C9A7EB", "#F7B8D2", "", "Misión"),
    EXTRA("#9ED8F5", "#8FE8E2", "ex", "Misión extra"),
    TRABAJO("#C8A27C", "#F2D58A", "tra", "Misión de trabajo");

    public final String primario;
    public final String secundario;
    public final String sufijo;
    public final String nombre;
    private static final Pattern EXTRA_TOKEN = Pattern.compile("(\\d+)ex(\\d*)");

    TipoMision(String primario, String secundario, String sufijo, String nombre) {
        this.primario = primario;
        this.secundario = secundario;
        this.sufijo = sufijo;
        this.nombre = nombre;
    }

    public static TipoMision de(int number, int parent) {
        if (MisionTrabajo.es(number)) return TRABAJO;
        return parent > 0 ? EXTRA : NORMAL;
    }

    public static String token(int number, int parent) {
        return token(number, parent, 0);
    }

    // Cómo sale en el tab de los comandos: la extra va con el número de su misión y, desde la segunda, con su orden
    // (orden 0 = 12ex, 1 = 12ex2)
    public static String token(int number, int parent, int orden) {
        return switch (de(number, parent)) {
            case NORMAL -> String.valueOf(number);
            case EXTRA -> parent + EXTRA.sufijo + (orden > 0 ? String.valueOf(orden + 1) : "");
            case TRABAJO -> number + TRABAJO.sufijo;
        };
    }

    // El número de misión de lo que se escribió ("1", "1ex", "1ex2", "170tra" o el número de siempre); -1 si no es
    // válido. extraOf da la extra de una misión normal según su orden (0 la primera), o -1 si no la tiene
    public static int parse(String text, IntBinaryOperator extraOf) {
        String token = text.toLowerCase(Locale.ROOT).trim();
        try {
            if (token.endsWith(TRABAJO.sufijo)) {
                int number = Integer.parseInt(token.substring(0, token.length() - TRABAJO.sufijo.length()));
                return MisionTrabajo.es(number) ? number : -1;
            }
            Matcher extra = EXTRA_TOKEN.matcher(token);
            if (extra.matches()) {
                int orden = extra.group(2).isEmpty() ? 0 : Integer.parseInt(extra.group(2)) - 1;
                return orden < 0 ? -1 : extraOf.applyAsInt(Integer.parseInt(extra.group(1)), orden);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Para ordenar los tokens de las extras: por su misión y después por su orden ("12ex2" va después de "12ex")
    public static int[] partesExtra(String token) {
        Matcher extra = EXTRA_TOKEN.matcher(token);
        if (!extra.matches()) return new int[]{Integer.MAX_VALUE, 0};
        return new int[]{Integer.parseInt(extra.group(1)), extra.group(2).isEmpty() ? 1 : Integer.parseInt(extra.group(2))};
    }
}
