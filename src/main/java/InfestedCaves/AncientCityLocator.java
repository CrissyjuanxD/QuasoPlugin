package InfestedCaves;

import java.util.Random;

public final class AncientCityLocator {

    public static final int SIZE_X = 222;
    public static final int SIZE_Y = 37;
    public static final int SIZE_Z = 230;

    public static final int BASE_Y = -59;

    public static final int CLEAR_RADIUS = Math.max(SIZE_X, SIZE_Z) / 2 + 10;

    public static final int CLEAR_TRANSITION = 18;

    public static final int CLEAR_HEIGHT_PADDING = 0;

    private static final int MIN_DIST_TO_SPAWN = 500;

    public static final int CELL_SIZE = 512;

    private static final int CHANCE = 6;

    private AncientCityLocator() {}

    public static final class CityInfo {
        public final long cellX;
        public final long cellZ;

        public final int originX;
        public final int originY;
        public final int originZ;

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

        public int minY() { return originY; }

        public int maxY() { return originY + SIZE_Y - 1; }
    }

    // El mundo se divide en celdas de 512x512 y cada una tiene 1 en 6 de tener una Ancient City (solo con la seed)
    public static CityInfo getCityForCell(long worldSeed, long cellX, long cellZ) {
        int cellCenterX = (int)(cellX * CELL_SIZE + CELL_SIZE / 2);
        int cellCenterZ = (int)(cellZ * CELL_SIZE + CELL_SIZE / 2);

        double distToSpawn = Math.sqrt((double) cellCenterX * cellCenterX
                + (double) cellCenterZ * cellCenterZ);
        if (distToSpawn < MIN_DIST_TO_SPAWN) return null;

        Random rng = new Random(worldSeed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L));
        if (rng.nextInt(CHANCE) != 0) return null;

        int originX = cellCenterX - SIZE_X / 2;
        int originZ = cellCenterZ - SIZE_Z / 2;

        return new CityInfo(cellX, cellZ,
                originX, BASE_Y, originZ,
                cellCenterX, cellCenterZ);
    }

    public static long cellOf(int blockCoord) {
        return Math.floorDiv(blockCoord, CELL_SIZE);
    }

    // Revisa la celda actual y las 8 de alrededor por si hay una ciudad que afecte a ese bloque
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

    // 1 dentro del radio de la ciudad y baja hasta 0 en la transición; sirve para vaciar la cueva donde va la ciudad
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