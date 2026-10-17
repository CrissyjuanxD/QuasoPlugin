package studios.viciont.quasogeyser;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.session.GeyserSession;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Propiedad sincronizada de Bedrock: independiente del nametag y de las variantes vanilla. */
final class MobModels {
    record Mob(String model, String entity, String[] names, int mark_variant) {}
    record Tracked(String type, long runtimeId) {}
    private final Mob[] catalog;
    private final Map<String, GeyserIntEntityProperty> properties = new HashMap<>();
    private final Map<GeyserSession, Map<UUID, Tracked>> tracked = new ConcurrentHashMap<>();

    MobModels() {
        try (var input = new InputStreamReader(getClass().getResourceAsStream("/mobs.json"), StandardCharsets.UTF_8)) {
            catalog = new Gson().fromJson(input, Mob[].class);
        } catch (Exception error) { throw new IllegalStateException("No se pudo leer el catálogo de mobs", error); }
    }

    void define(GeyserDefineEntityPropertiesEvent event) {
        for (Mob mob : catalog) properties.computeIfAbsent(mob.entity(), type ->
                event.registerIntegerProperty(Identifier.of(type), Identifier.of("quaso:model"), 0, 9999, 0));
    }

    void spawn(ServerSpawnEntityEvent event) {
        if (!(event.connection() instanceof GeyserSession session) || event.definition() == null) return;
        String type = event.definition().identifier().toString();
        if (!properties.containsKey(type)) return;
        event.preSpawnConsumer(entity -> tracked.computeIfAbsent(session, unused -> new ConcurrentHashMap<>())
                .put(event.uuid(), new Tracked(type, entity.geyserId())));
    }

    int model(String type, String name) {
        String plain = VisualText.plain(name);
        for (Mob mob : catalog) {
            if (!mob.entity().equals(type)) continue;
            for (String expected : mob.names())
                if (plain.equalsIgnoreCase(expected) || plain.regionMatches(true, 0, expected + " ", 0, expected.length()+1))
                    return mob.mark_variant();
        }
        return 0;
    }

    /** Sólo el hilo principal de Paper consulta sus entidades. Geyser se actualiza en su propio hilo. */
    void tick(Set<? extends GeyserConnection> online) {
        tracked.keySet().retainAll(online);
        tracked.forEach((session, entities) -> entities.forEach((uuid, record) -> {
            var javaEntity = Bukkit.getEntity(uuid);
            if (javaEntity == null || !javaEntity.isValid()) { entities.remove(uuid, record); return; }
            int model = model(record.type(), javaEntity.getCustomName());
            var property = properties.get(record.type());
            session.executeInEventLoop(() -> {
                var bedrockEntity = session.entities().byUuid(uuid);
                if (bedrockEntity == null || bedrockEntity.geyserId() != record.runtimeId()) {
                    entities.remove(uuid, record);
                    return;
                }
                bedrockEntity.updateProperty(property, model);
            });
        }));
    }

    void clear() { tracked.clear(); }
}
