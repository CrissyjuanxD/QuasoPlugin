package Trabajos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// La curva de niveles y las recompensas. Los 6 trabajos piden la misma XP por nivel (la curva de Guerrero) y pagan
// lo mismo (DinoCoins, bonus y experiencia)
public final class TrabajoNiveles {

    private TrabajoNiveles() {}

    // XP base para pasar del nivel anterior a este (nivel 1 = 45 XP, nivel 50 = 1.823 XP, nivel 100 = 4.016 XP)
    public static int xpParaNivel(int nivel) {
        if (nivel <= 0) return 0;
        return (int) Math.round(25 + 20 * Math.pow(nivel, 1.15));
    }

    // XP que pide ese trabajo para pasar a este nivel: la misma curva en todos, así ninguno se sube en un día
    public static int xpParaNivel(Trabajo trabajo, int nivel) {
        return xpParaNivel(nivel);
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

    // Un libro que se gana al llegar a un nivel del trabajo
    public record Libro(int nivel, Encantamientos.QuasoEnchant encantamiento, int nivelLibro) {}

    // El encantamiento del trabajo: I en el nivel 10, II en el 40 y III en el 70. Minería además gana Veta: I en el 25
    // y II en el 55
    public static List<Libro> libros(Trabajo trabajo) {
        List<Libro> libros = new ArrayList<>(List.of(
                new Libro(10, trabajo.encantamiento(), 1),
                new Libro(40, trabajo.encantamiento(), 2),
                new Libro(70, trabajo.encantamiento(), 3)));
        if (trabajo == Trabajo.MINERIA) {
            libros.add(new Libro(25, Encantamientos.QuasoEnchant.VETA, 1));
            libros.add(new Libro(55, Encantamientos.QuasoEnchant.VETA, 2));
        }
        libros.sort(Comparator.comparingInt(Libro::nivel));
        return libros;
    }

    // El libro que se gana al llegar a este nivel (null si no da libro)
    public static Libro libro(Trabajo trabajo, int nivel) {
        for (Libro libro : libros(trabajo)) {
            if (libro.nivel() == nivel) return libro;
        }
        return null;
    }

    // El próximo libro desde este nivel (null si ya los ganó todos)
    public static Libro proximoLibro(Trabajo trabajo, int nivelActual) {
        for (Libro libro : libros(trabajo)) {
            if (libro.nivel() > nivelActual) return libro;
        }
        return null;
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
