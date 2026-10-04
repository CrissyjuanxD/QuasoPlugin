package EndBiomes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Shulker;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

// Los biomas nuevos del End se ponen sobre el End vanilla: cuando se genera un chunk de las islas de afuera que cae
// en una región nueva se le cambia el bioma, se le saca el chorus, la end stone de arriba pasa a tierra (con
// degradado hacia la end stone de abajo) y se le ponen los árboles, las plantas y las estructuras
public class EndPopulator extends BlockPopulator {

    private static final int MAX_Y = 254;
    // Hasta qué profundidad baja la tierra; desde la 3ra capa se va mezclando con end stone
    private static final int SOIL_DEPTH = 8;

    private final BlackShulker blackShulker;

    public EndPopulator(BlackShulker blackShulker) {
        this.blackShulker = blackShulker;
    }

    @Override
    public void populate(WorldInfo info, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        EndBiomeMap map = EndBiomeMap.forSeed(info.getSeed());
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        EndBiome[] columns = new EndBiome[256];
        boolean any = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                columns[x << 4 | z] = map.biomeAt(baseX + x, baseZ + z);
                if (columns[x << 4 | z] != null) any = true;
            }
        }
        if (!any) return;

        // El bioma se guarda por cuadros de 4x4x4: se usa el de la columna del medio de cada cuadro
        for (int qx = 0; qx < 16; qx += 4) {
            for (int qz = 0; qz < 16; qz += 4) {
                EndBiome biome = columns[(qx + 2) << 4 | (qz + 2)];
                Biome value = biome != null ? biome.get() : null;
                if (value == null) continue;
                for (int y = info.getMinHeight(); y < info.getMaxHeight(); y += 4) {
                    region.setBiome(baseX + qx, y, baseZ + qz, value);
                }
            }
        }

        int[] top = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                top[col] = Integer.MIN_VALUE;
                EndBiome biome = columns[col];
                if (biome == null) continue;
                top[col] = soil(region, random, baseX + x, baseZ + z, biome == EndBiome.PARAMO_MARCHITO);
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                if (top[col] == Integer.MIN_VALUE) continue;
                if (columns[col] == EndBiome.PARAMO_MARCHITO) witheredPlant(region, random, baseX + x, top[col] + 1, baseZ + z);
                else prismaticPlant(region, random, baseX + x, top[col] + 1, baseZ + z);
            }
        }

        EndBiome center = columns[8 << 4 | 8];
        if (center == null) return;
        if (center == EndBiome.PARAMO_MARCHITO) witheredFeatures(region, random, baseX, baseZ, top);
        else prismaticFeatures(region, random, baseX, baseZ, top);
    }

    // Saca el chorus de la columna y pasa a tierra cada superficie de end stone. Devuelve la superficie más alta
    private int soil(LimitedRegion region, Random r, int x, int z, boolean paramo) {
        int highest = Integer.MIN_VALUE;
        for (int y = MAX_Y; y > 1; y--) {
            Material type = region.getType(x, y, z);
            if (isChorus(type)) {
                removeChorus(region, x, y, z);
                continue;
            }
            if (type != Material.END_STONE || !region.getType(x, y + 1, z).isAir()) continue;

            for (int d = 0; d < SOIL_DEPTH && y - d > 0; d++) {
                if (region.getType(x, y - d, z) != Material.END_STONE) break;
                double keep = d < 3 ? 1 : 1 - (d - 2) / (double) (SOIL_DEPTH - 2);
                if (r.nextDouble() > keep) continue;
                region.setType(x, y - d, z, soilBlock(r, d, paramo));
            }
            if (highest == Integer.MIN_VALUE) highest = y;
        }
        return highest;
    }

    // Saca la planta de chorus entera (también lo que cruza a los chunks de al lado): si quedara un pedazo sin su
    // base, al primer cambio de bloque se rompería y dejaría frutas de chorus tiradas
    private void removeChorus(LimitedRegion region, int x, int y, int z) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{x, y, z});
        int removed = 0;
        while (!queue.isEmpty() && removed < 512) {
            int[] p = queue.poll();
            if (!region.isInRegion(p[0], p[1], p[2]) || !isChorus(region.getType(p[0], p[1], p[2]))) continue;
            region.setType(p[0], p[1], p[2], Material.AIR);
            removed++;
            queue.add(new int[]{p[0] + 1, p[1], p[2]});
            queue.add(new int[]{p[0] - 1, p[1], p[2]});
            queue.add(new int[]{p[0], p[1] + 1, p[2]});
            queue.add(new int[]{p[0], p[1] - 1, p[2]});
            queue.add(new int[]{p[0], p[1], p[2] + 1});
            queue.add(new int[]{p[0], p[1], p[2] - 1});
        }
    }

    private static boolean isChorus(Material type) {
        return type == Material.CHORUS_PLANT || type == Material.CHORUS_FLOWER;
    }

    private Material soilBlock(Random r, int depth, boolean paramo) {
        if (paramo) {
            if (depth == 0) return r.nextInt(100) < 80 ? Material.SOUL_SOIL : Material.MUD;
            return r.nextInt(100) < 55 ? Material.MUD : Material.SOUL_SOIL;
        }
        if (depth == 0) return Material.GRASS_BLOCK;
        return r.nextInt(100) < 15 ? Material.COARSE_DIRT : Material.DIRT;
    }

    // Bosque Prismático: pasto, flores, arbustos de luciérnagas y pasto alto
    private void prismaticPlant(LimitedRegion region, Random r, int x, int y, int z) {
        if (region.getType(x, y - 1, z) != Material.GRASS_BLOCK || !EndTrees.canPlace(region, x, y, z)) return;
        int roll = r.nextInt(1000);
        if (roll < 220) region.setType(x, y, z, Material.SHORT_GRASS);
        else if (roll < 250) tall(region, x, y, z, Material.TALL_GRASS);
        else if (roll < 265) region.setType(x, y, z, Material.ALLIUM);
        else if (roll < 280) region.setType(x, y, z, Material.BLUE_ORCHID);
        else if (roll < 300) region.setType(x, y, z, Material.PINK_PETALS);
        else if (roll < 315) region.setType(x, y, z, Material.WILDFLOWERS);
        else if (roll < 322) region.setType(x, y, z, Material.FIREFLY_BUSH);
    }

    // Páramo Marchito: rosas del Wither, arbustos secos, pasto seco y hojarasca
    private void witheredPlant(LimitedRegion region, Random r, int x, int y, int z) {
        Material ground = region.getType(x, y - 1, z);
        if ((ground != Material.SOUL_SOIL && ground != Material.MUD) || !EndTrees.canPlace(region, x, y, z)) return;
        int roll = r.nextInt(1000);
        if (roll < 28) region.setType(x, y, z, Material.WITHER_ROSE);
        else if (roll < 60) region.setType(x, y, z, Material.DEAD_BUSH);
        else if (roll < 130) region.setType(x, y, z, Material.SHORT_DRY_GRASS);
        else if (roll < 150) region.setType(x, y, z, Material.TALL_DRY_GRASS);
        else if (roll < 175) region.setType(x, y, z, Material.LEAF_LITTER);
    }

    private void tall(LimitedRegion region, int x, int y, int z, Material type) {
        if (!EndTrees.canPlace(region, x, y + 1, z)) return;
        Bisected lower = (Bisected) type.createBlockData();
        lower.setHalf(Bisected.Half.BOTTOM);
        Bisected upper = (Bisected) type.createBlockData();
        upper.setHalf(Bisected.Half.TOP);
        region.setBlockData(x, y, z, lower);
        region.setBlockData(x, y + 1, z, upper);
    }

    // Pinos (2 intentos por chunk) y una geoda de amatista en más o menos uno de cada 3 chunks
    private void prismaticFeatures(LimitedRegion region, Random r, int baseX, int baseZ, int[] top) {
        for (int i = 0; i < 2; i++) {
            if (r.nextInt(100) >= 70) continue;
            int x = 2 + r.nextInt(12);
            int z = 2 + r.nextInt(12);
            int y = top[x << 4 | z];
            if (y == Integer.MIN_VALUE || region.getType(baseX + x, y, baseZ + z) != Material.GRASS_BLOCK) continue;
            clearPlant(region, baseX + x, y + 1, baseZ + z);
            EndTrees.pine(region, r, baseX + x, y + 1, baseZ + z);
        }
        if (r.nextInt(100) < 35) {
            int x = 3 + r.nextInt(10);
            int z = 3 + r.nextInt(10);
            int y = top[x << 4 | z];
            if (y != Integer.MIN_VALUE && region.getType(baseX + x, y, baseZ + z) == Material.GRASS_BLOCK) {
                EndStructures.amethystBall(region, r, baseX + x, y, baseZ + z);
            }
        }
    }

    // Un árbol marchito en la mitad de los chunks y cada tanto un santuario o una aguja de obsidiana con shulkers negros
    private void witheredFeatures(LimitedRegion region, Random r, int baseX, int baseZ, int[] top) {
        if (r.nextInt(100) < 55) {
            int x = 3 + r.nextInt(10);
            int z = 3 + r.nextInt(10);
            int y = top[x << 4 | z];
            if (y != Integer.MIN_VALUE) {
                clearPlant(region, baseX + x, y + 1, baseZ + z);
                EndTrees.withered(region, r, baseX + x, y + 1, baseZ + z);
            }
        }
        if (r.nextInt(100) < 12) {
            int x = 6 + r.nextInt(4);
            int z = 6 + r.nextInt(4);
            int y = top[x << 4 | z];
            if (y == Integer.MIN_VALUE) return;
            EndStructures.ShulkerSpot spot = (sx, sy, sz) -> shulker(region, sx, sy, sz);
            if (r.nextInt(3) == 0) EndStructures.spire(region, r, baseX + x, y + 1, baseZ + z, spot);
            else EndStructures.shrine(region, r, baseX + x, y + 1, baseZ + z, spot);
        }
    }

    private void clearPlant(LimitedRegion region, int x, int y, int z) {
        for (int i = 0; i < 2; i++) {
            if (!region.isInRegion(x, y + i, z)) return;
            Material type = region.getType(x, y + i, z);
            if (!type.isAir() && !type.isSolid() && type != Material.WATER) region.setType(x, y + i, z, Material.AIR);
        }
    }

    private void shulker(LimitedRegion region, int x, int y, int z) {
        if (!EndTrees.canPlace(region, x, y, z)) return;
        region.spawn(new Location(null, x + 0.5, y, z + 0.5), Shulker.class, blackShulker::setup);
    }
}
