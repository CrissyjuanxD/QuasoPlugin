package Habilidades;

import org.bukkit.Material;

// Las tres habilidades con lo que da cada uno de sus 8 niveles (el menú y la web leen de aquí)
public enum HabilidadesType {
    VITALIDAD("Vitalidad",
            "Otorga +2.5 Corazones permanentes.",
            "Otorga +2.5 Corazones permanentes.",
            "Otorga +2.5 Corazones permanentes.",
            "Otorga +2.5 Corazones permanentes.",
            "Otorga +4 Corazones permanentes.",
            "Otorga +4 Corazones permanentes.",
            "Otorga +4 Corazones permanentes.",
            "Otorga +4 Corazones permanentes."),
    RESISTENCIA("Resistencia",
            "8% prob. bloquear daño directo de proyectiles.",
            "8% prob. bloquear daño directo de monstruos.",
            "8% prob. bloquear cualquier daño.",
            "Resistencia I infinita.",
            "14% prob. bloquear daño directo de proyectiles.",
            "14% prob. bloquear daño directo de monstruos.",
            "14% prob. bloquear cualquier daño.",
            "Resistencia II infinita."),
    AGILIDAD("Agilidad",
            "Haste I infinito.",
            "Doble Salto y Gracia del Delfín II infinito.",
            "Velocidad I infinito.",
            "Triple Salto.",
            "Fuerza I infinita.",
            "Salto Alto I infinito.",
            "Velocidad II infinita.",
            "Cuádruple Salto.");

    public static final int NIVELES = 8;

    // Lo que cuesta cada nivel (igual en las tres): niveles de XP, bloques y DinoCoins
    public record Costo(int xp, Material bloque, int cantidad, String nombre, int dinocoins) {}

    private static final Costo[] COSTOS = {
            new Costo(30, Material.GOLD_BLOCK, 12, "Bloques de Oro", 5),
            new Costo(40, Material.DIAMOND_BLOCK, 15, "Bloques de Diamante", 10),
            new Costo(50, Material.EMERALD_BLOCK, 32, "Bloques de Esmeralda", 15),
            new Costo(60, Material.NETHERITE_BLOCK, 3, "Bloques de Netherite", 20),
            new Costo(70, Material.GOLD_BLOCK, 30, "Bloques de Oro", 10),
            new Costo(80, Material.DIAMOND_BLOCK, 40, "Bloques de Diamante", 20),
            new Costo(90, Material.EMERALD_BLOCK, 64, "Bloques de Esmeralda", 30),
            new Costo(100, Material.NETHERITE_BLOCK, 6, "Bloques de Netherite", 40),
    };

    private final String displayName;
    private final String[] niveles;

    HabilidadesType(String displayName, String... niveles) {
        this.displayName = displayName;
        this.niveles = niveles;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Lo que da el nivel (1 a 8)
    public String descripcion(int nivel) {
        return nivel >= 1 && nivel <= niveles.length ? niveles[nivel - 1] : "";
    }

    public static Costo costo(int nivel) {
        return COSTOS[Math.max(1, Math.min(COSTOS.length, nivel)) - 1];
    }
}
