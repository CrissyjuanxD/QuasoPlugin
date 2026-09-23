package Pesca;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FishingZoneManager {

    private final QuasoPlugin plugin;
    private File configFile;
    private FileConfiguration fishingConfig;

    private final Map<String, FishingZone> zones = new HashMap<>();

    public FishingZoneManager(QuasoPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "pesca.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo crear pesca.yml", e);
            }
        }
        fishingConfig = YamlConfiguration.loadConfiguration(configFile);
        zones.clear();
        loadZonesFromConfig();
    }

    public void save() {
        try {
            fishingConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar pesca.yml", e);
        }
    }

    // Carga las zonas guardadas en pesca.yml
    private void loadZonesFromConfig() {
        if (!fishingConfig.contains("zonas")) return;

        for (String zoneName : fishingConfig.getConfigurationSection("zonas").getKeys(false)) {
            String path = "zonas." + zoneName + ".";

            String worldName = fishingConfig.getString(path + "world");
            World world = Bukkit.getWorld(worldName != null ? worldName : "");
            if (world == null) continue;

            double x1 = fishingConfig.getDouble(path + "pos1.x");
            double y1 = fishingConfig.getDouble(path + "pos1.y");
            double z1 = fishingConfig.getDouble(path + "pos1.z");

            double x2 = fishingConfig.getDouble(path + "pos2.x");
            double y2 = fishingConfig.getDouble(path + "pos2.y");
            double z2 = fishingConfig.getDouble(path + "pos2.z");

            Location pos1 = new Location(world, x1, y1, z1);
            Location pos2 = new Location(world, x2, y2, z2);

            zones.put(zoneName.toLowerCase(), new FishingZone(zoneName, pos1, pos2));
        }
        plugin.getLogger().info("[Pesca] " + zones.size() + " zona(s) cargada(s).");
    }

    // Guarda una zona nueva (las dos posiciones tienen que estar en el mismo mundo)
    public boolean registerZone(String name, Location pos1, Location pos2) {
        if (pos1 == null || pos2 == null) return false;
        if (!pos1.getWorld().equals(pos2.getWorld())) return false;

        FishingZone zone = new FishingZone(name, pos1, pos2);
        zones.put(name.toLowerCase(), zone);

        String path = "zonas." + name.toLowerCase() + ".";
        String worldName = pos1.getWorld().getName();
        fishingConfig.set(path + "world", worldName);
        fishingConfig.set(path + "pos1.x", pos1.getX());
        fishingConfig.set(path + "pos1.y", pos1.getY());
        fishingConfig.set(path + "pos1.z", pos1.getZ());
        fishingConfig.set(path + "pos2.x", pos2.getX());
        fishingConfig.set(path + "pos2.y", pos2.getY());
        fishingConfig.set(path + "pos2.z", pos2.getZ());

        save();
        return true;
    }

    public boolean removeZone(String name) {
        if (!zones.containsKey(name.toLowerCase())) return false;

        zones.remove(name.toLowerCase());
        fishingConfig.set("zonas." + name.toLowerCase(), null);
        save();

        return true;
    }

    public FishingZone getZoneAt(Location loc) {
        for (FishingZone zone : zones.values()) {
            if (zone.contains(loc)) return zone;
        }
        return null;
    }

    public boolean isInFishingZone(Location loc) {
        return getZoneAt(loc) != null;
    }

    public Collection<FishingZone> getZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public Map<String, FishingZone> getZonesMap() {
        return Collections.unmodifiableMap(zones);
    }
}