package EndBiomes;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

final class EndTrees {

    private EndTrees() {}

    // Pino Prismático: tronco de abedul y copa en punta por pisos. Las hojas toman el color de la mancha del bosque
    // donde crece (rosa, verde, naranja, amarillo, rojo o morado); 3 de cada 10 son gigantes con tronco de 2x2
    static boolean pine(LimitedRegion region, Random r, int x, int y, int z) {
        boolean giant = r.nextInt(100) < 30;
        int width = giant ? 2 : 1;
        int height = giant ? 22 + r.nextInt(9) : 13 + r.nextInt(7);
        double maxRadius = giant ? 6.2 : 4.2;
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < width; dz++) {
                if (!solid(region, x + dx, y - 1, z + dz)) return false;
                for (int i = 0; i < height + 2; i++) {
                    if (!canPlace(region, x + dx, y + i, z + dz)) return false;
                }
            }
        }

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < width; dz++) {
                for (int i = 0; i < height; i++) set(region, x + dx, y + i, z + dz, log(Material.BIRCH_LOG));
            }
        }

        int top = y + height;
        int canopyStart = y + height / 3;
        double cx = x + (width - 1) / 2.0;
        double cz = z + (width - 1) / 2.0;
        BlockData leaves = leaves(Material.OAK_LEAVES);
        for (int ly = canopyStart; ly <= top + 1; ly++) {
            double p = (double) (top + 1 - ly) / (top + 1 - canopyStart);
            double radius = 0.9 + p * (maxRadius - 0.9);
            // Cada 3 bloques la copa se mete, así se ven los pisos del pino
            if ((top - ly) % 3 == 2) radius *= 0.62;
            int reach = (int) Math.ceil(radius) + 1;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double ddx = x + dx - cx;
                    double ddz = z + dz - cz;
                    double d2 = ddx * ddx + ddz * ddz;
                    if (d2 > radius * radius) continue;
                    if (d2 > (radius - 1) * (radius - 1) && r.nextInt(5) == 0) continue;
                    set(region, x + dx, ly, z + dz, leaves);
                }
            }
        }
        for (int i = 2; i <= (giant ? 3 : 2); i++) set(region, x, top + i, z, leaves);
        return true;
    }

    // Árbol Marchito: tronco grueso de roble oscuro con raíces, ramas gruesas que salen torcidas y copas ralas que
    // el bioma tiñe casi de negro
    static boolean withered(LimitedRegion region, Random r, int x, int y, int z) {
        int height = 12 + r.nextInt(7);
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                if (!solid(region, x + dx, y - 1, z + dz)) return false;
                for (int i = 0; i < height + 2; i++) {
                    if (!canPlace(region, x + dx, y + i, z + dz)) return false;
                }
            }
        }

        BlockData wood = Material.DARK_OAK_WOOD.createBlockData();
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int i = 0; i < height; i++) set(region, x + dx, y + i, z + dz, wood);
            }
        }
        int[][] roots = {{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}};
        for (int[] root : roots) {
            if (r.nextInt(2) == 0) continue;
            set(region, x + root[0], y, z + root[1], wood);
            if (r.nextInt(3) == 0) set(region, x + root[0], y + 1, z + root[1], wood);
        }

        BlockData leaves = leaves(Material.DARK_OAK_LEAVES);
        int branches = 5 + r.nextInt(2);
        for (int b = 0; b < branches; b++) {
            double angle = (b + r.nextDouble() * 0.6) * (Math.PI * 2 / branches);
            double bx = x + 0.5;
            double bz = z + 0.5;
            int by = y + height / 2 + r.nextInt(Math.max(1, height / 2));
            int length = 5 + r.nextInt(5);
            for (int s = 0; s < length; s++) {
                bx += Math.cos(angle);
                bz += Math.sin(angle);
                if (r.nextInt(2) == 0) by++;
                set(region, (int) Math.floor(bx), by, (int) Math.floor(bz), wood);
            }
            blob(region, r, (int) Math.floor(bx), by + 1, (int) Math.floor(bz), 3 + r.nextInt(2), leaves);
        }
        blob(region, r, x, y + height + 1, z, 4, leaves);
        return true;
    }

    private static void blob(LimitedRegion region, Random r, int x, int y, int z, int radius, BlockData leaves) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= radius - 1; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 > radius * radius + 1) continue;
                    if (r.nextInt(3) == 0) continue;
                    set(region, x + dx, y + dy, z + dz, leaves);
                }
            }
        }
    }

    private static Orientable log(Material type) {
        Orientable log = (Orientable) type.createBlockData();
        log.setAxis(Axis.Y);
        return log;
    }

    private static Leaves leaves(Material type) {
        Leaves leaves = (Leaves) type.createBlockData();
        leaves.setPersistent(true);
        return leaves;
    }

    static boolean solid(LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isSolid();
    }

    static boolean canPlace(LimitedRegion region, int x, int y, int z) {
        return y < 254 && region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    static void set(LimitedRegion region, int x, int y, int z, BlockData data) {
        if (canPlace(region, x, y, z)) region.setBlockData(x, y, z, data);
    }
}
