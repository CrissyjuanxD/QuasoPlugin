package SistemaTumbas;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarcadorTumbaTest {

    // Yaw 0 mira al sur (+Z): la tumba al sur va adelante, al oeste a la derecha, al este a la izquierda y al norte atrás
    @Test
    void theArrowPointsToTheGraveFromWhereThePlayerLooks() {
        assertEquals("↑", MarcadorTumba.flecha(0, 0, 10));
        assertEquals("→", MarcadorTumba.flecha(0, -10, 0));
        assertEquals("←", MarcadorTumba.flecha(0, 10, 0));
        assertEquals("↓", MarcadorTumba.flecha(0, 0, -10));
        assertEquals("↗", MarcadorTumba.flecha(0, -10, 10));
        // Mirando al norte (180) la tumba al norte va adelante; el yaw puede venir negativo o pasado de 360
        assertEquals("↑", MarcadorTumba.flecha(180, 0, -10));
        assertEquals("↑", MarcadorTumba.flecha(-180, 0, -10));
        assertEquals("→", MarcadorTumba.flecha(450, 0, -10));
    }

    @Test
    void theBarShowsTheMetersOrTheWorldOfTheGrave() {
        World overworld = mock(World.class);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        Location tumba = new Location(overworld, 0, 64, 500);
        String titulo = MarcadorTumba.titulo("Última Muerte", new Location(overworld, 0, 64, 0, 0, 0), tumba);
        assertTrue(titulo.endsWith("Última Muerte: 500mt §l↑"), titulo);
        assertTrue(MarcadorTumba.titulo("Última Muerte", new Location(overworld, 0, 64, 499), tumba).endsWith("Última Muerte: aquí"));
        assertTrue(MarcadorTumba.titulo("Muerte de las 13:20", new Location(overworld, 0, 64, 0), new Location(nether, 0, 64, 0))
                .endsWith("Muerte de las 13:20: en el Nether"));
    }
}
