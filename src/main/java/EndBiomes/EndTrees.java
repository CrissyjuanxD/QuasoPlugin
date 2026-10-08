package EndBiomes;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.LeafLitter;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

final class EndTrees {

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private EndTrees() {}

    // El color de la copa de un árbol del Bosque Prismático: lana con algo de vidrio del mismo color (sin hojas, así no
    // tiran partículas). El verde es de azalea y el rosa de cerezo
    enum Copa {
        ROJO(Material.RED_WOOL, Material.RED_STAINED_GLASS),
        NARANJA(Material.ORANGE_WOOL, Material.ORANGE_STAINED_GLASS),
        AMARILLO(Material.YELLOW_WOOL, Material.YELLOW_STAINED_GLASS),
        LIMA(Material.LIME_WOOL, Material.LIME_STAINED_GLASS),
        VERDE(Material.AZALEA_LEAVES, null),
        CELESTE(Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_STAINED_GLASS),
        AZUL(Material.BLUE_WOOL, Material.BLUE_STAINED_GLASS),
        MORADO(Material.PURPLE_WOOL, Material.PURPLE_STAINED_GLASS),
        MAGENTA(Material.MAGENTA_WOOL, Material.MAGENTA_STAINED_GLASS),
        ROSA(Material.CHERRY_LEAVES, null),
        BLANCO(Material.WHITE_WOOL, Material.WHITE_STAINED_GLASS);

        private final Material main;
        private final Material glass;

        Copa(Material main, Material glass) {
            this.main = main;
            this.glass = glass;
        }

        BlockData block(Random r) {
            if (glass != null && r.nextInt(7) < 2) return glass.createBlockData();
            return main == Material.AZALEA_LEAVES || main == Material.CHERRY_LEAVES ? leaves(main) : main.createBlockData();
        }

        // Los de hojas dejan hojarasca alrededor
        boolean hojarasca() {
            return glass == null;
        }
    }

    // Los colores que más salen en cada mancha del bosque; el resto de los árboles sale de cualquier color
    static Copa[] tono(EndBiome biome) {
        return switch (biome) {
            case PRISMATICO_ROSA -> new Copa[]{Copa.ROSA, Copa.MAGENTA};
            case PRISMATICO_VERDE -> new Copa[]{Copa.LIMA, Copa.VERDE};
            case PRISMATICO_NARANJA -> new Copa[]{Copa.NARANJA, Copa.AMARILLO};
            case PRISMATICO_AMARILLO -> new Copa[]{Copa.AMARILLO, Copa.BLANCO};
            case PRISMATICO_ROJO -> new Copa[]{Copa.ROJO, Copa.NARANJA};
            default -> new Copa[]{Copa.MORADO, Copa.AZUL, Copa.CELESTE};
        };
    }

    static Copa color(EndBiome biome, Random r) {
        Copa[] tono = tono(biome);
        if (r.nextInt(100) < 55) return tono[r.nextInt(tono.length)];
        Copa[] all = Copa.values();
        return all[r.nextInt(all.length)];
    }

    // Árbol Prismático grande: tronco de abedul pelado con raíces, ramas que suben hacia afuera y bolas de copa en la
    // punta de cada rama (como un roble grande), faroles colgando de las ramas
    static boolean fancy(LimitedRegion region, Random r, int x, int y, int z, Copa copa) {
        int height = 11 + r.nextInt(8);
        if (!solid(region, x, y - 1, z) || !column(region, x, y, z, height + 3)) return false;

        Material wood = Material.STRIPPED_BIRCH_WOOD;
        for (int i = 0; i < height; i++) set(region, x, y + i, z, log(Material.STRIPPED_BIRCH_LOG, Axis.Y));
        roots(region, r, x, y, z, wood);

        int top = y + height;
        blob(region, r, x, top, z, 3, 2, copa);
        int branches = 3 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            double angle = (b + r.nextDouble() * 0.7) * (Math.PI * 2 / branches);
            int by = y + height / 2 + r.nextInt(Math.max(1, height / 2 - 1));
            int length = 3 + r.nextInt(3);
            double bx = x + 0.5, bz = z + 0.5;
            int ey = by;
            for (int s = 0; s < length; s++) {
                bx += Math.cos(angle);
                bz += Math.sin(angle);
                if (s % 2 == 1) ey++;
                Axis axis = Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle)) ? Axis.X : Axis.Z;
                set(region, (int) Math.floor(bx), ey, (int) Math.floor(bz), log(Material.STRIPPED_BIRCH_LOG, axis));
            }
            int ex = (int) Math.floor(bx), ez = (int) Math.floor(bz);
            blob(region, r, ex, ey + 1, ez, 2 + r.nextInt(2), 2, copa);
            if (r.nextInt(3) == 0) lantern(region, r, (int) Math.floor(x + 0.5 + Math.cos(angle) * 2), ey - 1, (int) Math.floor(z + 0.5 + Math.sin(angle) * 2));
        }
        decorate(region, r, x, y, z, height, 5, copa);
        return true;
    }

    // Pino Prismático: tronco de abeto pelado, copa en punta fina que se ensancha abajo
    static boolean pine(LimitedRegion region, Random r, int x, int y, int z, Copa copa) {
        int height = 14 + r.nextInt(8);
        if (!solid(region, x, y - 1, z) || !column(region, x, y, z, height + 2)) return false;

        for (int i = 0; i < height; i++) set(region, x, y + i, z, log(Material.STRIPPED_SPRUCE_LOG, Axis.Y));
        roots(region, r, x, y, z, Material.STRIPPED_SPRUCE_WOOD);

        int top = y + height;
        int crown = height * 2 / 3;
        for (int ly = top - crown; ly <= top + 1; ly++) {
            double p = (double) (top + 1 - ly) / crown;
            double radius = 0.6 + p * 3.2;
            // Cada 3 bloques la copa se mete, así se ven los pisos
            if ((top - ly) % 3 == 2) radius *= 0.6;
            int reach = (int) Math.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d2 = dx * dx + dz * dz;
                    if (d2 > radius * radius) continue;
                    if (d2 > (radius - 1) * (radius - 1) && r.nextInt(4) == 0) continue;
                    set(region, x + dx, ly, z + dz, copa.block(r));
                }
            }
        }
        set(region, x, top + 2, z, copa.block(r));
        decorate(region, r, x, y, z, height, 4, copa);
        return true;
    }

    // Árbol Prismático gigante: tronco de jungla de 2x2 y una copa desordenada que cuelga alrededor de la punta
    static boolean jungle(LimitedRegion region, Random r, int x, int y, int z, Copa copa) {
        int height = 12 + r.nextInt(7);
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                if (!solid(region, x + dx, y - 1, z + dz) || !column(region, x + dx, y, z + dz, height + 2)) return false;
            }
        }
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int i = 0; i < height; i++) set(region, x + dx, y + i, z + dz, log(Material.STRIPPED_JUNGLE_LOG, Axis.Y));
            }
        }

        int top = y + height;
        blob(region, r, x, top, z, 4, 2, copa);
        for (int i = 0; i < 7; i++) {
            int bx = x + r.nextInt(9) - 4;
            int bz = z + r.nextInt(9) - 4;
            int by = top - 1 - r.nextInt(Math.max(1, height / 3));
            blob(region, r, bx, by, bz, 2, 1, copa);
        }
        decorate(region, r, x, y, z, height, 6, copa);
        return true;
    }

    // Arbusto: un tronquito con una mata de hojas que el bioma tiñe del color de la mancha (a veces con cerezo)
    static void bush(LimitedRegion region, Random r, int x, int y, int z) {
        if (!solid(region, x, y - 1, z) || !canPlace(region, x, y, z)) return;
        set(region, x, y, z, log(Material.STRIPPED_JUNGLE_LOG, Axis.Y));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 > 5 || r.nextInt(5) == 0) continue;
                    set(region, x + dx, y + dy + (dx == 0 && dz == 0 ? 1 : 0), z + dz, leaves(r.nextInt(100) < 15 ? Material.CHERRY_LEAVES : Material.OAK_LEAVES));
                }
            }
        }
    }

    // Raíces que salen del tronco hacia los costados y se meten en el suelo
    private static void roots(LimitedRegion region, Random r, int x, int y, int z, Material wood) {
        for (BlockFace face : SIDES) {
            if (r.nextInt(3) == 0) continue;
            int length = 1 + r.nextInt(2);
            for (int s = 1; s <= length; s++) {
                int rx = x + face.getModX() * s, rz = z + face.getModZ() * s;
                int ry = s == 1 ? y + r.nextInt(2) : y;
                set(region, rx, ry, rz, wood.createBlockData());
                if (s == length && region.isInRegion(rx, y - 1, rz) && !region.getType(rx, y - 1, rz).isAir()) {
                    region.setBlockData(rx, y - 1, rz, wood.createBlockData());
                }
            }
        }
    }

    // Lo que cuelga y lo que cae según el color: vides en los rojos, varas del End en los blancos y hojarasca en el suelo
    // debajo de los de hojas
    private static void decorate(LimitedRegion region, Random r, int x, int y, int z, int height, int radius, Copa copa) {
        for (int i = 0; i < 18; i++) {
            int dx = r.nextInt(radius * 2 + 1) - radius;
            int dz = r.nextInt(radius * 2 + 1) - radius;
            int bx = x + dx, bz = z + dz;
            for (int by = y + height + 2; by > y + 2; by--) {
                if (!region.isInRegion(bx, by, bz)) break;
                Material above = region.getType(bx, by, bz);
                if (above.isAir()) continue;
                if (!canPlace(region, bx, by - 1, bz)) break;
                if (copa == Copa.ROJO && r.nextInt(3) == 0) vines(region, r, bx, by - 1, bz);
                else if (copa == Copa.BLANCO && r.nextInt(5) == 0) endRod(region, bx, by - 1, bz);
                break;
            }
        }
        if (!copa.hojarasca()) return;
        for (int i = 0; i < 14; i++) {
            int bx = x + r.nextInt(7) - 3;
            int bz = z + r.nextInt(7) - 3;
            for (int by = y + 1; by >= y - 2; by--) {
                if (!region.isInRegion(bx, by, bz) || region.getType(bx, by - 1, bz) != Material.GRASS_BLOCK) continue;
                if (!canPlace(region, bx, by, bz)) break;
                LeafLitter litter = (LeafLitter) Material.LEAF_LITTER.createBlockData();
                litter.setSegmentAmount(1 + r.nextInt(4));
                litter.setFacing(SIDES[r.nextInt(SIDES.length)]);
                region.setBlockData(bx, by, bz, litter);
                break;
            }
        }
    }

    private static void vines(LimitedRegion region, Random r, int x, int y, int z) {
        int length = 3 + r.nextInt(6);
        int placed = 0;
        while (placed < length && canPlace(region, x, y - placed, z)) placed++;
        for (int i = 0; i < placed; i++) {
            region.setType(x, y - i, z, i == placed - 1 ? Material.WEEPING_VINES : Material.WEEPING_VINES_PLANT);
        }
    }

    private static void endRod(LimitedRegion region, int x, int y, int z) {
        org.bukkit.block.data.Directional rod = (org.bukkit.block.data.Directional) Material.END_ROD.createBlockData();
        rod.setFacing(BlockFace.DOWN);
        set(region, x, y, z, rod);
    }

    // Farol colgando de 2 a 4 eslabones de cadena
    private static void lantern(LimitedRegion region, Random r, int x, int y, int z) {
        if (!region.isInRegion(x, y + 1, z) || region.getType(x, y + 1, z).isAir()) return;
        int chain = 2 + r.nextInt(3);
        for (int i = 0; i <= chain; i++) {
            if (!canPlace(region, x, y - i, z)) return;
        }
        for (int i = 0; i < chain; i++) set(region, x, y - i, z, Material.IRON_CHAIN.createBlockData());
        Lantern lantern = (Lantern) Material.LANTERN.createBlockData();
        lantern.setHanging(true);
        set(region, x, y - chain, z, lantern);
    }

    // Bola de copa achatada con el borde deshilachado
    private static void blob(LimitedRegion region, Random r, int x, int y, int z, int radius, int up, Copa copa) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= up; dy++) {
                    double shape = (dx * dx + dz * dz) / (double) (radius * radius) + (dy * dy) / (double) ((up + 1) * (up + 1));
                    if (shape > 1.0 || (shape > 0.7 && r.nextInt(3) == 0)) continue;
                    set(region, x + dx, y + dy, z + dz, copa.block(r));
                }
            }
        }
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
            leafBlob(region, r, (int) Math.floor(bx), by + 1, (int) Math.floor(bz), 3 + r.nextInt(2), leaves);
        }
        leafBlob(region, r, x, y + height + 1, z, 4, leaves);
        return true;
    }

    private static void leafBlob(LimitedRegion region, Random r, int x, int y, int z, int radius, BlockData leaves) {
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

    private static boolean column(LimitedRegion region, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            if (!canPlace(region, x, y + i, z)) return false;
        }
        return true;
    }

    private static Orientable log(Material type, Axis axis) {
        Orientable log = (Orientable) type.createBlockData();
        log.setAxis(axis);
        return log;
    }

    static Leaves leaves(Material type) {
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
