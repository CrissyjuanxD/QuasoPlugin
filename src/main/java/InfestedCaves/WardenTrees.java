package InfestedCaves;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.HangingMoss;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

public final class WardenTrees {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private WardenTrees() {}

    // Caverna Sculk: el árbol de siempre, un chorus convertido en vallas de warped con froglights en las puntas
    static void sculk(LimitedRegion region, Random random, int x, int y, int z) {
        BlockData soil = region.getBlockData(x, y - 1, z);
        region.setType(x, y - 1, z, Material.END_STONE);
        region.generateTree(new Location(null, x, y, z), random, TreeType.CHORUS_PLANT, state -> {
            if (!region.isInRegion(state.getX(), state.getY(), state.getZ())) return false;
            if (!region.getType(state.getX(), state.getY(), state.getZ()).isAir()) return false;

            if (state.getType() == Material.CHORUS_PLANT) {
                MultipleFacing plant = (MultipleFacing) state.getBlockData();
                Fence fence = (Fence) Material.WARPED_FENCE.createBlockData();
                for (BlockFace face : HORIZONTAL) {
                    fence.setFace(face, plant.hasFace(face));
                }
                state.setBlockData(fence);
            } else if (state.getType() == Material.CHORUS_FLOWER) {
                state.setType(Material.VERDANT_FROGLIGHT);
            }
            return true;
        });
        region.setBlockData(x, y - 1, z, soil);
    }

    // Pantano Profundo: sauce de manglar con raíces, copa de azalea y enredaderas con bayas colgando
    static void swamp(LimitedRegion region, Random r, int x, int y, int z) {
        int height = 4 + r.nextInt(4);
        if (!hasSpace(region, x, y, z, height + 2)) return;

        BlockData log = Material.MANGROVE_LOG.createBlockData();
        for (int i = 0; i < height; i++) set(region, x, y + i, z, log);
        for (BlockFace face : HORIZONTAL) {
            if (r.nextInt(3) > 0) set(region, x + face.getModX(), y, z + face.getModZ(), Material.MANGROVE_ROOTS.createBlockData());
        }

        int top = y + height;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    double shape = (dx * dx + dz * dz) / 9.0 + (dy * dy) / 2.25;
                    if (shape > 1.0 || r.nextInt(8) == 0) continue;
                    set(region, x + dx, top + dy, z + dz, leaves(r.nextInt(10) < 3
                            ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES));
                }
            }
        }

        // Las hojas no sostienen enredaderas (se rompían y dejaban glow berries tiradas): cuelgan de un bloque de
        // musgo metido en la copa, y solo donde hay copa encima
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 9 || r.nextInt(4) != 0) continue;
                int vx = x + dx;
                int vz = z + dz;
                if (!region.isInRegion(vx, top - 1, vz) || !Tag.LEAVES.isTagged(region.getType(vx, top - 1, vz))) continue;
                if (!canPlace(region, vx, top - 2, vz)) continue;
                region.setType(vx, top - 1, vz, Material.MOSS_BLOCK);
                hangVines(region, r, vx, top - 2, vz, 1 + r.nextInt(4));
            }
        }
    }

    // Abismo Flotante: tronco de obsidiana llorosa con ramas de amatista y froglights perlados
    static void crystal(LimitedRegion region, Random r, int x, int y, int z) {
        int height = 3 + r.nextInt(3);
        if (!hasSpace(region, x, y, z, height + 3)) return;

        BlockData trunk = Material.CRYING_OBSIDIAN.createBlockData();
        for (int i = 0; i < height; i++) set(region, x, y + i, z, trunk);
        int top = y + height - 1;

        int branches = 3 + r.nextInt(2);
        for (int b = 0; b < branches; b++) {
            BlockFace dir = HORIZONTAL[r.nextInt(HORIZONTAL.length)];
            int length = 2 + r.nextInt(2);
            int bx = x, by = top, bz = z;
            for (int s = 0; s < length; s++) {
                bx += dir.getModX();
                bz += dir.getModZ();
                if (s % 2 == 0) by++;
                set(region, bx, by, bz, Material.AMETHYST_BLOCK.createBlockData());
            }
            set(region, bx, by + 1, bz, Material.PEARLESCENT_FROGLIGHT.createBlockData());
            if (r.nextBoolean()) {
                AmethystCluster cluster = (AmethystCluster) Material.AMETHYST_CLUSTER.createBlockData();
                cluster.setFacing(BlockFace.UP);
                set(region, bx, by + 2, bz, cluster);
            }
        }
        set(region, x, top + 1, z, Material.PEARLESCENT_FROGLIGHT.createBlockData());
    }

    // Ruinas de Ceniza: árbol seco de basalto con hojas de pale oak y musgo colgando
    static void ash(LimitedRegion region, Random r, int x, int y, int z) {
        int height = 4 + r.nextInt(4);
        if (!hasSpace(region, x, y, z, height + 2)) return;

        int tx = x, tz = z;
        for (int i = 0; i < height; i++) {
            if (i == height / 2 && r.nextBoolean()) {
                BlockFace bend = HORIZONTAL[r.nextInt(HORIZONTAL.length)];
                tx += bend.getModX();
                tz += bend.getModZ();
            }
            set(region, tx, y + i, tz, basalt(Axis.Y));
        }

        int top = y + height - 1;
        int branches = 2 + r.nextInt(2);
        for (int b = 0; b < branches; b++) {
            BlockFace dir = HORIZONTAL[r.nextInt(HORIZONTAL.length)];
            Axis axis = dir.getModX() != 0 ? Axis.X : Axis.Z;
            int length = 1 + r.nextInt(3);
            int bx = tx, by = top - r.nextInt(2), bz = tz;
            for (int s = 0; s < length; s++) {
                bx += dir.getModX();
                bz += dir.getModZ();
                set(region, bx, by, bz, basalt(axis));
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (r.nextInt(3) > 0) set(region, bx + dx, by + dy, bz + dz, leaves(Material.PALE_OAK_LEAVES));
                    }
                }
            }
            hangMoss(region, r, bx, by - 1, bz, 1 + r.nextInt(2));
        }
    }

    private static Leaves leaves(Material type) {
        Leaves leaves = (Leaves) type.createBlockData();
        leaves.setPersistent(true);
        return leaves;
    }

    private static Orientable basalt(Axis axis) {
        Orientable basalt = (Orientable) Material.BASALT.createBlockData();
        basalt.setAxis(axis);
        return basalt;
    }

    private static void hangVines(LimitedRegion region, Random r, int x, int y, int z, int max) {
        int length = 0;
        while (length < max && canPlace(region, x, y - length, z)) length++;
        for (int i = 0; i < length; i++) {
            Material type = i == length - 1 ? Material.CAVE_VINES : Material.CAVE_VINES_PLANT;
            CaveVinesPlant vine = (CaveVinesPlant) type.createBlockData();
            vine.setBerries(r.nextInt(100) < 35);
            region.setBlockData(x, y - i, z, vine);
        }
    }

    private static void hangMoss(LimitedRegion region, Random r, int x, int y, int z, int max) {
        int length = 0;
        while (length < max && canPlace(region, x, y - length, z)) length++;
        for (int i = 0; i < length; i++) {
            HangingMoss moss = (HangingMoss) Material.PALE_HANGING_MOSS.createBlockData();
            moss.setTip(i == length - 1);
            region.setBlockData(x, y - i, z, moss);
        }
    }

    private static boolean hasSpace(LimitedRegion region, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            if (!canPlace(region, x, y + i, z)) return false;
        }
        return true;
    }

    private static boolean canPlace(LimitedRegion region, int x, int y, int z) {
        return y < WardenGenerator.TOP_Y && region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }

    private static void set(LimitedRegion region, int x, int y, int z, BlockData data) {
        if (canPlace(region, x, y, z)) region.setBlockData(x, y, z, data);
    }
}
