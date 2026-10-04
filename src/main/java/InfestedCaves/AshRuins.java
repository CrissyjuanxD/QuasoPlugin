package InfestedCaves;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

// Mini estructuras de obsidiana de las Ruinas de Ceniza: lo que quedó de algo que se quemó hace mucho
final class AshRuins {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private AshRuins() {}

    // Elige una al azar; x, y, z es el primer bloque de aire arriba del suelo. Devuelve false si no entraba
    static boolean random(LimitedRegion region, Random r, int x, int y, int z) {
        return switch (r.nextInt(4)) {
            case 0 -> obelisk(region, r, x, y, z);
            case 1 -> ruinedPortal(region, r, x, y, z);
            case 2 -> spikes(region, r, x, y, z);
            default -> altar(region, r, x, y, z);
        };
    }

    // Obelisco: base de piedra negra pulida, aguja de obsidiana con bandas de obsidiana llorosa y fuego de almas arriba
    private static boolean obelisk(LimitedRegion region, Random r, int x, int y, int z) {
        int height = 7 + r.nextInt(6);
        if (!clear(region, x, y, z, 1, height + 2)) return false;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = dx != 0 && dz != 0;
                force(region, x + dx, y - 1, z + dz, corner ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        for (BlockFace face : HORIZONTAL) {
            set(region, x + face.getModX(), y, z + face.getModZ(), Material.POLISHED_BLACKSTONE_BRICK_STAIRS);
        }
        for (int i = 0; i < height; i++) {
            set(region, x, y + i, z, i % 3 == 2 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
        }
        set(region, x, y + height, z, Material.SOUL_SOIL);
        set(region, x, y + height + 1, z, Material.SOUL_FIRE);
        return true;
    }

    // Portal en ruinas: marco de obsidiana de 4x5 al que le faltan bloques, con obsidiana llorosa y pedazos caídos
    private static boolean ruinedPortal(LimitedRegion region, Random r, int x, int y, int z) {
        boolean alongX = r.nextBoolean();
        int ax = alongX ? 1 : 0;
        int az = alongX ? 0 : 1;
        if (!clear(region, x, y, z, 2, 6)) return false;

        for (int i = -1; i <= 2; i++) {
            force(region, x + ax * i, y - 1, z + az * i, Material.POLISHED_BLACKSTONE_BRICKS);
            for (int h = 0; h < 5; h++) {
                boolean frame = i == -1 || i == 2 || h == 0 || h == 4;
                if (!frame) continue;
                if (h > 0 && r.nextInt(100) < 28) continue;
                set(region, x + ax * i, y + h, z + az * i, r.nextInt(100) < 30 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
            }
        }

        for (int k = 0; k < 6; k++) {
            int dx = r.nextInt(7) - 3;
            int dz = r.nextInt(7) - 3;
            int fy = floorBelow(region, x + dx, y + 1, z + dz);
            if (fy == Integer.MIN_VALUE) continue;
            if (r.nextBoolean()) force(region, x + dx, fy, z + dz, Material.MAGMA_BLOCK);
            set(region, x + dx, fy + 1, z + dz, r.nextInt(3) == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
        }
        return true;
    }

    // Púas: de 4 a 7 picos de obsidiana que salen del suelo, con la punta de obsidiana llorosa
    private static boolean spikes(LimitedRegion region, Random r, int x, int y, int z) {
        if (!clear(region, x, y, z, 1, 3)) return false;
        int count = 4 + r.nextInt(4);
        force(region, x, y - 1, z, Material.CRYING_OBSIDIAN);
        for (int k = 0; k < count; k++) {
            int dx = r.nextInt(7) - 3;
            int dz = r.nextInt(7) - 3;
            int fy = floorBelow(region, x + dx, y + 2, z + dz);
            if (fy == Integer.MIN_VALUE) continue;
            int height = 2 + r.nextInt(5);
            if (!clear(region, x + dx, fy + 1, z + dz, 0, height)) continue;
            for (int i = 1; i < height; i++) set(region, x + dx, fy + i, z + dz, Material.OBSIDIAN);
            set(region, x + dx, fy + height, z + dz, Material.CRYING_OBSIDIAN);
        }
        return true;
    }

    // Altar: plataforma de ladrillos de piedra negra agrietados, 4 pilares de obsidiana y obsidiana llorosa al centro
    private static boolean altar(LimitedRegion region, Random r, int x, int y, int z) {
        if (!clear(region, x, y, z, 2, 4)) return false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                force(region, x + dx, y - 1, z + dz, r.nextInt(3) == 0
                        ? Material.CRACKED_POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_BLACKSTONE_BRICKS);
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                if (!corner) continue;
                int height = 2 + r.nextInt(2);
                for (int i = 0; i < height; i++) set(region, x + dx, y + i, z + dz, Material.OBSIDIAN);
                set(region, x + dx, y + height, z + dz, Material.CRYING_OBSIDIAN);
            }
        }
        force(region, x, y - 1, z, Material.CRYING_OBSIDIAN);
        set(region, x, y, z, Material.SOUL_LANTERN);
        return true;
    }

    // Lugar libre de (2r+1)x(2r+1) y esa altura arriba del suelo; el suelo puede tener huecos chicos (la base los tapa)
    // pero no puede estar en el borde de un precipicio
    private static boolean clear(LimitedRegion region, int x, int y, int z, int radius, int height) {
        int missing = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    if (!canPlace(region, x + dx, y + dy, z + dz)) return false;
                }
                if (!region.isInRegion(x + dx, y - 2, z + dz)) return false;
                if (!region.getType(x + dx, y - 1, z + dz).isSolid()) {
                    if (!region.getType(x + dx, y - 2, z + dz).isSolid()) return false;
                    missing++;
                }
            }
        }
        return missing <= (2 * radius + 1) * (2 * radius + 1) / 3;
    }

    // Primer suelo firme bajando desde fromY (hasta 4 bloques)
    private static int floorBelow(LimitedRegion region, int x, int fromY, int z) {
        for (int y = fromY; y > fromY - 5; y--) {
            if (!region.isInRegion(x, y, z)) return Integer.MIN_VALUE;
            if (region.getType(x, y, z).isSolid() && canPlace(region, x, y + 1, z)) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean canPlace(LimitedRegion region, int x, int y, int z) {
        return y < WardenGenerator.TOP_Y && region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private static void set(LimitedRegion region, int x, int y, int z, Material type) {
        if (canPlace(region, x, y, z)) region.setBlockData(x, y, z, type.createBlockData());
    }

    // Pone la base de las estructuras: reemplaza el suelo o tapa el hueco si falta un bloque
    private static void force(LimitedRegion region, int x, int y, int z, Material type) {
        if (!region.isInRegion(x, y, z)) return;
        Material current = region.getType(x, y, z);
        if (current.isSolid() || current.isAir()) {
            BlockData data = type.createBlockData();
            region.setBlockData(x, y, z, data);
        }
    }
}
