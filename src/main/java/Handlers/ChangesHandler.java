package Handlers;

import Dificultades.Change;
import Dificultades.OneChanges;
import Dificultades.ThreeChanges;
import Dificultades.TwoChanges;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChangesHandler {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Change> changes = new LinkedHashMap<>();

    public ChangesHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cambios.yml");

        register(new OneChanges(plugin));
        register(new TwoChanges(plugin));
        register(new ThreeChanges(plugin));

        boolean firstRun = !file.exists();
        for (String id : loadActive()) {
            Change change = changes.get(id);
            if (change != null) change.apply();
        }
        if (firstRun) save();
    }

    private void register(Change change) {
        changes.put(change.id(), change);
    }

    public Collection<Change> getChanges() {
        return changes.values();
    }

    // Acepta el nombre del cambio (uno, dos, tres) o su número (1, 2, 3)
    public Change find(String name) {
        Change byId = changes.get(name.toLowerCase(Locale.ROOT));
        if (byId != null) return byId;
        try {
            int index = Integer.parseInt(name) - 1;
            if (index >= 0 && index < changes.size()) return new ArrayList<>(changes.values()).get(index);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public boolean activate(Change change) {
        if (change.isApplied()) return false;
        change.apply();
        save();
        return true;
    }

    public boolean deactivate(Change change) {
        if (!change.isApplied()) return false;
        change.revert();
        save();
        return true;
    }

    // La primera vez arma la lista con el sistema de días viejo: con el día 1 o más, el cambio uno ya estaba activo
    private List<String> loadActive() {
        if (file.exists()) return YamlConfiguration.loadConfiguration(file).getStringList("activos");
        File old = new File(plugin.getDataFolder(), "DayandStorm.yml");
        int day = old.exists() ? YamlConfiguration.loadConfiguration(old).getInt("DiaActual", 1) : 1;
        return day >= 1 ? List.of("uno") : List.of();
    }

    private void save() {
        List<String> active = new ArrayList<>();
        for (Change change : changes.values()) {
            if (change.isApplied()) active.add(change.id());
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("activos", active);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar cambios.yml: " + e.getMessage());
        }
    }
}
