package InfestedCaves;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WardenPopulator extends BlockPopulator {

    private static final Set<Material> SCULK_SOIL = EnumSet.of(
            Material.SCULK, Material.CRYING_OBSIDIAN, Material.SCULK_CATALYST);
    private static final Set<Material> SWAMP_SOIL = EnumSet.of(
            Material.MOSS_BLOCK, Material.MUD, Material.SCULK);
    private static final Set<Material> ABYSS_SOIL = EnumSet.of(
            Material.DEEPSLATE, Material.CALCITE, Material.AMETHYST_BLOCK, Material.SCULK);
    private static final Set<Material> ASH_SOIL = EnumSet.of(
            Material.BASALT, Material.BLACKSTONE, Material.TUFF, Material.SMOOTH_BASALT, Material.SCULK,
            Material.OBSIDIAN, Material.CRYING_OBSIDIAN);

    // Pone los árboles de cada bioma y las ruinas de obsidiana de las Ruinas de Ceniza. No toca el spawn del templo
    // ni la caverna de las Ancient City (arriba de su techo sí)
    @Override
    public void populate(WorldInfo info, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        long seed = info.getSeed();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int centerX = baseX + 8;
        int centerZ = baseZ + 8;

        double distToSpawn = Math.sqrt((double) centerX * centerX + (double) centerZ * centerZ);
        if (distToSpawn < WardenGenerator.SPAWN_RADIUS + WardenGenerator.SPAWN_TRANSITION + 20) return;

        AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(seed, centerX, centerZ);
        int minY = city != null && AncientCityLocator.computeInfluence(city, centerX, centerZ) > 0
                ? AncientCityLocator.MAX_Y + 34 : WardenGenerator.MIN_Y + 1;

        WardenBiome biome = WardenBiomeMap.forSeed(seed).biomeAt(centerX, centerZ);
        int attempts = biome == WardenBiome.CAVERNA_SCULK ? 3 : 2;

        if (biome == WardenBiome.RUINAS_DE_CENIZA && random.nextInt(100) < 30) {
            for (int i = 0; i < 6; i++) {
                int x = baseX + 5 + random.nextInt(6);
                int z = baseZ + 5 + random.nextInt(6);
                int y = findSoil(region, ASH_SOIL, x, z, random, minY);
                if (y != Integer.MIN_VALUE && AshRuins.random(region, random, x, y + 1, z)) break;
            }
        }

        for (int i = 0; i < attempts; i++) {
            int x = baseX + 3 + random.nextInt(10);
            int z = baseZ + 3 + random.nextInt(10);
            int y = findSoil(region, soilFor(biome), x, z, random, minY);
            if (y == Integer.MIN_VALUE) continue;

            switch (biome) {
                case CAVERNA_SCULK -> WardenTrees.sculk(region, random, x, y + 1, z);
                case PANTANO_PROFUNDO -> WardenTrees.swamp(region, random, x, y + 1, z);
                case ABISMO_FLOTANTE -> WardenTrees.crystal(region, random, x, y + 1, z);
                case RUINAS_DE_CENIZA -> WardenTrees.ash(region, random, x, y + 1, z);
            }
        }
    }

    private Set<Material> soilFor(WardenBiome biome) {
        return switch (biome) {
            case CAVERNA_SCULK -> SCULK_SOIL;
            case PANTANO_PROFUNDO -> SWAMP_SOIL;
            case ABISMO_FLOTANTE -> ABYSS_SOIL;
            case RUINAS_DE_CENIZA -> ASH_SOIL;
        };
    }

    // Elige al azar un suelo válido de la columna (desde minY) que tenga al menos 4 bloques de aire arriba
    private int findSoil(LimitedRegion region, Set<Material> soil, int x, int z, Random random, int minY) {
        List<Integer> candidates = new ArrayList<>();
        for (int y = minY; y < WardenGenerator.TOP_Y - 12; y++) {
            if (!soil.contains(region.getType(x, y, z))) continue;
            boolean clear = true;
            for (int dy = 1; dy <= 4; dy++) {
                if (!region.getType(x, y + dy, z).isAir()) {
                    clear = false;
                    break;
                }
            }
            if (clear) candidates.add(y);
        }
        if (candidates.isEmpty()) return Integer.MIN_VALUE;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
