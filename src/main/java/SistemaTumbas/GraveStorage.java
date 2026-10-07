package SistemaTumbas;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lee primero los datos puros: Bukkit no debe resolver mundos antes de que existan. */
final class GraveStorage {
    private final File file;
    private final Yaml yaml = new Yaml(new SafeConstructor(loaderOptions()));
    private final Map<String, Object> root;
    private final Map<String, Object> records;
    private boolean legacyBackupNeeded;

    @SuppressWarnings("unchecked")
    GraveStorage(File file) throws IOException {
        this.file = file;
        Object loaded = file.isFile() ? yaml.load(Files.readString(file.toPath(), StandardCharsets.UTF_8)) : null;
        if (loaded != null && !(loaded instanceof Map<?, ?>)) throw new IllegalArgumentException("El archivo de tumbas no es un mapa YAML.");
        root = loaded == null ? new LinkedHashMap<>() : (Map<String, Object>) loaded;
        Object graves = root.get("graves");
        if (graves != null && !(graves instanceof Map<?, ?>)) throw new IllegalArgumentException("La sección graves no es un mapa YAML.");
        records = graves == null ? new LinkedHashMap<>() : (Map<String, Object>) graves;
        root.put("graves", records);
        legacyBackupNeeded = records.values().stream().anyMatch(value -> value instanceof Map<?, ?> record
                && record.get("location") instanceof Map<?, ?> location && location.containsKey("=="));
    }

    // SnakeYAML corta en 3 MB por defecto; las tumbas que esperan un mundo no vencen y el archivo puede crecer, así
    // que se quita el tope como hace Bukkit con YamlConfiguration
    private static LoaderOptions loaderOptions() {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(Integer.MAX_VALUE);
        return options;
    }

    Map<String, Object> records() { return records; }

    @SuppressWarnings("unchecked")
    YamlConfiguration read(String id) throws InvalidConfigurationException {
        Object value = records.get(id);
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("El registro no es un mapa YAML.");
        Map<String, Object> record = new LinkedHashMap<>((Map<String, Object>) value);
        if (record.get("location") instanceof Map<?, ?> location) {
            Map<String, Object> coordinates = new LinkedHashMap<>((Map<String, Object>) location);
            coordinates.remove("==");
            record.put("location", coordinates);
        }
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml.dump(record));
        return config;
    }

    void put(Grave grave) {
        var location = grave.getLocation();
        Map<String, Object> coordinates = new LinkedHashMap<>();
        coordinates.put("world", location.getWorld().getName());
        coordinates.put("world-uuid", location.getWorld().getUID().toString());
        coordinates.put("x", location.getX());
        coordinates.put("y", location.getY());
        coordinates.put("z", location.getZ());
        coordinates.put("yaw", location.getYaw());
        coordinates.put("pitch", location.getPitch());
        YamlConfiguration config = new YamlConfiguration();
        config.set("owner", grave.getOwner().toString());
        config.set("ownerName", grave.getOwnerName());
        config.set("location", coordinates);
        config.set("creationTime", grave.getCreationTime());
        config.set("expiryTime", grave.getExpiryTime());
        config.set("items", grave.getItems());
        records.put(grave.getId().toString(), yaml.load(config.saveToString()));
    }

    void save() throws IOException {
        Files.createDirectories(file.toPath().toAbsolutePath().getParent());
        if (legacyBackupNeeded && file.isFile()) {
            var backup = file.toPath().resolveSibling(file.getName() + ".pre-26.2.bak");
            if (!Files.exists(backup)) Files.copy(file.toPath(), backup);
            legacyBackupNeeded = false;
        }
        var temporary = Files.createTempFile(file.toPath().toAbsolutePath().getParent(), "tumbas-", ".tmp");
        try {
            Files.writeString(temporary, yaml.dump(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
