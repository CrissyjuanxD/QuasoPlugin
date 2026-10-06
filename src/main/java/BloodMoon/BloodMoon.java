package BloodMoon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** BloodMoon 0.8.1 de SpectralMemories integrada en el ciclo de vida de Quaso. */
public final class BloodMoon implements Listener {
    private static BloodMoon instance;
    private final JavaPlugin plugin;
    private final Map<UUID, BloodMoonActuator> worlds = new LinkedHashMap<>();
    private final Map<UUID, ConfigReader> configs = new LinkedHashMap<>();
    private final File cacheFile;
    private final YamlConfiguration cache;
    private final LocaleReader locales;
    private BukkitTask clock;
    private boolean shuttingDown;

    public BloodMoon(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        cacheFile = new File(plugin.getDataFolder(), "bloodmoon/estado.yml");
        cache = YamlConfiguration.loadConfiguration(cacheFile);
        locales = new LocaleReader(plugin);
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds()) LoadWorld(world);
        PluginCommand command = plugin.getCommand("bloodmoon");
        if (command != null) {
            BloodMoonCommands executor = new BloodMoonCommands(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        clock = GetScheduler().runTaskTimer(plugin, () -> {
            for (BloodMoonActuator actuator : List.copyOf(worlds.values())) actuator.checkNight();
        }, 1L, 40L);
    }

    public static BloodMoon GetInstance() { return instance; }
    public JavaPlugin getPlugin() { return plugin; }
    public BukkitScheduler GetScheduler() { return plugin.getServer().getScheduler(); }
    public ConfigReader getConfigReader(World world) { return configs.get(world.getUID()); }
    public LocaleReader getLocaleReader() { return locales; }
    public BloodMoonActuator getActuator(World world) { return world == null ? null : worlds.get(world.getUID()); }
    public boolean isActive(World world) {
        BloodMoonActuator actuator = getActuator(world);
        return actuator != null && actuator.isInProgress();
    }
    public List<World> getWorlds() { return worlds.values().stream().map(BloodMoonActuator::getWorld).toList(); }
    public boolean isShuttingDown() { return shuttingDown; }

    public void LoadWorld(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL || configs.containsKey(world.getUID())) return;
        ConfigReader config = new ConfigReader(plugin, world);
        configs.put(world.getUID(), config);
        if (config.GetIsBlacklistedConfig()) return;
        importLegacyCache(world);
        String path = world.getUID().toString();
        long day = world.getFullTime() / 24000;
        BloodMoonCycle cycle = new BloodMoonCycle(day, config.GetIntervalConfig(), cache.getLong(path + ".next-night", -1));
        BloodMoonActuator actuator = new BloodMoonActuator(this, world, cycle);
        worlds.put(world.getUID(), actuator);
        Bukkit.getPluginManager().registerEvents(actuator, plugin);
        if (config.GetPermanentBloodMoonConfig()
                || (cache.getBoolean(path + ".active") && cache.getLong(path + ".active-day", -1) == day && world.getTime() >= 12000)) {
            actuator.StartBloodMoon();
        }
    }

    private void importLegacyCache(World world) {
        String path = world.getUID().toString();
        if (cache.contains(path)) return;
        File legacy = new File(plugin.getDataFolder().getParentFile(), "BloodMoon/cache.db");
        if (!legacy.isFile()) return;
        // Paper proporciona SQLite. Si no hay controlador, sigue con el calendario inicial.
        try (var connection = DriverManager.getConnection("jdbc:sqlite:file:" + legacy.getAbsolutePath() + "?mode=ro");
             var query = connection.prepareStatement("SELECT days, checkAt FROM lastBloodMoon WHERE world = ?")) {
            query.setString(1, path);
            try (var result = query.executeQuery()) {
                if (!result.next()) return;
                int remaining = Math.max(0, result.getInt("days"));
                long checkAt = result.getLong("checkAt");
                long day = world.getFullTime() / 24000;
                cache.set(path + ".next-night", Math.max(day, checkAt / 24000) + Math.max(0, remaining - 1));
                cache.set(path + ".active", remaining == 0 && world.getTime() >= 12000);
                cache.set(path + ".active-day", day);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("No se pudo importar el calendario antiguo de BloodMoon para " + world.getName() + ": " + ex.getMessage());
        }
    }

    void remember(BloodMoonActuator actuator) {
        String path = actuator.getWorld().getUID().toString();
        cache.set(path + ".next-night", actuator.getCycle().nextNight());
        cache.set(path + ".active", actuator.isInProgress());
        cache.set(path + ".active-day", actuator.getActiveDay());
        try { cache.save(cacheFile); }
        catch (IOException ex) { plugin.getLogger().warning("No se pudo guardar el calendario de BloodMoon: " + ex.getMessage()); }
    }

    public void reload() {
        locales.RefreshLocales();
        for (ConfigReader reader : configs.values()) reader.RefreshConfigs();
        for (World world : new ArrayList<>(Bukkit.getWorlds())) {
            ConfigReader reader = getConfigReader(world);
            if (reader == null) { LoadWorld(world); continue; }
            BloodMoonActuator actuator = getActuator(world);
            if (reader.GetIsBlacklistedConfig()) {
                if (actuator != null) unload(world);
            } else if (actuator == null) {
                configs.remove(world.getUID());
                LoadWorld(world);
            } else {
                actuator.reload();
            }
        }
    }

    private void unload(World world) {
        BloodMoonActuator actuator = worlds.remove(world.getUID());
        if (actuator != null) {
            remember(actuator);
            actuator.shutdown();
            HandlerList.unregisterAll(actuator);
        }
        configs.remove(world.getUID());
    }
    @EventHandler public void onWorldLoad(WorldLoadEvent event) { LoadWorld(event.getWorld()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onWorldUnload(WorldUnloadEvent event) { unload(event.getWorld()); }

    public void shutdown() {
        shuttingDown = true;
        if (clock != null) clock.cancel();
        for (BloodMoonActuator actuator : worlds.values()) { remember(actuator); actuator.shutdown(); HandlerList.unregisterAll(actuator); }
        worlds.clear();
        configs.clear();
        HandlerList.unregisterAll(this);
        instance = null;
    }
}
