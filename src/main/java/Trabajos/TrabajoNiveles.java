package Trabajos;

// La curva de niveles y las recompensas, igual para los 6 trabajos. Un jugador trabajando en serio gana unas 1.000
// XP por hora: el nivel 10 sale en ~1 hora y media, el 30 en ~15, el 50 en ~45 y el 100 en ~200 horas
public final class TrabajoNiveles {

    private TrabajoNiveles() {}

    // XP para pasar del nivel anterior a este (nivel 1 = 45 XP, nivel 50 = 1.823 XP, nivel 100 = 4.016 XP)
    public static int xpParaNivel(int nivel) {
        if (nivel <= 0) return 0;
        return (int) Math.round(25 + 20 * Math.pow(nivel, 1.15));
    }

    // DinoCoins por subir de nivel según lo difícil que es: 2 hasta el 20, 3 hasta el 40... y 6 del 81 al 100
    public static int monedasPorNivel(int nivel) {
        if (nivel <= 0) return 0;
        return Math.min(6, (nivel - 1) / 20 + 2);
    }

    // Cada 5 niveles un bonus que crece con el nivel: 13 en el 5, 40 en el 50 y 70 en el 100
    public static int bonus(int nivel) {
        if (nivel <= 0 || nivel % 5 != 0) return 0;
        return (int) Math.round(10 + nivel * 0.6);
    }

    // Puntos de experiencia de Minecraft al subir (más en los niveles de bonus)
    public static int experiencia(int nivel) {
        int puntos = 30 + 6 * nivel;
        if (nivel % 5 == 0) puntos += 12 * nivel;
        return puntos;
    }

    public static int monedasTotales(int nivel) {
        return monedasPorNivel(nivel) + bonus(nivel);
    }

    // El próximo nivel con bonus desde este nivel (-1 si ya está en el máximo)
    public static int proximoBonus(int nivelActual) {
        int siguiente = (nivelActual / 5 + 1) * 5;
        return siguiente > Trabajo.NIVEL_MAXIMO ? -1 : siguiente;
    }
}
