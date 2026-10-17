package studios.viciont.quasogeyser;

import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.registry.Registries;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registro de los cuatro biomas que ya tienen recursos en QuasoTextures 1.1.0. */
final class CustomBiomes {
    // Bedrock reserva los IDs de biomas personalizados a partir de 30000.
    // 1.1.0 usaba 1024 y podía provocar InitialConnection-90 (Block) al entrar.
    // Referencia de protocolo: GeyserMC/Geyser#6658, CustomBiomeCache.FIRST_CUSTOM_ID.
    private static final int FIRST_CUSTOM_ID = 30000;
    private static final int MAX_ID = Short.MAX_VALUE;
    private static final int MAX_POOLED_STRINGS = 1024;
    private static final String[] NAMES = {"caverna_sculk", "pantano_profundo", "ruinas_de_ceniza", "abismo_flotante"};
    private static final int[] WATER = {0x1f7a86, 0x2f7a34, 0x5b5c63, 0x6a4fa0};

    record Registration(BiomeDefinitions definitions, Map<String, Integer> identifiers) {}

    static void register() {
        // Prepara y valida las dos tablas antes de publicar cambios. Si falla, no se
        // anuncian IDs para los que el cliente no vaya a recibir una definición.
        Registration prepared = prepare(Registries.BIOMES.get(), Registries.BIOME_IDENTIFIERS.get());
        Registries.BIOMES.set(prepared.definitions());
        Registries.BIOME_IDENTIFIERS.get().putAll(prepared.identifiers());
    }

    static Registration prepare(BiomeDefinitions source, Map<String, Integer> sourceIdentifiers) {
        Map<String, BiomeDefinitionData> definitions = new LinkedHashMap<>(source.getDefinitions());
        Map<String, Integer> identifiers = new LinkedHashMap<>(sourceIdentifiers);
        var occupied = new HashSet<>(identifiers.values());
        definitions.values().forEach(data -> { if (data.getId() != null) occupied.add(data.getId()); });
        BiomeDefinitionData reference = Objects.requireNonNull(definitions.get("minecraft:plains"), "Falta plains");
        int next = FIRST_CUSTOM_ID;

        for (int i = 0; i < NAMES.length; i++) {
            String name = "quaso:" + NAMES[i];
            BiomeDefinitionData existing = definitions.get(name);
            Integer mapped = identifiers.get(name);
            if (existing != null || mapped != null) {
                if (existing == null || mapped == null || mapped < FIRST_CUSTOM_ID || mapped > MAX_ID
                        || !mapped.equals(existing.getId())) {
                    throw new IllegalStateException("Registro de bioma incompatible: " + name + ". Reinicia el servidor sin la extensión antigua.");
                }
                continue;
            }
            while (next <= MAX_ID && occupied.contains(next)) next++;
            if (next > MAX_ID) throw new IllegalStateException("No quedan IDs Bedrock de biomas personalizados.");
            int id = next++;
            occupied.add(id);
            // Conserva los campos propios del protocolo a partir de una definición vanilla.
            // chunkGenData no se envía: el mundo lo genera Java.
            definitions.put(name, new BiomeDefinitionData(id, .5f, 0f, reference.getFoliageSnow(),
                    reference.getDepth(), reference.getScale(), new Color(WATER[i]), false,
                    List.of("overworld"), null));
            identifiers.put(name, id);
        }

        var strings = new HashSet<>(definitions.keySet());
        definitions.values().forEach(data -> { if (data.getTags() != null) strings.addAll(data.getTags()); });
        if (strings.size() > MAX_POOLED_STRINGS) {
            throw new IllegalStateException("El registro de biomas supera el límite de cadenas del protocolo.");
        }
        return new Registration(new BiomeDefinitions(definitions), identifiers);
    }
}
