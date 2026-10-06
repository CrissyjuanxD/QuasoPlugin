package BloodMoon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SafeSpawnFinderTest {
    private World world;
    private Block ground;
    private WorldBorder border;
    private SafeSpawnFinder finder;
    private final SafeSpawnFinder.Size zombie = new SafeSpawnFinder.Size(0.6, 1.95);
    @BeforeEach void setup() {
        world = mock(World.class);
        border = mock(WorldBorder.class);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any(Location.class))).thenReturn(true);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(63);
        ground = mock(Block.class);
        when(ground.getType()).thenReturn(Material.STONE);
        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(air.isPassable()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call -> (int) call.getArgument(1) <= 63 ? ground : air);
        finder = new SafeSpawnFinder(new Random(123));
    }
    @Test void mobsAppearAboveTheHighestBlockAndCenteredInsteadOfInsideIt() {
        Location location = finder.find(new Location(world, 0, 64, 0), 12, zombie).orElseThrow();
        assertEquals(64, location.getY());
        assertEquals(0.5, location.getX() - Math.floor(location.getX()));
        assertEquals(0.5, location.getZ() - Math.floor(location.getZ()));
    }
    @Test void lowCeilingsRejectTallMobsButAllowShortMobs() {
        BoundingBox ceiling = new BoundingBox(0, 66, 0, 1, 67, 1);
        when(world.hasCollisionsIn(any())).thenAnswer(call -> ceiling.overlaps(call.getArgument(0)));
        Location spot = new Location(world, 0.5, 64, 0.5);
        assertTrue(SafeSpawnFinder.isSafe(spot, zombie));
        assertFalse(SafeSpawnFinder.isSafe(spot, new SafeSpawnFinder.Size(0.6, 2.9)));
    }
    @Test void spiderWidthCannotClipAnAdjacentWall() {
        BoundingBox wall = new BoundingBox(1, 64, 0, 2, 67, 1);
        when(world.hasCollisionsIn(any())).thenAnswer(call -> wall.overlaps(call.getArgument(0)));
        Location spot = new Location(world, 0.5, 64, 0.5);
        assertTrue(SafeSpawnFinder.isSafe(spot, zombie));
        assertFalse(SafeSpawnFinder.isSafe(spot, new SafeSpawnFinder.Size(1.4, 0.9)));
    }
    @Test void waterAndHazardousGroundAreRejected() {
        Location spot = new Location(world, 0.5, 64, 0.5);
        for (Material type : new Material[]{Material.WATER, Material.LAVA, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.POWDER_SNOW}) {
            when(ground.getType()).thenReturn(type);
            assertFalse(SafeSpawnFinder.isSafe(spot, zombie), type.name());
        }
    }
    @Test void chunksOutsideTheLoadedAreaAreNeverReadOrLoaded() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        assertTrue(finder.find(new Location(world, 0, 64, 0), 12, zombie).isEmpty());
        verify(world, never()).getHighestBlockYAt(anyInt(), anyInt());
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }
    @Test void borderAndBuildHeightAreRespected() {
        assertFalse(SafeSpawnFinder.isSafe(new Location(world, 0.5, 319, 0.5), zombie));
        when(border.isInside(any(Location.class))).thenReturn(false);
        assertTrue(finder.find(new Location(world, 0, 64, 0), 12, zombie).isEmpty());
    }
    @Test void theEntireBodyMustFitInLoadedChunks() {
        when(world.isChunkLoaded(eq(1), anyInt())).thenReturn(false);
        assertFalse(SafeSpawnFinder.isSafe(new Location(world, 15.5, 64, 0.5), new SafeSpawnFinder.Size(1.4, 0.9)));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }
}
