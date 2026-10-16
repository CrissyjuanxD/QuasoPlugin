package studios.viciont.quasogeyser;

// Geyser-Spigot relocates Adventure and fastutil; compile against that distribution.
import org.geysermc.geyser.platform.spigot.shaded.net.kyori.adventure.key.Key;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** Visual translation only: never registers global overrides or edits the Java world. */
public final class QuasoWardenCave implements Extension {
    private final Map<String, CustomBlockData> blocks = new LinkedHashMap<>();
    private final Map<BlockMappings, BlockMappings> wardenMappings = new IdentityHashMap<>();
    private final Map<BlockMappings, BlockMappings> originals = new IdentityHashMap<>();
    private Set<String> worlds = Set.of("minecraft:wardencave");

    @Subscribe
    public void define(GeyserDefineCustomBlocksEvent event) {
        blocks.clear();
        wardenMappings.clear();
        originals.clear();
        try {
            readConfig();
            requirePack();
            for (String color : new String[]{"cyan", "green", "purple", "gray"}) {
                register(event, "ore_" + color, "Mineral de Warden Cave", false, 0, 1.4f);
            }
            register(event, "baya_sculk", "Baya Sculk", false, 15, 0.3f);
            register(event, "fruta_abisal", "Fruta Abisal", false, 15, 0.3f);
            register(event, "baya_luminosa", "Baya Regenerativa", true, 14, 0f);
            logger().info("Registradas 7 apariencias de Warden Cave, sin reemplazos globales.");
        } catch (IOException | RuntimeException e) {
            blocks.clear();
            logger().error("No se pudo preparar Warden Cave. Se conserva la apariencia normal.", e);
        }
    }

    private void requirePack() throws IOException {
        Path directory = geyserApi().packDirectory();
        if (Files.isDirectory(directory)) {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    if (!path.toString().endsWith(".mcpack") && !path.toString().endsWith(".zip")) continue;
                    try (ZipFile zip = new ZipFile(path.toFile())) {
                        if (zip.getEntry("models/blocks/warden_vine.geo.json") != null
                                && Stream.of("ore_cyan", "ore_green", "ore_purple", "ore_gray",
                                        "baya_sculk", "fruta_abisal", "baya_luminosa")
                                .allMatch(name -> zip.getEntry("textures/blocks/quaso/" + name + ".png") != null)) return;
                    } catch (IOException ignored) { /* Geyser reports invalid unrelated packs itself. */ }
                }
            }
        }
        throw new IOException("Falta QuasoWardenCave-1.0.0.mcpack en " + directory);
    }

    private void readConfig() throws IOException {
        Files.createDirectories(dataFolder());
        Path file = dataFolder().resolve("config.properties");
        if (!Files.exists(file)) {
            Files.writeString(file, "# Mundos Java exactos, separados por comas. Reinicia Geyser tras cambiarlos.\n"
                    + "worlds=minecraft:wardencave\n", StandardCharsets.UTF_8);
        }
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { config.load(reader); }
        worlds = Stream.of(config.getProperty("worlds", "minecraft:wardencave").split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> Key.key(s).asString()).collect(Collectors.toUnmodifiableSet());
        if (worlds.isEmpty()) throw new IOException("La lista worlds no puede estar vacía.");
    }

    private void register(GeyserDefineCustomBlocksEvent event, String name, String label,
                          boolean vine, int light, float hardness) {
        var components = CustomBlockComponents.builder()
                .displayName(label).destructibleByMining(hardness).friction(0.4f)
                .lightEmission(light).lightDampening(vine ? 0 : 15)
                .collisionBox(vine ? BoxComponent.emptyBox() : new BoxComponent(-8, 0, -8, 16, 16, 16))
                .selectionBox(new BoxComponent(vine ? -7 : -8, 0, vine ? -7 : -8,
                        vine ? 14 : 16, 16, vine ? 14 : 16))
                .materialInstance("*", MaterialInstance.builder().texture("quaso_wc_" + name)
                        .renderMethod(vine ? "alpha_test" : "opaque")
                        .faceDimming(true).ambientOcclusion(true).build());
        if (vine) components.geometry(GeometryComponent.builder().identifier("geometry.quaso.warden_vine").build());
        else components.unitCube(true);
        var block = CustomBlockData.builder().name("quaso_wc_" + name)
                .includedInCreativeInventory(false).components(components.build()).build();
        event.register(block);
        blocks.put(name, block);
    }

    @Subscribe
    public void initialized(GeyserPostInitializeEvent event) {
        if (blocks.size() != 7) {
            logger().warning("Warden Cave no está activa: revisa enable-custom-content y los mensajes anteriores.");
            return;
        }
        try {
            var states = selectedStates();
            // Construct and verify everything before installing either translator.
            for (BlockMappings base : BlockRegistries.BLOCKS.get().values()) {
                GeyserBedrockBlock[] translated = base.getJavaToBedrockBlocks().clone();
                for (var entry : states.entrySet()) {
                    translated[entry.getKey()] = Objects.requireNonNull(base.getCustomBlockStateDefinitions()
                            .get(blocks.get(entry.getValue()).defaultBlockState()), "Falta el estado " + entry.getValue());
                }
                BlockMappings mapped = copy(base, translated);
                verify(base, mapped, states);
                wardenMappings.put(base, mapped);
                originals.put(mapped, base);
            }
            verifyWorldSelection();
            Objects.requireNonNull(Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundLoginPacket.class));
            Objects.requireNonNull(Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundRespawnPacket.class));
            wrap(ClientboundLoginPacket.class, packet -> packet.getCommonPlayerSpawnInfo().getWorldName());
            wrap(ClientboundRespawnPacket.class, packet -> packet.getCommonPlayerSpawnInfo().getWorldName());
            logger().info("Warden Cave activa para " + worlds + ": " + states.size()
                    + " estados por protocolo, " + wardenMappings.size()
                    + " protocolos verificados. Fuera de esos mundos se restaura el mapping original.");
        } catch (RuntimeException | LinkageError e) {
            logger().error("Esta versión de Geyser no pudo activar la traducción de Warden Cave.", e);
        }
    }

    static String appearance(String state) {
        int bracket = state.indexOf('[');
        String material = bracket < 0 ? state : state.substring(0, bracket);
        return switch (material) {
            case "minecraft:cyan_glazed_terracotta" -> "ore_cyan";
            case "minecraft:lime_glazed_terracotta" -> "ore_green";
            case "minecraft:purple_glazed_terracotta" -> "ore_purple";
            case "minecraft:gray_glazed_terracotta" -> "ore_gray";
            case "minecraft:verdant_froglight" -> "baya_sculk";
            case "minecraft:pearlescent_froglight" -> "fruta_abisal";
            case "minecraft:cave_vines", "minecraft:cave_vines_plant" ->
                    state.contains("berries=true") ? "baya_luminosa" : null;
            default -> null;
        };
    }

    private Map<Integer, String> selectedStates() {
        Map<Integer, String> result = new HashMap<>();
        BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().forEach((state, id) -> {
            String name = appearance(state);
            if (name != null) result.put(id, name);
        });
        if (!Set.copyOf(result.values()).equals(blocks.keySet())) {
            throw new IllegalStateException("No se encontraron los siete tipos de bloque en Geyser.");
        }
        return Collections.unmodifiableMap(result);
    }

    BlockMappings select(BlockMappings current, Key world) {
        BlockMappings base = originals.getOrDefault(current, current);
        return world != null && worlds.contains(world.asString()) ? wardenMappings.getOrDefault(base, base) : base;
    }

    @SuppressWarnings("unchecked")
    private <T extends Packet> void wrap(Class<T> type, Function<T, Key> world) {
        var delegate = (PacketTranslator<T>) Objects.requireNonNull(Registries.JAVA_PACKET_TRANSLATORS.get(type));
        if (delegate instanceof WorldTranslator<?> wrapper) delegate = (PacketTranslator<T>) wrapper.delegate;
        Registries.JAVA_PACKET_TRANSLATORS.register(type, new WorldTranslator<>(delegate, world));
    }

    private final class WorldTranslator<T extends Packet> extends PacketTranslator<T> {
        private final PacketTranslator<T> delegate;
        private final Function<T, Key> world;
        private WorldTranslator(PacketTranslator<T> delegate, Function<T, Key> world) {
            this.delegate = delegate;
            this.world = world;
        }
        @Override public void translate(GeyserSession session, T packet) {
            // Runs on the same Geyser event loop, before the login/respawn and subsequent chunks.
            session.setBlockMappings(select(session.getBlockMappings(), world.apply(packet)));
            delegate.translate(session, packet);
        }
    }

    private void verifyWorldSelection() {
        for (var entry : wardenMappings.entrySet()) {
            BlockMappings base = entry.getKey(), custom = entry.getValue();
            for (String world : worlds) {
                if (select(base, Key.key(world)) != custom || select(custom, Key.key(world)) != custom)
                    throw new IllegalStateException("Falló entrar o reaparecer en Warden Cave.");
            }
            if (select(custom, Key.key("quaso_test:outside")) != base || select(base, null) != base)
                throw new IllegalStateException("No se restaura el mapping al salir.");
        }
    }

    private static void verify(BlockMappings base, BlockMappings mapped, Map<Integer, String> states) {
        if (base.getJavaToBedrockBlocks() == mapped.getJavaToBedrockBlocks())
            throw new IllegalStateException("Los arrays no pueden compartirse.");
        for (int i = 0; i < base.getJavaToBedrockBlocks().length; i++) {
            if (states.containsKey(i)) {
                if (base.getBedrockBlock(i) == mapped.getBedrockBlock(i) || !base.isRegistered(mapped.getBedrockBlock(i)))
                    throw new IllegalStateException("Estado custom inválido: " + i);
            } else if (base.getBedrockBlock(i) != mapped.getBedrockBlock(i)) {
                throw new IllegalStateException("Se ha alterado un bloque ajeno: " + i);
            }
        }
    }

    private static BlockMappings copy(BlockMappings b, GeyserBedrockBlock[] translated) {
        return BlockMappings.builder().bedrockAir(b.getBedrockAir()).bedrockWater(b.getBedrockWater())
                .bedrockMovingBlock(b.getBedrockMovingBlock()).javaToBedrockBlocks(translated)
                .javaToVanillaBedrockBlocks(b.getJavaToVanillaBedrockBlocks())
                .javaToBedrockIdentifiers(b.getJavaToBedrockIdentifiers()).stateDefinitionMap(b.getStateDefinitionMap())
                .bedrockRuntimeMap(b.getBedrockRuntimeMap()).remappedVanillaIds(b.getRemappedVanillaIds())
                .commandBlock(b.getCommandBlock()).mobSpawnerBlock(b.getMobSpawnerBlock())
                .netherPortalBlock(b.getNetherPortalBlock()).collisionIgnoredBlocks(b.getCollisionIgnoredBlocks())
                .itemFrames(b.getItemFrames()).flowerPotBlocks(b.getFlowerPotBlocks()).jigsawStates(b.getJigsawStates())
                .structureBlockStates(b.getStructureBlockStates()).blockProperties(b.getBlockProperties())
                .customBlockStateDefinitions(b.getCustomBlockStateDefinitions()).extendedCollisionBoxes(b.getExtendedCollisionBoxes())
                .build();
    }
}
