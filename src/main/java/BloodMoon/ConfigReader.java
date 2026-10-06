package BloodMoon;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Mantiene las claves del JAR 0.8.1 y copia su configuración por mundo si existe. */
public final class ConfigReader {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public ConfigReader(JavaPlugin plugin, World world) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bloodmoon/" + world.getName() + "/config.yml");
        if (!file.exists()) {
            File legacy = new File(plugin.getDataFolder().getParentFile(), "BloodMoon/" + world.getName() + "/config.yml");
            try {
                Files.createDirectories(file.toPath().getParent());
                if (legacy.isFile()) Files.copy(legacy.toPath(), file.toPath());
            } catch (IOException ex) {
                plugin.getLogger().warning("No se pudo importar la configuración de BloodMoon: " + ex.getMessage());
            }
        }
        RefreshConfigs();
    }

    public void RefreshConfigs() {
        config = YamlConfiguration.loadConfiguration(file);
        try (var stream = plugin.getResource("bloodmoon-defaults/config.yml")) {
            if (stream == null) throw new IOException("Falta bloodmoon-defaults/config.yml");
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            config.options().copyDefaults(true);
            // Estos ajustes del plugin externo no tienen efecto dentro de Quaso.
            config.set("ItemDespawnUponDeath", null);
            config.set("ExperienceDespawnsUponDeath", null);
            for (String key : java.util.Set.copyOf(config.getKeys(false))) {
                if (key.startsWith("ZombieBoss") || key.equals("EnableZombieBoss") || key.equals("ZOMBIEBOSSEffects")) config.set(key, null);
            }
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar BloodMoon: " + ex.getMessage());
        }
    }

    public String[] GetMobEffectConfig(String mob) {
        return config.getStringList(mob + "Effects").stream()
                .map(String::trim)
                .filter(effect -> !effect.toUpperCase(java.util.Locale.ROOT).startsWith("CONFUSION")
                        && !effect.toUpperCase(java.util.Locale.ROOT).startsWith("NAUSEA"))
                .toArray(String[]::new);
    }
    public String[] GetItemListConfig() { return config.getStringList("DropItemList").toArray(String[]::new); }
    public String[] GetHordeMobWhitelist() { return config.getStringList("HordeMobWhitelist").toArray(String[]::new); }
    public boolean GetIsBlacklistedConfig() { return config.getBoolean("IsBlacklisted", false); }
    public boolean GetHordeEnabled() { return config.getBoolean("HordesEnabled", true); }
    public boolean GetPermanentBloodMoonConfig() { return config.getBoolean("PermanentBloodMoon", false); }
    public boolean GetShieldPreventEffects() { return config.getBoolean("ShieldPreventsEffects", true); }
    public boolean GetMobsFromSpawnerNoRewardConfig() { return config.getBoolean("MobsFromSpawnerNoReward", false); }
    public boolean GetPreventSleepingConfig() { return config.getBoolean("PreventSleeping", true); }
    public int GetIntervalConfig() { return Math.max(1, config.getInt("BloodMoonInterval", 5)); }
    public int GetExpMultConfig() { return Math.max(0, config.getInt("ExperienceDropMult", 4)); }
    public int GetSpawnRateConfig() { return Math.max(0, config.getInt("BloodMoonSpawnMobRate", 25)); }
    public boolean GetBloodMoonEndSoundConfig() { return config.getBoolean("PlaySoundUponBloodMoonEnd", true); }
    public boolean GetDarkenSkyConfig() { return config.getBoolean("DarkenSky", true); }
    public boolean GetBloodMoonSkyEnabled() { return config.getBoolean("BloodMoonSkyEnabled", true); }
    public boolean GetMobHitParticleConfig() { return config.getBoolean("MobHitParticleEffect", true); }
    public boolean GetPlayerHitParticleConfig() { return config.getBoolean("PlayerHitParticleEffect", true); }
    public boolean GetPlayerDamageSoundConfig() { return config.getBoolean("PlaySoundUponHit", true); }
    public boolean GetThunderingConfig() { return config.getBoolean("ThunderDuringBloodMoon", true); }
    public String[] GetPreBloodMoonCommands() { return config.getStringList("CommandsOnStart").toArray(String[]::new); }
    public String[] GetPostBloodMoonCommands() { return config.getStringList("CommandsOnEnd").toArray(String[]::new); }
    public boolean GetMobDeathThunderConfig() { return config.getBoolean("LightningEffectOnMobDeath", true); }
    public boolean GetBloodMoonPeriodicSoundConfig() { return config.getBoolean("PlayPeriodicSoundsDuringBloodMoon", true); }
    public boolean GetLightningEffectConfig() { return config.getBoolean("LightningEffectOnPlayerDeath", true); }
    public int GetMinItemsDropConfig() { return Math.max(0, config.getInt("ItemDropsMinimum", 0)); }
    public int GetHordeMinPopulation() { return Math.max(0, config.getInt("HordeMinPopulation", 3)); }
    public int GetHordeSpawnrateBaseline() { return Math.clamp(config.getInt("BaselineHordeSpawnrate", 800), 1, 1200000); }
    public int GetHordeSpawnrateVariation() { return Math.clamp(config.getInt("HordeSpawnrateVariation", 200), 0, 1200000); }
    public int GetHordeSpawnDistance() { return Math.max(0, config.getInt("HordeSpawnDistance", 12)); }
    public int GetHordeMaxPopulation() { return Math.max(0, config.getInt("HordeMaxPopulation", 10)); }
    public int GetMaxItemsDropConfig() { return Math.max(0, config.getInt("ItemDropsMaximum", 4)); }
    public int GetMobDamageMultConfig() { return Math.max(0, config.getInt("MobDamageMultiplicator", 2)); }
    public int GetMobHealthMultConfig() { return Math.max(1, config.getInt("MobHealthMultiplicator", 3)); }
}
