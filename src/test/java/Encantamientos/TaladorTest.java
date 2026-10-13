package Encantamientos;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class TaladorTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    // Un mundo de mentira: lo que no está en el mapa es aire
    private static final class Mundo {
        final Map<String, Material> bloques = new HashMap<>();
        final Map<String, Block> cache = new HashMap<>();
        final World world = mock(World.class);

        Mundo(String nombre, World.Environment entorno) {
            when(world.getName()).thenReturn(nombre);
            when(world.getEnvironment()).thenReturn(entorno);
        }

        void poner(int x, int y, int z, Material tipo) { bloques.put(x + "," + y + "," + z, tipo); }

        Block block(int x, int y, int z) {
            return cache.computeIfAbsent(x + "," + y + "," + z, k -> {
                Block b = mock(Block.class);
                Material tipo = bloques.getOrDefault(k, Material.AIR);
                when(b.getType()).thenReturn(tipo);
                when(b.getY()).thenReturn(y);
                when(b.getWorld()).thenReturn(world);
                BlockData data = tipo.name().endsWith("_LEAVES") ? mock(Leaves.class) : mock(BlockData.class);
                when(b.getBlockData()).thenReturn(data);
                when(b.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                        block(x + (int) call.getArgument(0), y + (int) call.getArgument(1), z + (int) call.getArgument(2)));
                return b;
            });
        }
    }

    private static Mundo roble(String nombre, World.Environment entorno, boolean hojas) {
        Mundo mundo = new Mundo(nombre, entorno);
        for (int y = -1; y <= 4; y++) mundo.poner(0, y, 0, Material.OAK_LOG);
        mundo.poner(1, 3, 0, Material.OAK_LOG);
        mundo.poner(5, 0, 0, Material.OAK_LOG);
        if (hojas) mundo.poner(0, 5, 0, Material.OAK_LEAVES);
        return mundo;
    }

    // Cae todo lo que está arriba y pegado al tronco que se rompió, nada de abajo ni de otro árbol
    @Test void aNaturalTreeFallsFromTheBrokenLogUp() {
        Mundo mundo = roble("world", World.Environment.NORMAL, true);
        List<Block> arbol = Talador.arbol(mundo.block(0, 0, 0), 24);
        assertEquals(5, arbol.size());
        assertTrue(arbol.contains(mundo.block(1, 3, 0)));
        assertFalse(arbol.contains(mundo.block(0, -1, 0)));
        assertFalse(arbol.contains(mundo.block(5, 0, 0)));
        assertEquals(2, Talador.arbol(mundo.block(0, 0, 0), 2).size());
    }

    // Sin hojas naturales es una construcción de troncos y no se cae; en el End los árboles tienen copas de lana o vidrio
    @Test void logBuildingsStayUpButEndTreesFall() {
        assertTrue(Talador.arbol(roble("world", World.Environment.NORMAL, false).block(0, 0, 0), 24).isEmpty());
        assertEquals(5, Talador.arbol(roble("world_the_end", World.Environment.THE_END, false).block(0, 0, 0), 24).size());
        assertTrue(Talador.esTronco(Material.STRIPPED_BIRCH_LOG));
        assertTrue(Talador.esTronco(Material.CRIMSON_STEM));
        assertFalse(Talador.esTronco(Material.OAK_PLANKS));
    }
}
