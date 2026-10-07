package Pesca;

import net.kyori.adventure.text.format.TextColor;

import java.util.Random;

// Los premios especiales de las zonas de pesca. La tabla normal sale con la pesca buena (naranja) y la de suerte con la
// perfecta (verde), que sube la probabilidad de los raros y épicos. Cada tabla suma 100
final class FishingLoot {

    enum Rareza {
        COMUN("Común", 0xB8B8B8),
        POCO_COMUN("Poco común", 0x8FD694),
        RARO("Raro", 0x62B8D4),
        EPICO("Épico", 0xC792EA);

        private final String nombre;
        private final TextColor color;

        Rareza(String nombre, int color) {
            this.nombre = nombre;
            this.color = TextColor.color(color);
        }

        String nombre() { return nombre; }
        TextColor color() { return color; }
    }

    record Premio(String clave, int peso, int pesoPerfecta, Rareza rareza) {}

    static final Premio[] PREMIOS = {
            new Premio("chatarra", 30, 16, Rareza.COMUN),
            new Premio("manzana_podrida", 25, 14, Rareza.COMUN),
            new Premio("zanahoria_encantada", 15, 18, Rareza.POCO_COMUN),
            new Premio("pepitas_hierro_oxidadas", 12, 16, Rareza.POCO_COMUN),
            new Premio("pepitas_diamante", 8, 14, Rareza.RARO),
            new Premio("fragmentos_ambar", 5, 10, Rareza.RARO),
            new Premio("fosiles_pequenos", 3, 7, Rareza.EPICO),
            new Premio("lingote_platino", 2, 5, Rareza.EPICO)
    };

    // La pesca buena da premio especial el 40% de las veces, +5% por nivel de Suerte marina
    static final int CHANCE_BUENA = 40;
    static final int CHANCE_POR_SUERTE = 5;
    static final int TOPE_DIARIO = 60;

    private FishingLoot() {}

    static int chanceEspecial(FishingMiniGame.Resultado resultado, int suerteMarina) {
        return switch (resultado) {
            case PERFECTA -> 100;
            case BUENA -> CHANCE_BUENA + CHANCE_POR_SUERTE * Math.max(0, Math.min(3, suerteMarina));
            default -> 0;
        };
    }

    static Premio elegir(Random random, boolean perfecta) {
        int tirada = random.nextInt(100);
        for (Premio premio : PREMIOS) {
            tirada -= perfecta ? premio.pesoPerfecta() : premio.peso();
            if (tirada < 0) return premio;
        }
        return PREMIOS[PREMIOS.length - 1];
    }
}
