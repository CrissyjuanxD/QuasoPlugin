package Encantamientos;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import Managers.QuasoDatapackFiles;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EncantamientosTrabajoTest {

    @Test
    void everyEnchantmentHasItsDatapackFileWithTheSameMaxLevel() throws Exception {
        for (QuasoEnchant enchant : QuasoEnchant.values()) {
            String file = "data/quaso/enchantment/" + enchant.id() + ".json";
            assertTrue(QuasoDatapackFiles.files().contains(file), file);
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("datapack/QuasoPlugin/" + file)) {
                assertNotNull(in, file);
                JsonObject json = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(enchant.maxLevel(), json.get("max_level").getAsInt(), file);
            }
        }
    }

    @Test
    void theJobEnchantmentsGrowWithTheirLevel() {
        assertArrayEquals(new int[]{24, 64, 160}, Talador.MAXIMO);
        assertEquals(2, VigorDeBatalla.curacion(1));
        assertEquals(6, VigorDeBatalla.curacion(3));

        assertEquals(Material.WHEAT_SEEDS, Cosecha.semilla(Material.WHEAT));
        assertEquals(Material.CARROT, Cosecha.semilla(Material.CARROTS));
        assertEquals(Material.NETHER_WART, Cosecha.semilla(Material.NETHER_WART));
        assertEquals(Material.COCOA_BEANS, Cosecha.semilla(Material.COCOA));
        assertNull(Cosecha.semilla(Material.MELON));
        assertNull(Cosecha.semilla(Material.SUGAR_CANE));
    }
}
