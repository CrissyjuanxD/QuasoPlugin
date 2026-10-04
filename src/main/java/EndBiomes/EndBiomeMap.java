package EndBiomes;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Reparte las islas de afuera del End en regiones grandes (celdas de ~640 bloques con los bordes doblados): unas
// quedan vanilla y las otras son Bosque Prismático o Páramo Marchito. La isla del dragón y el vacío de alrededor no
// se tocan
public final class EndBiomeMap {

    public enum Zone { VANILLA, PRISMATICO, MARCHITO }

    private static final Map<Long, EndBiomeMap> CACHE = new ConcurrentHashMap<>();

    private static final double CELL = 640;
    private static final double JITTER = 0.32;
    private static final double WARP = 90;
    private static final double WARP_SCALE = 1.0 / 300.0;
    // Manchas de un color dentro del Bosque Prismático
    private static final double COLOR_CELL = 44;
    public static final int MAIN_ISLAND_RADIUS = 1100;

    private final long seed;
    private final SimplexOctaveGenerator warpX;
    private final SimplexOctaveGenerator warpZ;

    private EndBiomeMap(long seed) {
        this.seed = seed;
        warpX = new SimplexOctaveGenerator(new Random(seed ^ 0x51ED270BL), 2);
        warpX.setScale(WARP_SCALE);
        warpZ = new SimplexOctaveGenerator(new Random(seed ^ 0x1B873593L), 2);
        warpZ.setScale(WARP_SCALE);
    }

    public static EndBiomeMap forSeed(long seed) {
        return CACHE.computeIfAbsent(seed, EndBiomeMap::new);
    }

    public Zone zoneAt(int x, int z) {
        if ((long) x * x + (long) z * z < (long) MAIN_ISLAND_RADIUS * MAIN_ISLAND_RADIUS) return Zone.VANILLA;
        double px = x + warpX.noise(x, z, 0.5, 0.5, true) * WARP;
        double pz = z + warpZ.noise(x, z, 0.5, 0.5, true) * WARP;
        long[] cell = nearest(px, pz, CELL, seed);
        int roll = (int) Math.floorMod(hash(seed + 7, cell[0], cell[1]) >>> 33, 100L);
        if (roll < 34) return Zone.VANILLA;
        return roll < 67 ? Zone.PRISMATICO : Zone.MARCHITO;
    }

    // El bioma que va en esa columna (null si queda vanilla)
    public EndBiome biomeAt(int x, int z) {
        return switch (zoneAt(x, z)) {
            case VANILLA -> null;
            case MARCHITO -> EndBiome.PARAMO_MARCHITO;
            case PRISMATICO -> {
                long[] cell = nearest(x, z, COLOR_CELL, seed + 11);
                yield EndBiome.PRISMATIC[(int) Math.floorMod(hash(seed + 13, cell[0], cell[1]) >>> 33, (long) EndBiome.PRISMATIC.length)];
            }
        };
    }

    private static long[] nearest(double px, double pz, double size, long seed) {
        long cellX = (long) Math.floor(px / size);
        long cellZ = (long) Math.floor(pz / size);
        long[] best = {cellX, cellZ};
        double bestDistance = Double.MAX_VALUE;
        for (long i = cellX - 1; i <= cellX + 1; i++) {
            for (long j = cellZ - 1; j <= cellZ + 1; j++) {
                double cx = (i + 0.5 + JITTER * (unit(hash(seed, i, j)) * 2 - 1)) * size;
                double cz = (j + 0.5 + JITTER * (unit(hash(seed + 1, i, j)) * 2 - 1)) * size;
                double d = (px - cx) * (px - cx) + (pz - cz) * (pz - cz);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new long[]{i, j};
                }
            }
        }
        return best;
    }

    private static long hash(long seed, long x, long z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
