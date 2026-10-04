package InfestedCaves;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Reparte los 4 biomas en regiones grandes: celdas de Voronoi de ~560 bloques con los bordes doblados por un noise.
// Antes salía del signo de 2 noise y donde los dos cruzaban el cero quedaban tiras finas y microbiomas
public final class WardenBiomeMap {

    private static final Map<Long, WardenBiomeMap> CACHE = new ConcurrentHashMap<>();

    private static final double CELL = 560;
    // Cuánto se corre el centro de cada región dentro de su celda; con más de 0.35 salen regiones finitas y estiradas
    private static final double JITTER = 0.32;
    // Cuánto se doblan los bordes (amplitud * escala < 0.35 para que no se formen islas sueltas)
    private static final double WARP = 105;
    private static final double WARP_SCALE = 1.0 / 330.0;
    // Ancho de la mezcla entre biomas: a 22 bloques del borde el otro bioma pesa 13%, a 44 bloques 2%
    private static final double BLEND = 22;

    private final long seed;
    private final SimplexOctaveGenerator warpX;
    private final SimplexOctaveGenerator warpZ;
    private final SimplexOctaveGenerator dither;
    // La región donde cae el 0 0 siempre es Caverna Sculk (ahí está la build con el portal de salida)
    private final long spawnCellX;
    private final long spawnCellZ;

    private WardenBiomeMap(long seed) {
        this.seed = seed;
        warpX = new SimplexOctaveGenerator(new Random(seed ^ 0x2F6B1D35L), 2);
        warpX.setScale(WARP_SCALE);
        warpZ = new SimplexOctaveGenerator(new Random(seed ^ 0x7C15A9E3L), 2);
        warpZ.setScale(WARP_SCALE);
        dither = new SimplexOctaveGenerator(new Random(seed + 14), 2);
        dither.setScale(1.0 / 9);

        long[] cell = nearestCell(warpX.noise(0, 0, 0.5, 0.5, true) * WARP, warpZ.noise(0, 0, 0.5, 0.5, true) * WARP);
        spawnCellX = cell[0];
        spawnCellZ = cell[1];
    }

    private long[] nearestCell(double px, double pz) {
        long cellX = (long) Math.floor(px / CELL);
        long cellZ = (long) Math.floor(pz / CELL);
        long[] best = {cellX, cellZ};
        double bestDistance = Double.MAX_VALUE;
        for (long i = cellX - 1; i <= cellX + 1; i++) {
            for (long j = cellZ - 1; j <= cellZ + 1; j++) {
                double d = Math.hypot(px - centerX(i, j), pz - centerZ(i, j));
                if (d < bestDistance) {
                    bestDistance = d;
                    best = new long[]{i, j};
                }
            }
        }
        return best;
    }

    private double centerX(long i, long j) {
        return (i + 0.5 + JITTER * (unit(hash(seed, i, j)) * 2 - 1)) * CELL;
    }

    private double centerZ(long i, long j) {
        return (j + 0.5 + JITTER * (unit(hash(seed + 1, i, j)) * 2 - 1)) * CELL;
    }

    private int cellBiome(long i, long j) {
        if (i == spawnCellX && j == spawnCellZ) return WardenBiome.CAVERNA_SCULK.ordinal();
        return (int) Math.floorMod(hash(seed + 2, i, j) >>> 33, 4L);
    }

    public static WardenBiomeMap forSeed(long seed) {
        return CACHE.computeIfAbsent(seed, WardenBiomeMap::new);
    }

    // Peso de cada bioma en la columna (suman 1): sale de la distancia al centro más cercano de cada bioma
    public double[] weights(int x, int z) {
        double px = x + warpX.noise(x, z, 0.5, 0.5, true) * WARP;
        double pz = z + warpZ.noise(x, z, 0.5, 0.5, true) * WARP;
        long cellX = (long) Math.floor(px / CELL);
        long cellZ = (long) Math.floor(pz / CELL);

        double[] distance = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        for (long i = cellX - 1; i <= cellX + 1; i++) {
            for (long j = cellZ - 1; j <= cellZ + 1; j++) {
                int biome = cellBiome(i, j);
                double d = Math.hypot(px - centerX(i, j), pz - centerZ(i, j));
                if (d < distance[biome]) distance[biome] = d;
            }
        }

        double nearest = Math.min(Math.min(distance[0], distance[1]), Math.min(distance[2], distance[3]));
        double[] w = new double[4];
        double sum = 0;
        for (int b = 0; b < 4; b++) {
            if (distance[b] == Double.MAX_VALUE) continue;
            w[b] = Math.exp(-(distance[b] - nearest) / BLEND);
            sum += w[b];
        }
        for (int b = 0; b < 4; b++) w[b] /= sum;

        double force = forcedSculk(x, z);
        if (force > 0) {
            for (int b = 0; b < 4; b++) w[b] *= 1 - force;
            w[WardenBiome.CAVERNA_SCULK.ordinal()] += force;
        }
        return w;
    }

    // El bioma de la columna. En la franja de mezcla entre dos biomas sale uno u otro en manchas (así el cambio de
    // bloques no es una línea recta) y la mancha se elige por cuadros de 4x4, igual que guarda los biomas el juego:
    // así los bloques y el bioma con el que spawnean los mobs siempre coinciden
    public WardenBiome biomeAt(int x, int z) {
        int qx = x & ~3;
        int qz = z & ~3;
        double[] w = weights(qx, qz);
        double sum = 0;
        for (double weight : w) if (weight >= 0.2) sum += weight;
        double r = (dither.noise(qx, qz, 0.5, 0.5, true) + 1) / 2 * sum;
        double total = 0;
        for (int b = 0; b < 4; b++) {
            if (w[b] < 0.2) continue;
            total += w[b];
            if (r <= total) return WardenBiome.values()[b];
        }
        return dominant(w);
    }

    public static WardenBiome dominant(double[] w) {
        int best = 0;
        for (int i = 1; i < w.length; i++) {
            if (w[i] > w[best]) best = i;
        }
        return WardenBiome.values()[best];
    }

    // Margen mínimo de Sculk alrededor del spawn por si el 0 0 cae cerca del borde de su región
    private static double forcedSculk(int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        if (d <= 130) return 1.0;
        if (d >= 210) return 0.0;
        double t = 1.0 - (d - 130) / 80;
        return t * t * (3 - 2 * t);
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
