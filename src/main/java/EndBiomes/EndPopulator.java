package EndBiomes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Shulker;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

// Los biomas nuevos del End se ponen sobre el End vanilla: cuando se genera un chunk de las islas de afuera que cae
// en una región nueva se le cambia el bioma, se le saca el chorus, la end stone de arriba pasa al suelo del bioma
// (tierra, tierra de almas o nieve) y se le ponen los árboles, las plantas y las estructuras
public class EndPopulator extends BlockPopulator {

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

        EndSuelo.Bloques blocks = EndSuelo.de(region);
        EndSuelo.Ruido ruido = EndSuelo.Ruido.of(info.getSeed());
        int[] top = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                top[col] = Integer.MIN_VALUE;
                EndBiome biome = columns[col];
                if (biome == null) continue;
                removeChorus(region, baseX + x, baseZ + z);
                top[col] = EndSuelo.suelo(blocks, random, ruido, baseX + x, baseZ + z, biome);
                if (biome == EndBiome.PICOS_HELADOS && top[col] != Integer.MIN_VALUE) {
                    top[col] = EndSuelo.dunas(blocks, ruido, baseX + x, top[col], baseZ + z);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                if (top[col] != Integer.MIN_VALUE) EndSuelo.planta(blocks, random, baseX + x, top[col] + 1, baseZ + z, columns[col]);
            }
        }

        EndBiome center = columns[8 << 4 | 8];
        if (center == null) return;
        switch (center) {
            case PARAMO_MARCHITO -> witheredFeatures(region, random, baseX, baseZ, top);
            case PICOS_HELADOS -> iceFeatures(region, random, baseX, baseZ, top);
            default -> prismaticFeatures(region, random, baseX, baseZ, top, center);
        }
    }

    // Saca el chorus de la columna entera (también lo que cruza a los chunks de al lado): si quedara un pedazo sin su
    // base, al primer cambio de bloque se rompería y dejaría frutas de chorus tiradas
    private void removeChorus(LimitedRegion region, int x, int z) {
        for (int y = EndSuelo.MAX_Y; y > 1; y--) {
            if (!isChorus(region.getType(x, y, z))) continue;
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
    }

    private static boolean isChorus(Material type) {
        return type == Material.CHORUS_PLANT || type == Material.CHORUS_FLOWER;
    }

    // Bosque Prismático (como el bioma colorido de Stellarity): de 2 a 4 árboles por chunk de muchos colores, un tercio
    // arbustos, y cada tanto una roca de diorita o una geoda de amatista (de ahí sale la Celestita)
    private void prismaticFeatures(LimitedRegion region, Random r, int baseX, int baseZ, int[] top, EndBiome biome) {
        int roll = r.nextInt(10);
        int trees = roll < 5 ? 2 : roll < 9 ? 3 : 4;
        for (int i = 0; i < trees; i++) {
            int x = 2 + r.nextInt(12);
            int z = 2 + r.nextInt(12);
            int y = top[x << 4 | z];
            if (y == Integer.MIN_VALUE || region.getType(baseX + x, y, baseZ + z) != Material.GRASS_BLOCK) continue;
            clearPlant(region, baseX + x, y + 1, baseZ + z);
            int kind = r.nextInt(100);
            if (kind < 33) {
                EndTrees.bush(region, r, baseX + x, y + 1, baseZ + z);
                continue;
            }
            EndTrees.Copa copa = EndTrees.color(biome, r);
            if (kind < 63) EndTrees.fancy(region, r, baseX + x, y + 1, baseZ + z, copa);
            else if (kind < 88) EndTrees.pine(region, r, baseX + x, y + 1, baseZ + z, copa);
            else EndTrees.jungle(region, r, baseX + x, y + 1, baseZ + z, copa);
        }
        if (r.nextInt(100) < 35) {
            int x = 3 + r.nextInt(10);
            int z = 3 + r.nextInt(10);
            int y = top[x << 4 | z];
            if (y != Integer.MIN_VALUE && region.getType(baseX + x, y, baseZ + z) == Material.GRASS_BLOCK) {
                EndStructures.amethystBall(region, r, baseX + x, y, baseZ + z);
            }
        } else if (r.nextInt(100) < 25) {
            int x = 3 + r.nextInt(10);
            int z = 3 + r.nextInt(10);
            int y = top[x << 4 | z];
            if (y != Integer.MIN_VALUE) EndStructures.rock(region, r, baseX + x, y + 1, baseZ + z);
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

    // Picos Helados: un pico de hielo en más o menos 2 de cada 3 chunks, carámbanos debajo de las islas y bolsones de
    // nieve polvo
    private void iceFeatures(LimitedRegion region, Random r, int baseX, int baseZ, int[] top) {
        for (int i = 0; i < 2; i++) {
            if (r.nextInt(100) >= 35) continue;
            int x = 4 + r.nextInt(8);
            int z = 4 + r.nextInt(8);
            int y = top[x << 4 | z];
            if (y == Integer.MIN_VALUE || region.getType(baseX + x, y, baseZ + z) != Material.SNOW_BLOCK) continue;
            clearPlant(region, baseX + x, y + 1, baseZ + z);
            EndHielo.spike(region, r, baseX + x, y + 1, baseZ + z);
        }
        for (int i = 0; i < 2; i++) {
            if (r.nextInt(100) >= 40) continue;
            int x = 3 + r.nextInt(10);
            int z = 3 + r.nextInt(10);
            int y = top[x << 4 | z];
            if (y == Integer.MIN_VALUE) continue;
            int bottom = EndHielo.underside(region, baseX + x, y, baseZ + z);
            if (bottom != Integer.MIN_VALUE && bottom < y - 2) EndHielo.icicle(region, r, baseX + x, bottom - 1, baseZ + z);
        }
        for (int i = 0; i < 3; i++) {
            int x = 2 + r.nextInt(12);
            int z = 2 + r.nextInt(12);
            int y = top[x << 4 | z];
            if (y != Integer.MIN_VALUE) EndHielo.powderSnow(region, r, baseX + x, y, baseZ + z);
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
