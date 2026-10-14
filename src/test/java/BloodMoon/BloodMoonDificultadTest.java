package BloodMoon;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloodMoonDificultadTest {

    @Test
    void bloodMoonMobsSpawnMoreOftenOnHarderNights() {
        assertEquals(5, BloodMoonDificultad.FACIL.probabilidadMob());
        assertEquals(25, BloodMoonDificultad.NORMAL.probabilidadMob());
        assertEquals(50, BloodMoonDificultad.DIFICIL.probabilidadMob());
    }

    // Con los multiplicadores de siempre (x2 de daño y x3 de aguante) Difícil queda igual que antes
    @Test
    void regularMobsAreStrongerOnEachDifficultyButHardKeepsTheOriginalValues() {
        assertEquals(1.25, BloodMoonDificultad.FACIL.multiplicador(2), 1e-9);
        assertEquals(1.6, BloodMoonDificultad.NORMAL.multiplicador(2), 1e-9);
        assertEquals(2.0, BloodMoonDificultad.DIFICIL.multiplicador(2), 1e-9);
        assertEquals(1.5, BloodMoonDificultad.FACIL.multiplicador(3), 1e-9);
        assertEquals(2.2, BloodMoonDificultad.NORMAL.multiplicador(3), 1e-9);
        assertEquals(3.0, BloodMoonDificultad.DIFICIL.multiplicador(3), 1e-9);
        assertTrue(BloodMoonDificultad.FACIL.multiplicador(2) < BloodMoonDificultad.NORMAL.multiplicador(2));
    }

    @Test
    void easyRemovesThreeQuartersOfTheLootAndNormalAThird() {
        assertEquals(0.25, BloodMoonDificultad.FACIL.botin(), 1e-9);
        assertEquals(0.65, BloodMoonDificultad.NORMAL.botin(), 1e-9);
        assertEquals(1.0, BloodMoonDificultad.DIFICIL.botin(), 1e-9);
    }

    @Test
    void difficultyIsReadWithOrWithoutAccents() {
        assertEquals(BloodMoonDificultad.FACIL, BloodMoonDificultad.de("fácil"));
        assertEquals(BloodMoonDificultad.FACIL, BloodMoonDificultad.de("FACIL"));
        assertEquals(BloodMoonDificultad.NORMAL, BloodMoonDificultad.de(" normal "));
        assertEquals(BloodMoonDificultad.DIFICIL, BloodMoonDificultad.de("Difícil"));
        assertNull(BloodMoonDificultad.de("extrema"));
        assertNull(BloodMoonDificultad.de(null));
        assertEquals("dificil", BloodMoonDificultad.DIFICIL.id());
    }

    @Test
    void onlyTheFourVanillaMobsHaveABloodMoonVersion() {
        assertEquals(BloodMoonMobs.Tipo.ZOMBIE, BloodMoonMobs.Tipo.de(EntityType.ZOMBIE));
        assertEquals(BloodMoonMobs.Tipo.ESQUELETO, BloodMoonMobs.Tipo.de(EntityType.SKELETON));
        assertEquals(BloodMoonMobs.Tipo.ARANA, BloodMoonMobs.Tipo.de(EntityType.SPIDER));
        assertEquals(BloodMoonMobs.Tipo.CREEPER, BloodMoonMobs.Tipo.de(EntityType.CREEPER));
        assertNull(BloodMoonMobs.Tipo.de(EntityType.HUSK));
        assertNull(BloodMoonMobs.Tipo.de(EntityType.CAVE_SPIDER));
        assertEquals(BloodMoonMobs.Tipo.ARANA, BloodMoonMobs.Tipo.porId("aranabloodmoon"));
        assertNull(BloodMoonMobs.Tipo.porId("zombiefloral"));
    }

    @Test
    void bloodMoonMobsAreRecognisedByTheirTag() {
        Zombie zombie = mock(Zombie.class);
        assertFalse(BloodMoonMobs.es(zombie));
        assertFalse(BloodMoonMobs.es(null));
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(zombie.getPersistentDataContainer()).thenReturn(data);
        assertFalse(BloodMoonMobs.es(zombie));
        when(data.get(BloodMoonMobs.KEY, PersistentDataType.STRING)).thenReturn("zombiebloodmoon");
        assertEquals(BloodMoonMobs.Tipo.ZOMBIE, BloodMoonMobs.tipo(zombie));
    }
}
