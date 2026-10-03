package InfestedCaves;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Las Ancient City de la dimensión son las vanilla. Acá se calcula dónde las va a poner el juego (misma cuenta
// que el structure_set "ancient_cities") para que el generador les vacíe la caverna antes
public final class AncientCityLocator {

    // Valores del structure_set vanilla: una ciudad por región de 24x24 chunks, separadas al menos 8
    private static final int SPACING = 24;
    private static final int SEPARATION = 8;
    private static final int SALT = 20083232;

    // Las piezas llegan hasta unos 130 bloques del centro (medido en un server de prueba)
    public static final int CLEAR_RADIUS = 134;
    public static final int CLEAR_TRANSITION = 18;

    // Las piezas van de Y -52 a -22 (el ancla está en -27). Abajo de MIN_Y queda piso firme y MAX_Y es el
    // techo de la caverna en los bordes; en el centro sube hasta 16 bloques más
    public static final int MIN_Y = -53;
    public static final int MAX_Y = -16;

    private static final int MIN_DIST_TO_SPAWN = 500;

    private static final Map<Long, Optional<CityInfo>> CACHE = new ConcurrentHashMap<>();

    private AncientCityLocator() {}

    public static final class CityInfo {
        public final int chunkX;
        public final int chunkZ;
        public final int centerX;
        public final int centerZ;

        CityInfo(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.centerX = chunkX << 4;
            this.centerZ = chunkZ << 4;
        }

        public int minY() { return MIN_Y; }

        public int maxY() { return MAX_Y; }
    }

    // La ciudad de una región o null. El chunk sale igual que en RandomSpreadStructurePlacement (spread lineal)
    // y además se descartan las que caen cerca del spawn o tocan el Abismo, que no tiene suelo
    public static CityInfo getCityForRegion(long seed, int regionX, int regionZ) {
        long key = seed ^ ((long) regionX << 32) ^ (regionZ & 0xFFFFFFFFL);
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(compute(seed, regionX, regionZ))).orElse(null);
    }

    private static CityInfo compute(long seed, int regionX, int regionZ) {
        Random random = new Random((long) regionX * 341873128712L + (long) regionZ * 132897987541L + seed + SALT);
        int chunkX = regionX * SPACING + random.nextInt(SPACING - SEPARATION);
        int chunkZ = regionZ * SPACING + random.nextInt(SPACING - SEPARATION);
        CityInfo info = new CityInfo(chunkX, chunkZ);

        if (Math.hypot(info.centerX, info.centerZ) < MIN_DIST_TO_SPAWN) return null;
        if (touchesAbyss(seed, info.centerX, info.centerZ)) return null;
        return info;
    }

    // El generador solo deja empezar estructuras en estos chunks, así nunca sale una ciudad sin su caverna
    public static boolean isStartChunk(long seed, int chunkX, int chunkZ) {
        CityInfo info = getCityForRegion(seed, Math.floorDiv(chunkX, SPACING), Math.floorDiv(chunkZ, SPACING));
        return info != null && info.chunkX == chunkX && info.chunkZ == chunkZ;
    }

    private static boolean touchesAbyss(long seed, int centerX, int centerZ) {
        WardenBiomeMap map = WardenBiomeMap.forSeed(seed);
        int r = CLEAR_RADIUS;
        int[][] points = {{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}};
        for (int[] p : points) {
            if (map.biomeAt(centerX + p[0], centerZ + p[1]) == WardenBiome.ABISMO_FLOTANTE) return true;
        }
        return false;
    }

    public static CityInfo findCityNear(long seed, int blockX, int blockZ) {
        return findCityNear(seed, blockX, blockZ, 0);
    }

    // Revisa la región del bloque y las de alrededor. Con margen: el generador busca desde el centro del chunk
    // y tiene que encontrar la ciudad aunque solo le toque una esquina
    public static CityInfo findCityNear(long seed, int blockX, int blockZ, double margin) {
        int regionX = Math.floorDiv(Math.floorDiv(blockX, 16), SPACING);
        int regionZ = Math.floorDiv(Math.floorDiv(blockZ, 16), SPACING);
        double maxDist = CLEAR_RADIUS + CLEAR_TRANSITION + margin;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                CityInfo info = getCityForRegion(seed, regionX + dx, regionZ + dz);
                if (info != null && Math.hypot(blockX - info.centerX, blockZ - info.centerZ) <= maxDist) return info;
            }
        }
        return null;
    }

    // 1 dentro del radio de la ciudad y baja hasta 0 en la transición
    public static double computeInfluence(CityInfo info, int x, int z) {
        double d = Math.hypot(x - info.centerX, z - info.centerZ);
        if (d <= CLEAR_RADIUS) return 1.0;
        if (d >= CLEAR_RADIUS + CLEAR_TRANSITION) return 0.0;
        return 1.0 - (d - CLEAR_RADIUS) / CLEAR_TRANSITION;
    }
}
