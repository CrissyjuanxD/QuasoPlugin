package InfestedCaves;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import Managers.QuasoDatapackFiles;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AncientCityLocatorTest {

    private static JsonObject json(String file) throws Exception {
        try (InputStream in = AncientCityLocatorTest.class.getClassLoader().getResourceAsStream("datapack/QuasoPlugin/" + file)) {
            assertNotNull(in, file);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // El locator tiene que hacer la misma cuenta que el structure_set, si no la caverna queda en otro lado
    @Test
    void theLocatorMatchesTheStructureSet() throws Exception {
        JsonObject set = json("data/quaso/worldgen/structure_set/ciudades_warden.json");
        JsonObject placement = set.getAsJsonObject("placement");
        assertEquals(AncientCityLocator.SPACING, placement.get("spacing").getAsInt());
        assertEquals(AncientCityLocator.SEPARATION, placement.get("separation").getAsInt());
        assertEquals(AncientCityLocator.SALT, placement.get("salt").getAsInt());
        assertEquals("quaso:" + AncientCityLocator.STRUCTURE_KEY,
                set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());

        // La ciudad solo sale en los biomas de la Warden Cave y el mundo normal queda con la Ancient City vanilla
        JsonObject structure = json("data/quaso/worldgen/structure/" + AncientCityLocator.STRUCTURE_KEY + ".json");
        assertEquals("#quaso:has_structure/ciudad_warden", structure.get("biomes").getAsString());
        assertEquals("minecraft:ancient_city/city_center", structure.get("start_pool").getAsString());
        List<String> files = QuasoDatapackFiles.files();
        assertTrue(files.contains("data/quaso/tags/worldgen/biome/has_structure/ciudad_warden.json"));
        assertTrue(files.contains("data/quaso/worldgen/structure_set/ciudades_warden.json"));
        assertFalse(files.contains("data/minecraft/tags/worldgen/biome/has_structure/ancient_city.json"));
    }

    // Entre dos ciudades siempre hay más de 500 bloques (antes podían quedar a 144)
    @Test
    void citiesAreAtLeast500BlocksApart() {
        for (long seed : new long[]{12345L, -987654321L, 42L}) {
            List<AncientCityLocator.CityInfo> cities = new ArrayList<>();
            for (int rx = -6; rx <= 6; rx++) {
                for (int rz = -6; rz <= 6; rz++) {
                    AncientCityLocator.CityInfo city = AncientCityLocator.getCityForRegion(seed, rx, rz);
                    if (city != null) cities.add(city);
                }
            }
            assertTrue(cities.size() > 50, "pocas ciudades: " + cities.size());
            for (AncientCityLocator.CityInfo a : cities) {
                for (AncientCityLocator.CityInfo b : cities) {
                    if (a == b) continue;
                    assertTrue(Math.hypot(a.centerX - b.centerX, a.centerZ - b.centerZ) >= 500,
                            a.centerX + "," + a.centerZ + " y " + b.centerX + "," + b.centerZ);
                }
            }
        }
    }
}
