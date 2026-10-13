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

    // DinoCoins por subir de nivel: crecen igual que la XP que pide el nivel (1 DinoCoin cada 180 XP, mínimo 2), así
    // que los niveles altos pagan lo que cuestan: 2 en el nivel 1, 10 en el 50 y 22 en el 100
    public static int monedasPorNivel(int nivel) {
        if (nivel <= 0) return 0;
        return (int) Math.max(2, Math.round(xpParaNivel(nivel) / 180.0));
    }

    // Cada 5 niveles un bonus que también sigue a la XP del nivel: 14 en el 5, 56 en el 50 y 110 en el 100
    public static int bonus(int nivel) {
        if (nivel <= 0 || nivel % 5 != 0) return 0;
        return (int) Math.round(10 + xpParaNivel(nivel) / 40.0);
    }

    // El nivel del libro del encantamiento del trabajo que se gana al llegar a este nivel (0 si no da libro): I en el
    // 10, II en el 40 y III en el 70
    public static int libro(int nivel) {
        return switch (nivel) {
            case 10 -> 1;
            case 40 -> 2;
            case 70 -> 3;
            default -> 0;
        };
    }

    // El próximo nivel que da libro (-1 si ya los ganó todos)
    public static int proximoLibro(int nivelActual) {
        for (int nivel : new int[]{10, 40, 70}) {
            if (nivel > nivelActual) return nivel;
        }
        return -1;
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
