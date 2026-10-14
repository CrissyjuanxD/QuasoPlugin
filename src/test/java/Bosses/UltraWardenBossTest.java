package Bosses;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UltraWardenBossTest {

    @Test
    void ithas2000OfLife() {
        assertEquals(2000, UltraWardenBoss.HEALTH);
        assertEquals("ultra_warden", UltraWardenBoss.BOSS_ID);
    }

    // El sonic boom del Silencio Profundo saca el 90% de la vida en cualquier dificultad
    @Test
    void theSilenceBoomIgnoresTheDifficulty() {
        double objetivo = 360;
        double facil = UltraWardenBoss.sinDificultad(org.bukkit.Difficulty.EASY, objetivo);
        assertEquals(objetivo, Math.min(facil / 2 + 1, facil), 1e-9);
        assertEquals(objetivo, UltraWardenBoss.sinDificultad(org.bukkit.Difficulty.NORMAL, objetivo), 1e-9);
        assertEquals(objetivo, UltraWardenBoss.sinDificultad(org.bukkit.Difficulty.HARD, objetivo) * 1.5, 1e-9);
    }

    // La flecha que pasa por el catalizador en este tick lo rompe aunque vaya rápido
    @Test
    void anArrowThatCrossesTheCatalystCounts() {
        Vector catalizador = new Vector(0, 2, 0);
        assertTrue(UltraWardenBoss.distanciaASegmento(catalizador, new Vector(-2, 2, 0), new Vector(2, 2, 0)) < 0.01);
        assertEquals(1, UltraWardenBoss.distanciaASegmento(catalizador, new Vector(-2, 3, 0), new Vector(2, 3, 0)), 1e-9);
        assertEquals(Math.sqrt(1 + 9), UltraWardenBoss.distanciaASegmento(catalizador, new Vector(3, 1, 0), new Vector(5, 1, 0)), 1e-9);
    }
}
