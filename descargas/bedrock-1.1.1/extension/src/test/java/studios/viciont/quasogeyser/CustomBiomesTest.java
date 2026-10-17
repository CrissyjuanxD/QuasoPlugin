package studios.viciont.quasogeyser;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;
import org.cloudburstmc.protocol.bedrock.codec.v2193.Bedrock_v2193;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.geysermc.geyser.registry.loader.BiomeLoader;

import java.awt.Color;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Usa las definiciones vanilla y los codecs reales del JAR Geyser que se comprueba. */
public final class CustomBiomesTest {
    public static void main(String[] args) throws Exception {
        var gson = new GsonBuilder().registerTypeAdapter(Color.class, new BiomeLoader.ColorTypeAdapter()).create();
        Map<String, BiomeDefinitionData> vanilla;
        try (var reader = new InputStreamReader(CustomBiomesTest.class.getResourceAsStream(
                "/bedrock/stripped_biome_definitions.json"), StandardCharsets.UTF_8)) {
            vanilla = gson.fromJson(reader, new TypeToken<Map<String, BiomeDefinitionData>>() {}.getType());
        }
        check(vanilla.size() > 80, "Debe cargar el registro vanilla real");
        BiomeDefinitions source = new BiomeDefinitions(vanilla);
        Map<String, Integer> mappings = Map.of("minecraft:plains", 1);
        var prepared = CustomBiomes.prepare(source, mappings);
        var definitions = prepared.definitions().getDefinitions();
        check(definitions.size() == vanilla.size() + 4, "Sólo se añaden los cuatro biomas Quaso");
        check(source.getDefinitions().size() == vanilla.size() && mappings.size() == 1, "No muta las tablas de entrada");
        vanilla.forEach((name, data) -> check(definitions.get(name) == data, "Definición vanilla intacta: " + name));
        var ids = new HashSet<Integer>();
        definitions.forEach((name, data) -> {
            if (!name.startsWith("quaso:")) return;
            check(data.getId() >= 30000 && data.getId() <= Short.MAX_VALUE, "ID fuera del rango custom Bedrock");
            check(ids.add(data.getId()), "ID repetido");
            check(data.getId().equals(prepared.identifiers().get(name)), "El chunk y la definición deben usar el mismo ID");
            check(data.getChunkGenData() == null, "No se envía generación Bedrock de un mundo Java");
        });
        check(ids.equals(java.util.Set.of(30000, 30001, 30002, 30003)), "Regresión: no volver a asignar desde 1024");

        // Comprueba el paquete completo, no sólo los valores antes de serializar.
        for (BedrockCodec codec : List.of(Bedrock_v1001.CODEC, Bedrock_v2168.CODEC, Bedrock_v2193.CODEC)) {
            var packet = new BiomeDefinitionListPacket(); packet.setBiomes(prepared.definitions());
            var buffer = Unpooled.buffer();
            try {
                codec.tryEncode(codec.createHelper(), buffer, packet);
                var decoded = (BiomeDefinitionListPacket) codec.tryDecode(codec.createHelper(), buffer,
                        codec.getPacketDefinition(BiomeDefinitionListPacket.class).getId());
                check(!buffer.isReadable(), "El codec debe consumir todo el paquete");
                check(decoded.getBiomes().getDefinitions().keySet().equals(definitions.keySet()), "El registro debe llegar completo");
                for (String name : definitions.keySet()) {
                    var expected = definitions.get(name); var actual = decoded.getBiomes().getDefinitions().get(name);
                    check(java.util.Objects.equals(expected.getId(), actual.getId()), "ID alterado en el protocolo " + codec.getProtocolVersion());
                    check(expected.getMapWaterColor().equals(actual.getMapWaterColor()), "Color alterado");
                }
                System.out.println("OK: BiomeDefinitionListPacket " + codec.getProtocolVersion() + ", " + definitions.size()
                        + " definiciones completas e IDs Quaso 30000–30003.");
            } finally { buffer.release(); }
        }

        var again = CustomBiomes.prepare(prepared.definitions(), prepared.identifiers());
        check(again.identifiers().equals(prepared.identifiers()), "Registrar de nuevo no reasigna IDs");
        var occupiedMappings = new HashMap<>(mappings); occupiedMappings.put("otro:bioma", 30000);
        var occupiedDefinitions = new LinkedHashMap<>(vanilla);
        var sample = definitions.get("quaso:caverna_sculk");
        occupiedDefinitions.put("otro:solo_definicion", withId(sample, 30001));
        var collision = CustomBiomes.prepare(new BiomeDefinitions(occupiedDefinitions), occupiedMappings);
        check(collision.identifiers().get("quaso:caverna_sculk") == 30002, "Evita IDs ocupados en ambas tablas");
        check(collision.identifiers().get("otro:bioma") == 30000, "Conserva otros registros");

        var legacy = new HashMap<>(mappings); legacy.put("quaso:caverna_sculk", 1024);
        rejects(() -> CustomBiomes.prepare(source, legacy), "No aceptar un registro antiguo incompleto");
        check(legacy.get("quaso:caverna_sculk") == 1024, "Un error no publica cambios parciales");
        var exhausted = new HashMap<>(mappings);
        for (int i = 30000; i <= Short.MAX_VALUE; i++) exhausted.put("otro:" + i, i);
        rejects(() -> CustomBiomes.prepare(source, exhausted), "No exceder el short firmado del protocolo");
        var crowded = new LinkedHashMap<>(vanilla);
        for (int i = 0; i < 1024; i++) crowded.put("otro:bioma_" + i, sample);
        rejects(() -> CustomBiomes.prepare(new BiomeDefinitions(crowded), mappings), "No exceder el pool de cadenas");
        System.out.println("OK: rango custom, vanilla intacto, repetición, colisiones, límites y fallos sin mutación parcial.");
    }

    private static BiomeDefinitionData withId(BiomeDefinitionData sample, int id) {
        return new BiomeDefinitionData(id, sample.getTemperature(), sample.getDownfall(), sample.getFoliageSnow(),
                sample.getDepth(), sample.getScale(), sample.getMapWaterColor(), sample.isRain(), sample.getTags(), null);
    }
    private static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError(message);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
