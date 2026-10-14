package BloodMoon;

import java.util.Locale;

// Dificultad de la BloodMoon (Dificultad en el config de cada mundo). Cambia cuántos mobs normales salen como mobs de
// BloodMoon, cuánto de la fuerza extra reciben los mobs normales (daño, aguante y efectos al pegar) y cuánto del botín
// extra sueltan. Difícil es la BloodMoon de siempre
public enum BloodMoonDificultad {
    FACIL("Fácil", 5, 0.25, 0.25),
    NORMAL("Normal", 25, 0.6, 0.65),
    DIFICIL("Difícil", 50, 1.0, 1.0);

    private final String nombre;
    private final int probabilidadMob;
    private final double fuerza;
    private final double botin;

    BloodMoonDificultad(String nombre, int probabilidadMob, double fuerza, double botin) {
        this.nombre = nombre;
        this.probabilidadMob = probabilidadMob;
        this.fuerza = fuerza;
        this.botin = botin;
    }

    public String nombre() {
        return nombre;
    }

    // % de que un zombie, esqueleto, araña o creeper que sale solo sea de BloodMoon
    public int probabilidadMob() {
        return probabilidadMob;
    }

    // Parte de la fuerza extra que se queda: con x2 de daño, Fácil pega x1.25, Normal x1.6 y Difícil x2
    public double multiplicador(int base) {
        return 1 + (Math.max(1, base) - 1) * fuerza;
    }

    // Los efectos al pegar duran menos y el rayo del creeper sale solo a veces
    public double fuerza() {
        return fuerza;
    }

    // Parte del botín extra que se queda: Fácil 25% (se quita el 75%), Normal 65% (se quita el 35%), Difícil todo
    public double botin() {
        return botin;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    // Acepta facil/fácil, normal y dificil/difícil; cualquier otra cosa es null
    public static BloodMoonDificultad de(String texto) {
        if (texto == null) return null;
        return switch (texto.trim().toLowerCase(Locale.ROOT)) {
            case "facil", "fácil", "easy" -> FACIL;
            case "normal" -> NORMAL;
            case "dificil", "difícil", "hard" -> DIFICIL;
            default -> null;
        };
    }
}
