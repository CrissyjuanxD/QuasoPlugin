package EndBiomes;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;

// Los biomas nuevos del End (vienen en el datapack). El Bosque Prismático tiene una variante por color: el juego tiñe
// las hojas con el color del bioma, así cada mancha del bosque tiene sus pinos de un color
public enum EndBiome {
    PRISMATICO_ROSA("bosque_prismatico_rosa"),
    PRISMATICO_VERDE("bosque_prismatico_verde"),
    PRISMATICO_NARANJA("bosque_prismatico_naranja"),
    PRISMATICO_AMARILLO("bosque_prismatico_amarillo"),
    PRISMATICO_ROJO("bosque_prismatico_rojo"),
    PRISMATICO_MORADO("bosque_prismatico_morado"),
    PARAMO_MARCHITO("paramo_marchito");

    public static final EndBiome[] PRISMATIC = {
            PRISMATICO_ROSA, PRISMATICO_VERDE, PRISMATICO_NARANJA, PRISMATICO_AMARILLO, PRISMATICO_ROJO, PRISMATICO_MORADO};

    private final NamespacedKey key;
    private volatile Biome cached;

    EndBiome(String id) {
        this.key = new NamespacedKey("quaso", id);
    }

    public NamespacedKey key() {
        return key;
    }

    // El bioma del registro, o null si el datapack todavía no se cargó
    public Biome get() {
        if (cached == null) cached = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key);
        return cached;
    }

    public static boolean isPrismatic(Biome biome) {
        return biome != null && biome.getKey().getNamespace().equals("quaso") && biome.getKey().getKey().startsWith("bosque_prismatico");
    }

    public static boolean isParamo(Biome biome) {
        return biome != null && biome.getKey().equals(PARAMO_MARCHITO.key);
    }
}
