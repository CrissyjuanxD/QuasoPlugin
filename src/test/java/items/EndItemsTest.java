package items;

import Managers.ItemManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndItemsTest {

    @Test
    void theEndItemsCanBeGivenWithGiveqp() {
        List<String> claves = ItemManager.claves();
        for (EndItems.Tool tool : EndItems.Tool.values()) assertTrue(claves.contains(tool.id), tool.id);
        for (String id : List.of("lingote_celestita", "plantilla_celestita", "enderking_pearl", "peto_warden_alado")) {
            assertTrue(claves.contains(id), id);
        }
    }

    @Test
    void onlyTheWeaponsHitTheEnderKingHarderAndTheMissionIdsMatch() {
        assertEquals(EndItems.Tool.ESPADA, EndItems.toolById("espada_celestita"));
        assertEquals(EndItems.Tool.HACHA, EndItems.toolById("hacha_celestita"));
        assertEquals(EndItems.Tool.LANZA, EndItems.toolById("lanza_celestita"));
        assertTrue(EndItems.Tool.LANZA.weapon);
        assertFalse(EndItems.Tool.PICO.weapon);
        assertNull(EndItems.toolById("cristal_celestita"));
    }

    @Test
    void theMaterialsAreKeptOutOfVanillaRecipesButNotTheTools() {
        assertTrue(EndItems.isMaterial("lingote_celestita"));
        assertTrue(EndItems.isMaterial("plantilla_celestita"));
        assertTrue(EndItems.isMaterial("enderking_pearl"));
        assertFalse(EndItems.isMaterial("espada_celestita"));
        assertFalse(EndItems.isMaterial(null));
    }
}
