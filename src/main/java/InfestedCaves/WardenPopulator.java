package InfestedCaves;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WardenPopulator extends BlockPopulator {
    private final JavaPlugin plugin;
    private static final Set<ChunkCoords> chunks = ConcurrentHashMap.newKeySet();
    private static final Set<ChunkCoords> unpopulatedChunks = ConcurrentHashMap.newKeySet();

    public WardenPopulator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        ChunkCoords chunkCoordinates = new ChunkCoords(chunkX, chunkZ);

        if (!chunks.contains(chunkCoordinates)) {
            chunks.add(chunkCoordinates);
            unpopulatedChunks.add(chunkCoordinates);
        }

        for (ChunkCoords unpopulatedChunk : unpopulatedChunks.toArray(new ChunkCoords[0])) {
            if (areAllNeighborsLoaded(unpopulatedChunk)) {
                actuallyPopulate(world, random, world.getChunkAt(unpopulatedChunk.x, unpopulatedChunk.z));
                unpopulatedChunks.remove(unpopulatedChunk);
            }
        }
    }

    private boolean areAllNeighborsLoaded(ChunkCoords coords) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                if (!chunks.contains(coords.offset(x, z))) return false;
            }
        }
        return true;
    }

    private void actuallyPopulate(World world, Random random, Chunk chunk) {
        int attempts = 3;

        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);

            for (int y = -50; y < 100; y += 3) {
                if (isValidSoil(chunk.getBlock(x, y, z).getType()) &&
                        chunk.getBlock(x, y + 1, z).getType() == Material.AIR) {

                    Location loc = chunk.getBlock(x, y + 1, z).getLocation();
                    generateCustomTree(world, loc, random);
                    break;
                }
            }
        }
    }

    private boolean isValidSoil(Material mat) {
        return mat == Material.SCULK ||
                mat == Material.CRYING_OBSIDIAN ||
                mat == Material.SCULK_CATALYST ||
                mat == Material.COARSE_DIRT;
    }

    /**
     * FIX #6: El bug del "árbol de una sola valla" ocurría porque BlockChangeDelegate.isEmpty()
     * consultaba world.getBlockAt(), que devuelve el estado REAL del mundo antes de que el
     * generador de chorus_plant hubiera colocado los bloques del árbol. Así, el generador
     * pensaba que su propio tronco era aire y no podía expandirse.
     *
     * Solución: mantener un mapa interno (placedBlocks) donde registramos cada bloque que
     * el delegate coloca. isEmpty() consulta primero ese mapa, y solo si no está ahí mira
     * el mundo real. Esto le da al generador de Chorus visibilidad de su propio trabajo
     * en curso, igual que lo haría en un mundo real donde los bloques ya existen.
     */
    private void generateCustomTree(World world, Location location, Random random) {
        Material originalSoil = location.clone().subtract(0, 1, 0).getBlock().getType();
        location.clone().subtract(0, 1, 0).getBlock().setType(Material.END_STONE);

        // Mapa temporal: clave = "x,y,z" → material colocado por este delegate
        Map<String, Material> placedBlocks = new HashMap<>();

        world.generateTree(location, TreeType.CHORUS_PLANT, new BlockChangeDelegate() {
            @Override
            public boolean setBlockData(int x, int y, int z, @NotNull BlockData blockData) {
                Material mat = blockData.getMaterial();

                if (mat == Material.CHORUS_PLANT) {
                    world.getBlockAt(x, y, z).setType(Material.WARPED_FENCE);
                    placedBlocks.put(x + "," + y + "," + z, Material.WARPED_FENCE);
                } else if (mat == Material.CHORUS_FLOWER) {
                    world.getBlockAt(x, y, z).setType(Material.VERDANT_FROGLIGHT);
                    placedBlocks.put(x + "," + y + "," + z, Material.VERDANT_FROGLIGHT);
                } else if (mat == Material.END_STONE) {
                    // El generador de chorus a veces coloca END_STONE como base: lo ignoramos
                    placedBlocks.put(x + "," + y + "," + z, mat);
                } else {
                    // Aire u otros: registrar igualmente para que isEmpty() sea correcto
                    placedBlocks.put(x + "," + y + "," + z, Material.AIR);
                }
                return true;
            }

            @Override
            public @NotNull BlockData getBlockData(int x, int y, int z) {
                Material placed = placedBlocks.get(x + "," + y + "," + z);
                if (placed != null) {
                    return placed.createBlockData();
                }
                return world.getBlockAt(x, y, z).getBlockData();
            }

            @Override
            public int getHeight() { return 256; }

            @Override
            public boolean isEmpty(int x, int y, int z) {
                // FIX: comprobamos primero el mapa interno del árbol en construcción
                Material placed = placedBlocks.get(x + "," + y + "," + z);
                if (placed != null) {
                    return placed == Material.AIR;
                }
                return world.getBlockAt(x, y, z).getType() == Material.AIR;
            }
        });

        if (originalSoil != Material.AIR && originalSoil != Material.END_STONE) {
            location.clone().subtract(0, 1, 0).getBlock().setType(originalSoil);
        }
    }

    private static class ChunkCoords {
        public final int x;
        public final int z;

        public ChunkCoords(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public ChunkCoords offset(int dx, int dz) {
            return new ChunkCoords(x + dx, z + dz);
        }

        @Override
        public int hashCode() {
            return (x + z) * (x + z + 1) / 2 + x;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkCoords other = (ChunkCoords) obj;
            return x == other.x && z == other.z;
        }
    }
}