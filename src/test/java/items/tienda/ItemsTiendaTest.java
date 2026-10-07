package items.tienda;

import Managers.ItemManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemsTiendaTest {

    @Test
    void theNewItemsCanBeGivenWithGiveqp() {
        for (String id : ItemsTienda.claves()) assertTrue(ItemManager.claves().contains(id), id);
        assertEquals(3, ItemsTienda.usosMaximos("brujula_explorador"));
        assertEquals(3, ItemsTienda.usosMaximos("radar_minerales"));
        assertEquals(5, ItemsTienda.usosMaximos("propulsor_estelar"));
        assertEquals(0, ItemsTienda.usosMaximos("botiquin"));
        assertFalse(ItemsTienda.existe("dinocoins"));
    }

    @Test
    void theMysteryBoxPrizesAddUpTo100AndAllExist() {
        assertEquals("racion_viaje", ItemsTienda.premioCaja(0)[1]);
        assertEquals("mochila_nivel_3", ItemsTienda.premioCaja(99)[1]);
        for (int tirada = 0; tirada < 100; tirada++) {
            String clave = (String) ItemsTienda.premioCaja(tirada)[1];
            boolean existe = ItemManager.claves().contains(clave);
            if (!existe) {
                Material.valueOf(clave.toUpperCase());
                existe = true;
            }
            assertTrue(existe, clave);
        }
    }
}
