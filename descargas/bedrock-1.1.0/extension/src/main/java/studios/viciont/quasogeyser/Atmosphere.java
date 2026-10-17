package studios.viciont.quasogeyser;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** La API de cámara envía niebla por jugador. Los biomas tienen identidades independientes. */
final class Atmosphere {
    private final QuasoWardenCave extension;
    private final Map<UUID, FogState> fogs = new HashMap<>();
    private BukkitTask task;
    private boolean warned;
    private Plugin quaso;
    private Method getBloodMoon, isActive;
    private static final class FogState {
        UUID world;
        int step;
        String sent;
    }
    Atmosphere(QuasoWardenCave extension) { this.extension = extension; }

    static void registerBiomes() {
        Map<String, BiomeDefinitionData> definitions = new LinkedHashMap<>(Registries.BIOMES.get().getDefinitions());
        int next = Math.max(1024, Registries.BIOME_IDENTIFIERS.get().values().intStream().max().orElse(0) + 1);
        String[] names = {"caverna_sculk", "pantano_profundo", "ruinas_de_ceniza", "abismo_flotante"};
        int[] water = {0x1f7a86, 0x2f7a34, 0x5b5c63, 0x6a4fa0};
        for (int i = 0; i < names.length; i++) {
            String identifier = "quaso:" + names[i];
            if (Registries.BIOME_IDENTIFIERS.get().containsKey(identifier)) continue;
            int id = next++;
            definitions.put(identifier, new BiomeDefinitionData(id, .5f, 0f, 0f, 0f, 0f, 0f,
                    0f, .125f, .05f, new Color(water[i]), false, java.util.List.of("overworld"), null));
            Registries.BIOME_IDENTIFIERS.get().put(identifier, id);
        }
        Registries.BIOMES.set(new BiomeDefinitions(definitions));
    }

    void start() {
        Plugin owner = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (owner == null) throw new IllegalStateException("La integración requiere Geyser-Spigot.");
        task = Bukkit.getScheduler().runTaskTimer(owner, this::tick, 10L, 10L);
    }

    private boolean bloodMoon(World world) throws ReflectiveOperationException {
        Plugin current = Bukkit.getPluginManager().getPlugin("QuasoPlugin");
        if (current == null || !current.isEnabled()) return false;
        if (current != quaso) {
            quaso = current;
            getBloodMoon = current.getClass().getMethod("getBloodMoon");
            isActive = null;
        }
        Object manager = getBloodMoon.invoke(current);
        if (manager == null) return false;
        if (isActive == null) isActive = manager.getClass().getMethod("isActive", World.class);
        return (boolean) isActive.invoke(manager, world);
    }

    private void tick() {
        var connected = new HashSet<UUID>();
        for (var connection : extension.geyserApi().onlineConnections()) {
            UUID id = connection.javaUuid();
            if (id == null || !(connection instanceof GeyserSession session)) continue;
            var player = Bukkit.getPlayer(id);
            if (player == null) continue;
            connected.add(id);
            FogState state = fogs.computeIfAbsent(id, unused -> new FogState());
            World world = player.getWorld();
            if (!world.getUID().equals(state.world)) { state.world = world.getUID(); state.step = 0; }
            boolean active = false;
            try { active = bloodMoon(world); }
            catch (ReflectiveOperationException | RuntimeException error) {
                if (!warned) { warned = true; extension.logger().warning("BloodMoon Bedrock no disponible: " + error); }
            }
            state.step = Math.max(0, Math.min(20, state.step + (active ? 1 : -1)));
            String fog = state.step == 0 ? null : "quaso:bloodmoon_" + state.step;
            if (!java.util.Objects.equals(state.sent, fog)) {
                String old = state.sent;
                session.executeInEventLoop(() -> {
                    if (old != null) session.camera().removeFog(old);
                    if (fog != null) session.camera().sendFog(fog);
                });
                state.sent = fog;
            }
        }
        fogs.keySet().retainAll(connected);
    }

    void stop() {
        if (task != null) task.cancel();
        for (var connection : extension.geyserApi().onlineConnections()) {
            FogState state = fogs.get(connection.javaUuid());
            if (state != null && state.sent != null && connection instanceof GeyserSession session) {
                String fog = state.sent;
                session.executeInEventLoop(() -> session.camera().removeFog(fog));
            }
        }
        fogs.clear();
    }
}
