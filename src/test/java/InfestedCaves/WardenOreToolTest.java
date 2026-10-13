package InfestedCaves;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WardenOreToolTest {

    // El Pico Floral y el de Celestita son picos de Netherite por debajo
    @Test void onlyNetheritePickaxesBreakTheDeepOres() {
        assertTrue(WardenCaveListeners.puedeRomper(Material.NETHERITE_PICKAXE));
        assertFalse(WardenCaveListeners.puedeRomper(Material.DIAMOND_PICKAXE));
        assertFalse(WardenCaveListeners.puedeRomper(Material.NETHERITE_SHOVEL));
        assertFalse(WardenCaveListeners.puedeRomper(Material.AIR));
        assertEquals(Material.NETHERITE_PICKAXE, items.FloralItems.Pieza.PICO.base);
        assertEquals(Material.NETHERITE_PICKAXE, items.EndItems.Tool.PICO.base);
    }
}
