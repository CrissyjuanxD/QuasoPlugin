package EndBiomes;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

import static EndBiomes.EndTrees.canPlace;
import static EndBiomes.EndTrees.set;

final class EndStructures {

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private EndStructures() {}

    // Geoda del End: bola de amatista medio enterrada con amatista con brote arriba y racimos encima y a los costados.
    // Los brotes vuelven a crecer, así los racimos (de donde sale la Celestita) se farmean toda la fase
    static void amethystBall(LimitedRegion region, Random r, int x, int y, int z) {
        int radius = 2 + r.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > radius * radius + 1) continue;
                    int bx = x + dx, by = y + dy, bz = z + dz;
                    if (!region.isInRegion(bx, by, bz)) continue;
                    Material current = region.getType(bx, by, bz);
                    if (!current.isAir() && !isGround(current)) continue;
                    boolean shell = d2 >= (radius - 1) * (radius - 1) + 1;
                    Material type = dy >= 0 && shell && r.nextInt(100) < 35 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK;
                    region.setType(bx, by, bz, type);
                }
            }
        }

        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                for (int dy = 0; dy <= radius + 1; dy++) {
                    int bx = x + dx, by = y + dy, bz = z + dz;
                    if (!canPlace(region, bx, by, bz)) continue;
                    BlockFace face = touching(region, bx, by, bz);
                    if (face == null) continue;
                    int roll = r.nextInt(100);
                    if (roll < 40) cluster(region, bx, by, bz, Material.AMETHYST_CLUSTER, face);
                    else if (roll < 55) cluster(region, bx, by, bz, Material.LARGE_AMETHYST_BUD, face);
                }
            }
        }
    }

    // La cara hacia donde crece un racimo puesto en ese aire (arriba si hay amatista abajo, o a un costado)
    private static BlockFace touching(LimitedRegion region, int x, int y, int z) {
        if (isAmethyst(region, x, y - 1, z)) return BlockFace.UP;
        for (BlockFace side : SIDES) {
            if (isAmethyst(region, x - side.getModX(), y, z - side.getModZ())) return side;
        }
        return null;
    }

    private static boolean isAmethyst(LimitedRegion region, int x, int y, int z) {
        if (!region.isInRegion(x, y, z)) return false;
        Material type = region.getType(x, y, z);
        return type == Material.AMETHYST_BLOCK || type == Material.BUDDING_AMETHYST;
    }

    private static void cluster(LimitedRegion region, int x, int y, int z, Material type, BlockFace facing) {
        AmethystCluster data = (AmethystCluster) type.createBlockData();
        data.setFacing(facing);
        region.setBlockData(x, y, z, data);
    }

    // Santuario Marchito: plataforma de obsidiana, columnas rotas con obsidiana llorosa arriba y un altar al centro.
    // En lo alto de las columnas más altas hay shulkers negros (shulker recibe la altura de cada una)
    static boolean shrine(LimitedRegion region, Random r, int x, int y, int z, ShulkerSpot shulker) {
        if (!flat(region, x, y, z, 4)) return false;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 20) continue;
                force(region, x + dx, y - 1, z + dz, r.nextInt(100) < 18 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
            }
        }

        int shulkers = 0;
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3;
            int px = x + (int) Math.round(Math.cos(angle) * 4);
            int pz = z + (int) Math.round(Math.sin(angle) * 4);
            int height = r.nextInt(4) == 0 ? 2 : 4 + r.nextInt(4);
            for (int i = 0; i < height - 1; i++) set(region, px, y + i, pz, Material.OBSIDIAN.createBlockData());
            set(region, px, y + height - 1, pz, Material.CRYING_OBSIDIAN.createBlockData());
            if (height >= 5 && shulkers < 3 && r.nextInt(3) > 0) {
                shulker.place(px, y + height, pz);
                shulkers++;
            }
        }
        if (shulkers == 0) shulker.place(x, y + 2, z + 1);

        set(region, x, y, z, Material.CRYING_OBSIDIAN.createBlockData());
        set(region, x, y + 1, z, Material.CRYING_OBSIDIAN.createBlockData());
        set(region, x, y + 2, z, Material.END_ROD.createBlockData());
        for (int k = 0; k < 4; k++) {
            int fx = x + r.nextInt(7) - 3;
            int fz = z + r.nextInt(7) - 3;
            if (EndTrees.solid(region, fx, y - 1, fz)) set(region, fx, y, fz, Material.WITHER_ROSE.createBlockData());
        }
        return true;
    }

    // Aguja Marchita: obelisco de obsidiana que se afina hacia arriba, con bandas de obsidiana llorosa y un shulker
    // negro en la punta
    static boolean spire(LimitedRegion region, Random r, int x, int y, int z, ShulkerSpot shulker) {
        if (!flat(region, x, y, z, 1)) return false;
        int height = 10 + r.nextInt(7);
        for (int i = 0; i < height; i++) {
            int radius = i < height / 3 ? 1 : 0;
            Material type = i % 4 == 3 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius == 1 && Math.abs(dx) == 1 && Math.abs(dz) == 1 && i > 1) continue;
                    set(region, x + dx, y + i, z + dz, type.createBlockData());
                }
            }
        }
        shulker.place(x, y + height, z);
        return true;
    }

    interface ShulkerSpot {
        void place(int x, int y, int z);
    }

    // Roca de diorita y calcita medio enterrada en el pasto del Bosque Prismático
    static void rock(LimitedRegion region, Random r, int x, int y, int z) {
        int radius = 1 + r.nextInt(2);
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                for (int dy = -1; dy <= radius; dy++) {
                    double d = dx * dx + dz * dz + dy * dy * 1.6;
                    if (d > radius * radius + 1 + r.nextDouble() * 1.5) continue;
                    if (!region.isInRegion(x + dx, y + dy, z + dz)) continue;
                    Material current = region.getType(x + dx, y + dy, z + dz);
                    if (!current.isAir() && !isGround(current) && current.isSolid()) continue;
                    region.setType(x + dx, y + dy, z + dz, r.nextInt(5) == 0 ? Material.CALCITE : Material.DIORITE);
                }
            }
        }
    }

    // Lugar parejo: el suelo de alrededor está a la misma altura (±1) y hay aire arriba
    private static boolean flat(LimitedRegion region, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) continue;
                boolean ground = EndTrees.solid(region, x + dx, y - 1, z + dz) || EndTrees.solid(region, x + dx, y - 2, z + dz);
                if (!ground || !canPlace(region, x + dx, y + 1, z + dz)) return false;
            }
        }
        return true;
    }

    private static void force(LimitedRegion region, int x, int y, int z, Material type) {
        if (!region.isInRegion(x, y, z)) return;
        Material current = region.getType(x, y, z);
        if (current.isAir() || isGround(current) || current == Material.WITHER_ROSE) region.setType(x, y, z, type);
    }

    static boolean isGround(Material type) {
        return type == Material.END_STONE || type == Material.GRASS_BLOCK || type == Material.DIRT
                || type == Material.COARSE_DIRT || type == Material.SOUL_SOIL || type == Material.MUD
                || type == Material.SHORT_GRASS || type == Material.TALL_GRASS;
    }
}
