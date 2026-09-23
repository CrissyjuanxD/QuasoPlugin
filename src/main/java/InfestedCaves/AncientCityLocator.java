package InfestedCaves;

import java.util.Random;

/**
 * Calcula de forma puramente determinista (a partir de la seed del mundo) dónde
 * caen las Ancient City en la grilla de celdas, SIN necesitar que ningún chunk
 * esté cargado.
 */
public final class AncientCityLocator {

    // Tamaño real según FAWE: 222 x 37 x 230
    public static final int SIZE_X = 222;
    public static final int SIZE_Y = 37;
    public static final int SIZE_Z = 230;

    // Y de pegado: -59 es la capa sculk base.
    // El schem se pega con su bloque inferior en -59, apoyado exactamente sobre el suelo.
    public static final int BASE_Y = -59;

    // Radio del hueco circular = mitad del lado más largo + margen justo.
    // SIZE_Z/2 = 115, +10 de margen = 125. Ajustado a lo real del schem.
    public static final int CLEAR_RADIUS = Math.max(SIZE_X, SIZE_Z) / 2 + 10; // 125

    // Transición suave: más ancha para que el borde sea orgánico, no un corte recto.
    public static final int CLEAR_TRANSITION = 18;

    // Sin padding vertical: el techo lo genera el noise de forma 100% natural.
    public static final int CLEAR_HEIGHT_PADDING = 0;

    // Distancia mínima al centro para no chocar con el templo de spawn
    private static final int MIN_DIST_TO_SPAWN = 500;

    // Tamaño de la grilla. Debe ser > 2*(CLEAR_RADIUS+CLEAR_TRANSITION) para que
    // dos ciudades vecinas no se solapen.
    public static final int CELL_SIZE = 512;

    private static final int CHANCE = 6;

    private AncientCityLocator() {}

    public static final class CityInfo {
        public final long cellX;
        public final long cellZ;

        /** Esquina NW del schem para WorldEdit .to(...) */
        public final int originX;
        public final int originY;
        public final int originZ;

        /** Centro geométrico del schem en X/Z — coincide con el centro del hueco circular */
        public final int centerX;
        public final int centerZ;

        CityInfo(long cellX, long cellZ,
                 int originX, int originY, int originZ,
                 int centerX, int centerZ) {
            this.cellX   = cellX;
            this.cellZ   = cellZ;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        /** Y inferior del schem (donde se apoya en el suelo) */
        public int minY() { return originY; }

        /** Y superior del schem — sin padding, para no limpiar el techo natural */
        public int maxY() { return originY + SIZE_Y - 1; }
    }

    /**
     * Decide de forma determinista si una celda contiene una Ancient City.
     * Devuelve null si no hay ciudad en esa celda.
     */
    public static CityInfo getCityForCell(long worldSeed, long cellX, long cellZ) {
        // Centro de la celda en coordenadas de bloque
        int cellCenterX = (int)(cellX * CELL_SIZE + CELL_SIZE / 2);
        int cellCenterZ = (int)(cellZ * CELL_SIZE + CELL_SIZE / 2);

        double distToSpawn = Math.sqrt((double) cellCenterX * cellCenterX
                + (double) cellCenterZ * cellCenterZ);
        if (distToSpawn < MIN_DIST_TO_SPAWN) return null;

        Random rng = new Random(worldSeed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L));
        if (rng.nextInt(CHANCE) != 0) return null;

        // FIX CENTRADO: la esquina del schem se calcula desde el centro de la celda
        // restando la mitad exacta del tamaño del schem → el schem queda centrado
        // en el mismo punto que el hueco circular.
        int originX = cellCenterX - SIZE_X / 2;
        int originZ = cellCenterZ - SIZE_Z / 2;

        return new CityInfo(cellX, cellZ,
                originX, BASE_Y, originZ,
                cellCenterX, cellCenterZ);
    }

    public static long cellOf(int blockCoord) {
        return Math.floorDiv(blockCoord, CELL_SIZE);
    }

    /**
     * Busca si la columna (blockX, blockZ) cae dentro del área de influencia
     * de alguna Ancient City (celda propia + 8 vecinas).
     */
    public static CityInfo findCityNear(long worldSeed, int blockX, int blockZ) {
        long ccx = cellOf(blockX);
        long ccz = cellOf(blockZ);
        double maxDist = CLEAR_RADIUS + CLEAR_TRANSITION;

        for (long dx = -1; dx <= 1; dx++) {
            for (long dz = -1; dz <= 1; dz++) {
                CityInfo info = getCityForCell(worldSeed, ccx + dx, ccz + dz);
                if (info == null) continue;
                if (distance(blockX, blockZ, info.centerX, info.centerZ) <= maxDist)
                    return info;
            }
        }
        return null;
    }

    /**
     * Influencia de limpieza circular (0.0–1.0).
     * 1.0 dentro de CLEAR_RADIUS, transición lineal hasta 0.0 en CLEAR_RADIUS+CLEAR_TRANSITION.
     */
    public static double computeInfluence(CityInfo info, int x, int z) {
        double d = distance(x, z, info.centerX, info.centerZ);
        if (d <= CLEAR_RADIUS)                      return 1.0;
        if (d >= CLEAR_RADIUS + CLEAR_TRANSITION)   return 0.0;
        return 1.0 - (d - CLEAR_RADIUS) / CLEAR_TRANSITION;
    }

    private static double distance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2, dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static long cellKey(long cellX, long cellZ) {
        return (cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }
}