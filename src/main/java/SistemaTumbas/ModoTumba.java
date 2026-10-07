package SistemaTumbas;

import java.util.Locale;

// Quién puede abrir una tumba ajena: nadie (privada), cualquiera (abierta) o nadie un rato y después cualquiera (mixta).
// El dueño y los admins siempre pueden
public enum ModoTumba {
    PRIVADA, ABIERTA, MIXTA;

    public static ModoTumba parse(String text, ModoTumba fallback) {
        if (text == null) return fallback;
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // Si otro jugador ya la puede abrir
    public boolean abiertaParaTodos(long creada, long ahora, int minutosPrivada) {
        return switch (this) {
            case PRIVADA -> false;
            case ABIERTA -> true;
            case MIXTA -> ahora >= creada + minutosPrivada * 60_000L;
        };
    }

    // Cuántos minutos dura la tumba antes de soltar las cosas: la mixta dura su parte privada más la abierta
    public int minutosDeVida(int minutos, int minutosPrivada, int minutosAbierta) {
        return this == MIXTA ? minutosPrivada + minutosAbierta : minutos;
    }

    // 00:05:00, para el reloj de la tumba y /muertes
    public static String reloj(long millis) {
        long segundos = Math.max(0, millis) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", segundos / 3600, (segundos / 60) % 60, segundos % 60);
    }
}
