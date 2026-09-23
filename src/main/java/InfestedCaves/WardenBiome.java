package InfestedCaves;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import items.WardenCaveItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

public enum WardenBiome {
    CAVERNA_SCULK("caverna_sculk", Material.CYAN_GLAZED_TERRACOTTA),
    PANTANO_PROFUNDO("pantano_profundo", Material.LIME_GLAZED_TERRACOTTA),
    ABISMO_FLOTANTE("abismo_flotante", Material.PURPLE_GLAZED_TERRACOTTA),
    RUINAS_DE_CENIZA("ruinas_de_ceniza", Material.GRAY_GLAZED_TERRACOTTA);

    private final String id;
    private final Material ore;

    WardenBiome(String id, Material ore) {
        this.id = id;
        this.ore = ore;
    }

    public NamespacedKey key() {
        return new NamespacedKey("quaso", id);
    }

    public Material ore() {
        return ore;
    }

    // El mineral y el fragmento de cada bioma (Cian casco, Verde botas, Morado peto y Gris pantalón)
    public WardenCaveItems.Variant variant() {
        return switch (this) {
            case CAVERNA_SCULK -> WardenCaveItems.Variant.CIAN;
            case PANTANO_PROFUNDO -> WardenCaveItems.Variant.VERDE;
            case ABISMO_FLOTANTE -> WardenCaveItems.Variant.MORADO;
            case RUINAS_DE_CENIZA -> WardenCaveItems.Variant.GRIS;
        };
    }

    public boolean isLoaded() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key()) != null;
    }

    // El bioma del datapack; si no está cargado usa uno vanilla parecido para que el mundo igual se genere
    public Biome resolve() {
        Biome custom = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key());
        if (custom != null) return custom;
        return switch (this) {
            case CAVERNA_SCULK -> Biome.DEEP_DARK;
            case PANTANO_PROFUNDO -> Biome.LUSH_CAVES;
            case ABISMO_FLOTANTE -> Biome.END_HIGHLANDS;
            case RUINAS_DE_CENIZA -> Biome.BASALT_DELTAS;
        };
    }

    public static WardenBiome fromOre(Material material) {
        for (WardenBiome biome : values()) {
            if (biome.ore == material) return biome;
        }
        return null;
    }
}
