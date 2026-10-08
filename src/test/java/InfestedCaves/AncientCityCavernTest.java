package InfestedCaves;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AncientCityCavernTest {

    private static final long SEED = 12345L;

    private static AncientCityLocator.CityInfo city() {
        for (int rx = -6; rx <= 6; rx++) {
            for (int rz = -6; rz <= 6; rz++) {
                AncientCityLocator.CityInfo city = AncientCityLocator.getCityForRegion(SEED, rx, rz);
                if (city != null) return city;
            }
        }
        throw new AssertionError("no hay ciudades cerca");
    }

    // El Infested Warden solo cuenta a los que están en la caverna, no a los que están arriba en el terreno
    @Test
    void onlyPlayersInsideTheCityCavernCount() {
        AncientCityLocator.CityInfo city = city();
        int x = city.centerX + 20, z = city.centerZ - 15;
        assertTrue(WardenGenerator.inCityCavern(SEED, x, AncientCityLocator.MIN_Y + 2, z));
        assertTrue(WardenGenerator.inCityCavern(SEED, x, AncientCityLocator.MAX_Y, z));
        // Arriba del techo de roca (de 10 a 16 bloques más la cúpula) ya es el terreno de afuera
        assertFalse(WardenGenerator.inCityCavern(SEED, x, AncientCityLocator.MAX_Y + 40, z));
        assertFalse(WardenGenerator.inCityCavern(SEED, city.centerX + 400, AncientCityLocator.MIN_Y + 2, city.centerZ + 400));
    }
}
