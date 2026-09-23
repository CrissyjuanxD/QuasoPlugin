package InfestedCaves;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

public class WardenBiomeProvider extends BiomeProvider {

    private final Biome[] biomes;
    private final List<Biome> list;

    public WardenBiomeProvider() {
        WardenBiome[] values = WardenBiome.values();
        biomes = new Biome[values.length];
        for (int i = 0; i < values.length; i++) {
            biomes[i] = values[i].resolve();
        }
        list = List.of(biomes);
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return biomes[WardenBiomeMap.forSeed(worldInfo.getSeed()).biomeAt(x, z).ordinal()];
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return list;
    }
}
