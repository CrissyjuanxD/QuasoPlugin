package items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcoSculkTest {

    // Lo mismo que suma el juego por Poder: con la flecha base de 2, Poder V llega a 5 y Poder VI a 5,5
    @Test
    void powerAddsLikeTheGame() {
        assertEquals(0, ArcoSculk.bonoPoder(0));
        assertEquals(1, ArcoSculk.bonoPoder(1));
        assertEquals(3, ArcoSculk.bonoPoder(5));
        assertEquals(3.5, ArcoSculk.bonoPoder(6));
        assertEquals(0, ArcoSculk.bonoPoder((org.bukkit.inventory.ItemStack) null));
    }
}
