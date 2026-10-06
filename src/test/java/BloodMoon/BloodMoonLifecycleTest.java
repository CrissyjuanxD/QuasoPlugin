package BloodMoon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BloodMoonLifecycleTest {
    @TempDir Path directory;
    private JavaPlugin plugin;
    private World world;
    private MockedStatic<Bukkit> bukkit;
    private BloodMoon manager;
    private Path folder;
    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() throws Exception {
        plugin = mock(JavaPlugin.class);
        folder = directory.resolve("plugins/QuasoPlugin");
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.namespace()).thenReturn("quasoplugin");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BloodMoonTest"));
        when(plugin.getResource(anyString())).thenAnswer(call -> getClass().getClassLoader().getResourceAsStream(call.getArgument(0)));
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> mock(BukkitTask.class));
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong())).thenAnswer(call -> mock(BukkitTask.class));
        world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getTime()).thenReturn(14000L);
        when(world.getFullTime()).thenReturn(14000L);
        when(world.getSpawnLimit(SpawnCategory.MONSTER)).thenReturn(70);
        when(world.getPlayers()).thenReturn(List.of());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
        bukkit.when(() -> Bukkit.createBossBar(anyString(), any(), any(), any(org.bukkit.boss.BarFlag[].class))).thenReturn(mock(BossBar.class));
    }
    @AfterEach void cleanup() { if (manager != null) manager.shutdown(); bukkit.close(); }
    @Test void originalWorldSettingsAreImportedAndRemovedFeaturesAreDiscarded() throws Exception {
        Path old = directory.resolve("plugins/BloodMoon/world/config.yml");
        Files.createDirectories(old.getParent());
        Files.writeString(old, "BloodMoonInterval: 8\nHordeMinPopulation: 7\nHordeMaxPopulation: 7\nMobDamageMultiplicator: 4\nItemDespawnUponDeath: true\nExperienceDespawnsUponDeath: true\nEnableZombieBoss: true\nZombieBossHealth: 80\nZOMBIEBOSSEffects: ['WITHER,9,2']\n");
        ConfigReader config = new ConfigReader(plugin, world);
        assertEquals(8, config.GetIntervalConfig());
        assertEquals(7, config.GetHordeMinPopulation());
        assertEquals(4, config.GetMobDamageMultConfig());
        assertEquals(3, config.GetMobHealthMultConfig());
        var persisted = YamlConfiguration.loadConfiguration(folder.resolve("bloodmoon/world/config.yml").toFile());
        assertFalse(persisted.contains("ItemDespawnUponDeath"));
        assertFalse(persisted.contains("ExperienceDespawnsUponDeath"));
        assertFalse(persisted.contains("EnableZombieBoss"));
        assertFalse(persisted.contains("ZombieBossHealth"));
        assertFalse(persisted.contains("ZOMBIEBOSSEffects"));
        assertTrue(Files.exists(old));
    }
    @Test void restartingDuringTheSameNightRestoresAnActiveBloodMoonAndItsCalendar() {
        manager = new BloodMoon(plugin);
        manager.enable();
        assertFalse(manager.isActive(world));
        manager.getActuator(world).StartBloodMoon();
        assertTrue(manager.isActive(world));
        long nextNight = manager.getActuator(world).getCycle().nextNight();
        manager.shutdown();
        manager = new BloodMoon(plugin);
        manager.enable();
        assertTrue(manager.isActive(world));
        assertEquals(nextNight, manager.getActuator(world).getCycle().nextNight());
    }
    @Test void restartAtTheFrozenDawnRestoresOnlyTheRemainingDuration() {
        manager = new BloodMoon(plugin);
        manager.enable();
        manager.getActuator(world).StartBloodMoon();
        when(world.getGameTime()).thenReturn(2000L);
        manager.shutdown();
        when(world.getTime()).thenReturn(23000L);
        when(world.getFullTime()).thenReturn(23000L);
        manager = new BloodMoon(plugin);
        manager.enable();
        assertTrue(manager.isActive(world));
        assertEquals(7000, manager.getActuator(world).getRemainingTicks());
        assertEquals(5, manager.getActuator(world).getCycle().nextNight());
        when(world.getGameTime()).thenReturn(9000L);
        manager.getActuator(world).checkNight();
        assertFalse(manager.isActive(world));
    }
    @Test void aSavedEventDoesNotRestartOnADifferentDay() {
        manager = new BloodMoon(plugin);
        manager.enable();
        manager.getActuator(world).StartBloodMoon();
        manager.shutdown();
        when(world.getTime()).thenReturn(1000L);
        when(world.getFullTime()).thenReturn(25000L);
        manager = new BloodMoon(plugin);
        manager.enable();
        assertFalse(manager.isActive(world));
    }
    @Test void disablingWeatherDuringReloadStillClearsTheWeatherOwnedByBloodMoon() throws Exception {
        when(world.hasStorm()).thenReturn(true);
        when(world.isThundering()).thenReturn(true);
        manager = new BloodMoon(plugin);
        manager.enable();
        manager.getActuator(world).StartBloodMoon();
        Path file = folder.resolve("bloodmoon/world/config.yml");
        var settings = YamlConfiguration.loadConfiguration(file.toFile());
        settings.set("ThunderDuringBloodMoon", false);
        settings.save(file.toFile());
        clearInvocations(world);
        manager.reload();
        verify(world).setStorm(false);
        verify(world).setThundering(false);
        verify(world).setClearWeatherDuration(12000);
        assertTrue(manager.isActive(world));
    }
    @Test void aResumedBloodMoonDoesNotKeepItsPreviouslySavedRainWhenItEnds() {
        when(world.hasStorm()).thenReturn(true);
        when(world.isThundering()).thenReturn(true);
        manager = new BloodMoon(plugin);
        manager.enable();
        manager.getActuator(world).StartBloodMoon();
        manager.shutdown();
        manager = new BloodMoon(plugin);
        manager.enable();
        assertTrue(manager.isActive(world));
        clearInvocations(world);
        manager.getActuator(world).StopBloodMoon();
        verify(world).setStorm(false);
        verify(world).setThundering(false);
        verify(world).setClearWeatherDuration(12000);
        verify(world, never()).setStorm(true);
        verify(world, never()).setThundering(true);
        assertFalse(manager.isActive(world));
    }
}
