package items;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WardenGunTest {

    @Test
    void itIsNotBrokenAnymore() {
        assertEquals(10, WardenGun.dano(false, false));
        assertEquals(6, WardenGun.dano(false, true));
        assertEquals(5, WardenGun.dano(true, false));
        assertEquals(5, WardenGun.dano(true, true));
        assertEquals(100, WardenGun.RECARGA);
        assertTrue(WardenGun.CARGA < WardenGun.RECARGA);
        assertEquals(3, WardenGun.MAX_GOLPES);
    }

    // El rayo pega a lo que tiene delante y a medio bloque del costado, no a lo que está atrás o más lejos
    @Test
    void theBeamHitsWhatIsInFrontWithinRange() {
        Vector origen = new Vector(0, 1.6, 0);
        Vector dir = new Vector(1, 0, 0);
        BoundingBox zombie = new BoundingBox(5, 0, -0.3, 5.6, 1.95, 0.3);
        assertTrue(WardenGun.pasaPor(zombie, origen, dir, WardenGun.ALCANCE));
        assertFalse(WardenGun.pasaPor(zombie, origen, dir, 4));
        assertFalse(WardenGun.pasaPor(zombie, origen, new Vector(-1, 0, 0), WardenGun.ALCANCE));
        assertTrue(WardenGun.pasaPor(zombie.clone().shift(0, 0, 0.7), origen, dir, WardenGun.ALCANCE));
        assertFalse(WardenGun.pasaPor(zombie.clone().shift(0, 0, 1.5), origen, dir, WardenGun.ALCANCE));
        // Pegado al que dispara también cuenta
        assertTrue(WardenGun.pasaPor(new BoundingBox(-0.2, 0, -0.3, 0.4, 1.95, 0.3), origen, dir, WardenGun.ALCANCE));
    }
}
