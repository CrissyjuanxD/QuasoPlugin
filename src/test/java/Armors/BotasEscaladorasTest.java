package Armors;

import items.WardenCaveItems;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotasEscaladorasTest {

    // El salto en la pared empuja hacia arriba y lejos de la pared, aunque el jugador la esté mirando
    @Test
    void theWallJumpPushesUpAndAwayFromTheWall() {
        Vector mirandoLaPared = new Vector(0, 0, -1);
        Vector impulso = BotasEscaladoras.impulso(BlockFace.NORTH, mirandoLaPared);
        assertEquals(BotasEscaladoras.IMPULSO_ARRIBA, impulso.getY(), 1e-9);
        assertTrue(impulso.getZ() > 0.2, "tiene que alejarse de la pared del norte: " + impulso);
        assertEquals(0, impulso.getX(), 1e-9);

        Vector deCostado = BotasEscaladoras.impulso(BlockFace.EAST, new Vector(0, 0, 1));
        assertTrue(deCostado.getX() < -0.4);
        assertTrue(deCostado.getZ() > 0, "sigue un poco hacia donde mira");
    }

    // Agarrado baja lento y por poco tiempo
    @Test
    void theClingIsSlowAndShort() {
        assertTrue(BotasEscaladoras.CAIDA_AGARRADO < 0 && BotasEscaladoras.CAIDA_AGARRADO > -0.15);
        assertTrue(BotasEscaladoras.MAX_AGARRE <= 60);
    }

    @Test
    void theLoreTellsWhatTheBootsDo() {
        String lore = String.join("", WardenArmor.lore(WardenCaveItems.Variant.VERDE, 4, false, true));
        assertTrue(lore.contains("Se pega a las paredes"));
        assertTrue(lore.contains("Salta de pared en pared"));
        assertFalse(String.join("", WardenArmor.lore(WardenCaveItems.Variant.VERDE, 4, false)).contains("paredes"));
    }
}
