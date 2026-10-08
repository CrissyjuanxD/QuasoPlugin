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
        for (String id : List.of("lingote_celestita", "plantilla_celestita", "enderking_pearl", "peto_warden_alado", "warden_gun")) {
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

    // La espada le pega el doble a todo lo del End: lo que está en el End y los endermans, endermites y shulkers de afuera
    @Test
    void theCelestiteSwordHitsEveryEndMobHarder() {
        assertEquals(2, EspadaCelestita.MULTIPLICADOR);
        // La espada de Netherite hace 8: la de Celestita 12; las demás herramientas 1 más
        assertEquals(4, EndItems.Tool.ESPADA.extra);
        assertEquals(1, EndItems.Tool.HACHA.extra);
        assertTrue(EspadaCelestita.delEnd(org.bukkit.World.Environment.THE_END, org.bukkit.entity.EntityType.ZOMBIE));
        assertTrue(EspadaCelestita.delEnd(org.bukkit.World.Environment.NORMAL, org.bukkit.entity.EntityType.ENDERMAN));
        assertTrue(EspadaCelestita.delEnd(org.bukkit.World.Environment.NETHER, org.bukkit.entity.EntityType.SHULKER));
        assertFalse(EspadaCelestita.delEnd(org.bukkit.World.Environment.NORMAL, org.bukkit.entity.EntityType.ZOMBIE));
        assertFalse(EspadaCelestita.delEnd(org.bukkit.World.Environment.NETHER, org.bukkit.entity.EntityType.BLAZE));
        assertEquals(5000, Bosses.ReyEnderBoss.HEALTH);
    }
}
