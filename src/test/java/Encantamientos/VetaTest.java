package Encantamientos;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class VetaTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    private static final class Mundo {
        final Map<String, Material> bloques = new HashMap<>();
        final Map<String, Block> cache = new HashMap<>();
        final World world = mock(World.class);

        Mundo(String nombre) {
            when(world.getName()).thenReturn(nombre);
        }

        void poner(int x, int y, int z, Material tipo) { bloques.put(x + "," + y + "," + z, tipo); }

        Block block(int x, int y, int z) {
            return cache.computeIfAbsent(x + "," + y + "," + z, k -> {
                Block b = mock(Block.class);
                when(b.getType()).thenReturn(bloques.getOrDefault(k, Material.STONE));
                when(b.getWorld()).thenReturn(world);
                when(b.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                        block(x + (int) call.getArgument(0), y + (int) call.getArgument(1), z + (int) call.getArgument(2)));
                return b;
            });
        }
    }

    // La veta entera del mismo mineral (también la parte de deepslate y en diagonal), sin el primero ni otros minerales
    @Test void theWholeVeinOfTheSameOreBreaks() {
        Mundo mundo = new Mundo("world");
        mundo.poner(0, 0, 0, Material.IRON_ORE);
        mundo.poner(1, 0, 0, Material.IRON_ORE);
        mundo.poner(1, -1, 1, Material.DEEPSLATE_IRON_ORE);
        mundo.poner(0, 1, 0, Material.IRON_ORE);
        mundo.poner(-1, 0, 0, Material.COAL_ORE);
        mundo.poner(3, 0, 0, Material.IRON_ORE);
        List<Block> veta = Veta.veta(mundo.block(0, 0, 0), 12);
        assertEquals(3, veta.size());
        assertFalse(veta.contains(mundo.block(-1, 0, 0)));
        assertFalse(veta.contains(mundo.block(3, 0, 0)));
        assertEquals(2, Veta.veta(mundo.block(0, 0, 0), 2).size());
        assertArrayEquals(new int[]{12, 32}, Veta.MAXIMO);
    }

    @Test void onlyOresCountAndTheDeepOresOnlyInTheWardenCave() {
        Mundo mundo = new Mundo("world");
        mundo.poner(0, 0, 0, Material.CYAN_GLAZED_TERRACOTTA);
        mundo.poner(1, 0, 0, Material.DEEPSLATE_DIAMOND_ORE);
        assertNull(Veta.familia(mundo.block(0, 0, 0)));
        assertNull(Veta.familia(mundo.block(5, 5, 5)));
        assertEquals("DIAMOND_ORE", Veta.familia(mundo.block(1, 0, 0)));

        Mundo cueva = new Mundo(QuasoPlugin.WORLD_NAME);
        cueva.poner(0, 0, 0, Material.CYAN_GLAZED_TERRACOTTA);
        assertEquals("CYAN_GLAZED_TERRACOTTA", Veta.familia(cueva.block(0, 0, 0)));
    }
}
