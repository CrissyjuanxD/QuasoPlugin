package EndBiomes;

import org.bukkit.Material;
import org.bukkit.boss.DragonBattle;
import org.bukkit.event.block.BlockIgniteEvent;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EndDragonTest {

    @Test
    void onlyDragonsBornFromTheRitualAreKept() {
        // Cuando sale el dragón del ritual el juego ya no está en ninguna fase: cuenta haberlo visto hace poco
        assertTrue(EndDragon.deRitual(DragonBattle.RespawnPhase.NONE, 1));
        assertTrue(EndDragon.deRitual(DragonBattle.RespawnPhase.SUMMONING_DRAGON, 5000));
        assertTrue(EndDragon.deRitual(DragonBattle.RespawnPhase.END, 5000));
        // El que crea el juego al entrar por primera vez no tiene ritual
        assertFalse(EndDragon.deRitual(DragonBattle.RespawnPhase.NONE, 1000));
    }

    @Test
    void fireDoesNotSpreadInTheEnd() {
        assertTrue(EndFire.permitido(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL));
        assertTrue(EndFire.permitido(BlockIgniteEvent.IgniteCause.ENDER_CRYSTAL));
        for (BlockIgniteEvent.IgniteCause cause : new BlockIgniteEvent.IgniteCause[]{
                BlockIgniteEvent.IgniteCause.SPREAD, BlockIgniteEvent.IgniteCause.FIREBALL,
                BlockIgniteEvent.IgniteCause.LAVA, BlockIgniteEvent.IgniteCause.LIGHTNING}) {
            assertFalse(EndFire.permitido(cause), cause.name());
        }
    }

    @Test
    void theTenTowersAreInTheVanillaCircle() {
        int[][] towers = EndIslaPrincipal.torres();
        assertEquals(10, towers.length);
        assertArrayEquals(new int[]{42, 0}, towers[0]);
        for (int[] t : towers) {
            double d = Math.hypot(t[0], t[1]);
            assertTrue(d > 40.5 && d < 43, d + "");
        }
    }

    @Test
    void thePlazaHasAColoredGlassRing() {
        Random random = new Random(1);
        assertEquals(Material.POLISHED_BLACKSTONE_BRICKS, EndIslaPrincipal.piso(4.5, 0, random));
        assertEquals(Material.END_STONE_BRICKS, EndIslaPrincipal.piso(10, 1, random));
        for (double a = -Math.PI; a < Math.PI; a += 0.3) {
            assertTrue(EndIslaPrincipal.piso(7, a, random).name().endsWith("_STAINED_GLASS"));
            Material ring = EndIslaPrincipal.piso(6, a, random);
            assertTrue(ring == Material.OBSIDIAN || ring == Material.CRYING_OBSIDIAN);
        }
    }

    @Test
    void eachForestPatchFavorsItsColorsButTreesComeInAllColors() {
        for (EndBiome biome : EndBiome.PRISMATIC) assertTrue(EndTrees.tono(biome).length > 0, biome.name());
        Random random = new Random(7);
        java.util.Set<EndTrees.Copa> seen = java.util.EnumSet.noneOf(EndTrees.Copa.class);
        int pink = 0;
        for (int i = 0; i < 2000; i++) {
            EndTrees.Copa copa = EndTrees.color(EndBiome.PRISMATICO_ROSA, random);
            seen.add(copa);
            if (copa == EndTrees.Copa.ROSA || copa == EndTrees.Copa.MAGENTA) pink++;
        }
        assertEquals(EndTrees.Copa.values().length, seen.size());
        assertTrue(pink > 1000, "rosa=" + pink);
    }
}
