package Events.MissionSystem;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

// Los tres tipos de misión, cada uno con sus dos colores pastel (action bars, menú y anuncios) y cómo se escribe en los
// comandos: la normal con su número (1), la extra con el número de su misión y "ex" (1ex) y la de trabajo con "tra" (170tra)
public enum TipoMision {
    NORMAL("#C9A7EB", "#F7B8D2", "", "Misión"),
    EXTRA("#9ED8F5", "#8FE8E2", "ex", "Misión extra"),
    TRABAJO("#C8A27C", "#F2D58A", "tra", "Misión de trabajo");

    public final String primario;
    public final String secundario;
    public final String sufijo;
    public final String nombre;

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

    // Cómo sale en el tab de los comandos: la extra va con el número de su misión
    public static String token(int number, int parent) {
        return switch (de(number, parent)) {
            case NORMAL -> String.valueOf(number);
            case EXTRA -> parent + EXTRA.sufijo;
            case TRABAJO -> number + TRABAJO.sufijo;
        };
    }

    // El número de misión de lo que se escribió ("1", "1ex", "170tra" o el número de siempre); -1 si no es válido.
    // extraOf da la extra de una misión normal (o -1 si no tiene)
    public static int parse(String text, IntUnaryOperator extraOf) {
        String token = text.toLowerCase(Locale.ROOT).trim();
        try {
            if (token.endsWith(TRABAJO.sufijo)) {
                int number = Integer.parseInt(token.substring(0, token.length() - TRABAJO.sufijo.length()));
                return MisionTrabajo.es(number) ? number : -1;
            }
            if (token.endsWith(EXTRA.sufijo)) {
                return extraOf.applyAsInt(Integer.parseInt(token.substring(0, token.length() - EXTRA.sufijo.length())));
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
