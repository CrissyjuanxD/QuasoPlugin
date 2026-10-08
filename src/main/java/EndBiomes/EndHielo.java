package EndBiomes;

import org.bukkit.Material;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

// Lo que tienen los Picos Helados (copiado de la idea del bioma de hielo de Stellarity): picos de hielo compacto con
// vetas de hielo azul que se meten en la isla, carámbanos colgando de abajo de las islas y bolsones de nieve polvo
final class EndHielo {

    private EndHielo() {}

    // Pico de hielo: un cono irregular apoyado en la nieve. Los grandes (6 de cada 10) miden de 16 a 28 bloques y los
    // chicos de 6 a 11. Abajo sigue como raíz de hielo compacto unos bloques dentro de la isla
    static void spike(LimitedRegion region, Random r, int x, int y, int z) {
        boolean large = r.nextInt(10) < 6;
        int height = large ? 16 + r.nextInt(13) : 6 + r.nextInt(6);
        double base = large ? 4.5 + r.nextDouble() * 2 : 1.8 + r.nextDouble();
        double leanX = (r.nextDouble() - 0.5) * 0.25;
        double leanZ = (r.nextDouble() - 0.5) * 0.25;
        for (int dy = 0; dy <= height; dy++) {
            double t = dy / (double) height;
            double radius = base * Math.pow(1 - t, 1.4) + 0.4;
            double cx = x + leanX * dy;
            double cz = z + leanZ * dy;
            int reach = (int) Math.ceil(radius) + 1;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int bx = (int) Math.floor(cx) + dx, bz = (int) Math.floor(cz) + dz;
                    double ddx = bx + 0.5 - cx, ddz = bz + 0.5 - cz;
                    double d = Math.sqrt(ddx * ddx + ddz * ddz);
                    if (d > radius + r.nextDouble() * 0.6) continue;
                    place(region, bx, y + dy, bz, ice(r));
                }
            }
        }
        for (int d = 1; d <= 4 + r.nextInt(5); d++) {
            for (int i = 0; i < 6; i++) {
                int bx = x + r.nextInt(3) - r.nextInt(3), bz = z + r.nextInt(3) - r.nextInt(3);
                if (!region.isInRegion(bx, y - d, bz)) continue;
                Material type = region.getType(bx, y - d, bz);
                if (type == Material.END_STONE || type == Material.SNOW_BLOCK) region.setType(bx, y - d, bz, ice(r));
            }
        }
    }

    // Carámbano: cono de hielo compacto colgando de la parte de abajo de una isla
    static void icicle(LimitedRegion region, Random r, int x, int y, int z) {
        int length = 6 + r.nextInt(10);
        double base = 1.5 + r.nextDouble() * 1.6;
        for (int dy = 0; dy < length; dy++) {
            double radius = base * (1 - dy / (double) length) + 0.3;
            int reach = (int) Math.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    place(region, x + dx, y - dy, z + dz, r.nextInt(8) == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                }
            }
        }
    }

    // Bolsón de nieve polvo metido en la nieve de arriba (se hunde el que lo pisa)
    static void powderSnow(LimitedRegion region, Random r, int x, int y, int z) {
        int radius = 1 + r.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    if (dx * dx + dz * dz + dy * dy > radius * radius + 1) continue;
                    int bx = x + dx, by = y + dy, bz = z + dz;
                    if (region.isInRegion(bx, by, bz) && region.getType(bx, by, bz) == Material.SNOW_BLOCK) {
                        region.setType(bx, by, bz, Material.POWDER_SNOW);
                    }
                }
            }
        }
    }

    // Busca la parte de abajo de la isla en esa columna (bloque sólido con aire debajo) desde la superficie
    static int underside(LimitedRegion region, int x, int top, int z) {
        for (int y = top; y > 2; y--) {
            if (!region.getType(x, y, z).isAir() && region.getType(x, y - 1, z).isAir()) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static Material ice(Random r) {
        int roll = r.nextInt(100);
        return roll < 14 ? Material.BLUE_ICE : roll < 20 ? Material.ICE : Material.PACKED_ICE;
    }

    private static void place(LimitedRegion region, int x, int y, int z, Material type) {
        if (y < 1 || y >= EndSuelo.MAX_Y || !region.isInRegion(x, y, z)) return;
        Material current = region.getType(x, y, z);
        if (current.isAir() || current == Material.SNOW || current == Material.SNOW_BLOCK) region.setType(x, y, z, type);
    }
}
