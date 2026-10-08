package EndBiomes;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Reparte las islas de afuera del End en regiones (celdas de ~450 bloques con los bordes doblados): unas quedan vanilla
// y las otras son Bosque Prismático, Páramo Marchito o Picos Helados. La isla del dragón y el vacío de alrededor no
// se tocan
public final class EndBiomeMap {

    public enum Zone { VANILLA, PRISMATICO, MARCHITO, HIELO }

    private static final Map<Long, EndBiomeMap> CACHE = new ConcurrentHashMap<>();

    private static final double CELL = 448;
    private static final double JITTER = 0.32;
    private static final double WARP = 70;
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
        return zoneForRoll(roll);
    }

    // Qué le toca a una región con una tirada de 0 a 99: 26% vanilla, 27% Prismático, 27% Páramo y 20% Hielo
    static Zone zoneForRoll(int roll) {
        if (roll < 26) return Zone.VANILLA;
        if (roll < 53) return Zone.PRISMATICO;
        return roll < 80 ? Zone.MARCHITO : Zone.HIELO;
    }

    // El bioma que va en esa columna (null si queda vanilla)
    public EndBiome biomeAt(int x, int z) {
        return switch (zoneAt(x, z)) {
            case VANILLA -> null;
            case MARCHITO -> EndBiome.PARAMO_MARCHITO;
            case HIELO -> EndBiome.PICOS_HELADOS;
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
