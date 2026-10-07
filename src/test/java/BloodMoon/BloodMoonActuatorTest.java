package BloodMoon;

import Handlers.ActionBarHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BloodMoonActuatorTest {
    private JavaPlugin plugin;
    private BloodMoon manager;
    private ConfigReader config;
    private BloodMoonActuator actuator;
    private Player player;
    private World world;
    private PluginManager events;
    private MockedStatic<Bukkit> bukkit;
    private final List<Runnable> callbacks = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        manager = mock(BloodMoon.class);
        config = mock(ConfigReader.class);
        plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("quasoplugin");
        when(manager.getPlugin()).thenReturn(plugin);
        world = mock(World.class);
        when(manager.getConfigReader(world)).thenReturn(config);
        when(manager.setDayClockPaused(world, true)).thenReturn(true);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getTime()).thenReturn(14000L);
        when(world.getFullTime()).thenReturn(14000L);
        when(world.getSpawnLimit(SpawnCategory.MONSTER)).thenReturn(70);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(63);
        WorldBorder border = mock(WorldBorder.class);
        when(world.getWorldBorder()).thenReturn(border);
        when(border.isInside(any(Location.class))).thenReturn(true);
        Block stone = mock(Block.class), air = mock(Block.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(air.getType()).thenReturn(Material.AIR);
        when(air.isPassable()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call -> (int) call.getArgument(1) <= 63 ? stone : air);
        Zombie prototype = mock(Zombie.class);
        when(prototype.getWidth()).thenReturn(0.6);
        when(prototype.getHeight()).thenReturn(1.95);
        when(world.createEntity(any(Location.class), eq(Zombie.class))).thenReturn(prototype);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(call -> new Location(world, 0.5, 64, 0.5));
        when(player.getName()).thenReturn("Crosszy");
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.spigot()).thenReturn(mock(Player.Spigot.class));
        when(world.getPlayers()).thenReturn(List.of(player));
        when(config.GetHordeEnabled()).thenReturn(true);
        when(config.GetHordeMobWhitelist()).thenReturn(new String[]{"ZOMBIE"});
        when(config.GetHordeMinPopulation()).thenReturn(3);
        when(config.GetHordeMaxPopulation()).thenReturn(3);
        when(config.GetHordeSpawnDistance()).thenReturn(12);
        when(config.GetHordeSpawnrateBaseline()).thenReturn(800);
        when(config.GetHordeSpawnrateVariation()).thenReturn(0);
        when(config.GetIntervalConfig()).thenReturn(5);
        when(config.GetSpawnRateConfig()).thenReturn(25);
        when(config.GetPreBloodMoonCommands()).thenReturn(new String[0]);
        when(config.GetPostBloodMoonCommands()).thenReturn(new String[0]);
        when(config.GetMobEffectConfig(anyString())).thenReturn(new String[0]);
        LocaleReader locales = mock(LocaleReader.class);
        when(manager.getLocaleReader()).thenReturn(locales);
        when(locales.GetLocaleString(anyString())).thenReturn("BloodMoon de prueba");
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(manager.GetScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong())).thenAnswer(call -> capture(call.getArgument(1)));
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> capture(call.getArgument(1)));
        events = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(events);
        // La cola de mensajes vive más que un ciclo de BloodMoon; sus tareas no pertenecen al actuador.
        BukkitScheduler messages = mock(BukkitScheduler.class);
        when(messages.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mock(BukkitTask.class));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(messages);
        bukkit.when(Bukkit::getPluginManager).thenReturn(events);
        bukkit.when(() -> Bukkit.createBossBar(anyString(), any(), any(), any(org.bukkit.boss.BarFlag[].class))).thenReturn(mock(BossBar.class));
        actuator = new BloodMoonActuator(manager, world, new BloodMoonCycle(0, 5, -1));
    }
    private BukkitTask capture(Runnable callback) {
        callbacks.add(callback);
        BukkitTask task = mock(BukkitTask.class);
        tasks.add(task);
        return task;
    }
    @AfterEach void cleanup() { ActionBarHandler.shutdown(plugin); bukkit.close(); }
    @Test void aCancelledHordeCreatesNoEntitiesOrLightning() {
        doAnswer(call -> { ((BloodMoonHordeEvent) call.getArgument(0)).setCancelled(true); return null; }).when(events).callEvent(any(Event.class));
        assertEquals(BloodMoonActuator.HordeResult.BLOCKED, actuator.SpawnHorde(player));
        verify(world, never()).spawnEntity(any(), any(), any(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class));
        verify(world, never()).strikeLightningEffect(any());
    }
    @Test void aHordeWithEqualMinimumAndMaximumSpawnsAtSafeY() {
        Zombie zombie = mock(Zombie.class);
        when(zombie.isValid()).thenReturn(true);
        when(zombie.getWidth()).thenReturn(0.6);
        when(zombie.getHeight()).thenReturn(1.95);
        when(world.spawnEntity(any(), eq(EntityType.ZOMBIE), any(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class))).thenReturn(zombie);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertEquals(BloodMoonActuator.HordeResult.SPAWNED, actuator.SpawnHorde(player));
        }
        verify(world, times(3)).spawnEntity(argThat(location -> location.getY() == 64), eq(EntityType.ZOMBIE), any(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class));
        verify(zombie, times(3)).setTarget(player);
    }
    @Test void blockedTerrainOmitsTheHordeInsteadOfForcingAnUnsafeSpawn() {
        when(world.hasCollisionsIn(any())).thenReturn(true);
        assertEquals(BloodMoonActuator.HordeResult.NO_SAFE_LOCATION, actuator.SpawnHorde(player));
        verify(world, never()).spawnEntity(any(), any(), any(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class));
    }
    @Test void stoppingInvalidatesAllCallbacksEvenAfterANewBloodMoonStarts() {
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertTrue(actuator.StartBloodMoon());
            int count = callbacks.size();
            assertFalse(actuator.StartBloodMoon());
            assertEquals(count, callbacks.size());
            List<Runnable> oldCallbacks = List.copyOf(callbacks);
            actuator.StopBloodMoon();
            for (BukkitTask task : tasks) verify(task).cancel();
            verify(world).setSpawnLimit(SpawnCategory.MONSTER, 70);
            assertTrue(actuator.StartBloodMoon());
            clearInvocations(world, events);
            for (Runnable callback : oldCallbacks) callback.run();
            verify(events, never()).callEvent(any());
            verify(world, never()).setStorm(anyBoolean());
            verify(world, never()).spawnEntity(any(), any(), any(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.class));
        }
    }
    @Test void endingBloodMoonClearsRainEvenWhenItWasAlreadyRainingBeforeItStarted() {
        when(config.GetThunderingConfig()).thenReturn(true);
        when(world.hasStorm()).thenReturn(true);
        when(world.isThundering()).thenReturn(true);
        when(world.getWeatherDuration()).thenReturn(5000);
        when(world.getThunderDuration()).thenReturn(6000);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            clearInvocations(world);
            actuator.StopBloodMoon();
            verify(world).setStorm(false);
            verify(world).setThundering(false);
            verify(world).setClearWeatherDuration(12000);
            verify(world, never()).setStorm(true);
            verify(world, never()).setThundering(true);
        }
    }
    @Test void redSkyKeepsTheStormAndAvoidsTheBossbarDarkeningFlags() {
        when(config.GetThunderingConfig()).thenReturn(true);
        when(config.GetDarkenSkyConfig()).thenReturn(true);
        when(manager.syncSky(world, true)).thenReturn(true);
        when(manager.isSkyActive(world)).thenReturn(true);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertTrue(actuator.StartBloodMoon());
            verify(manager).syncSky(world, true);
            verify(world).setStorm(true);
            verify(world).setThundering(true);
            verify(world, never()).setStorm(false);
            verify(world, never()).setThundering(false);
            bukkit.verify(() -> Bukkit.createBossBar(anyString(), any(), any(), eq(new org.bukkit.boss.BarFlag[0])));
        }
    }
    @Test void redSkyDoesNotTakeOverWeatherWhenStormsAreExplicitlyDisabled() {
        when(config.GetThunderingConfig()).thenReturn(false);
        when(manager.syncSky(world, true)).thenReturn(true);
        when(manager.isSkyActive(world)).thenReturn(true);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            clearInvocations(world);
            actuator.StopBloodMoon();
            verify(manager).syncSky(world, false);
            verify(world, never()).setStorm(anyBoolean());
            verify(world, never()).setThundering(anyBoolean());
            verify(world, never()).setClearWeatherDuration(anyInt());
        }
    }
    @Test void dawnClearsBloodMoonWeatherAndCancelledAmbientTasksCannotRestoreIt() {
        when(config.GetThunderingConfig()).thenReturn(true);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            List<Runnable> oldCallbacks = List.copyOf(callbacks);
            when(world.getTime()).thenReturn(23000L);
            when(world.getFullTime()).thenReturn(23000L);
            when(world.getGameTime()).thenReturn(9000L);
            clearInvocations(world);
            actuator.checkNight();
            for (Runnable callback : oldCallbacks) callback.run();
            assertFalse(actuator.isInProgress());
            verify(world).setStorm(false);
            verify(world).setThundering(false);
            verify(world).setClearWeatherDuration(12000);
            verify(world, never()).setStorm(true);
            verify(world, never()).setThundering(true);
        }
    }
    @Test void permanentModeStillRunsOnlyAtNightAndLastsUntilDawn() {
        when(config.GetPermanentBloodMoonConfig()).thenReturn(true);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            when(world.getTime()).thenReturn(12999L);
            actuator.checkNight();
            assertFalse(actuator.isInProgress());
            when(world.getTime()).thenReturn(13000L);
            actuator.checkNight();
            assertTrue(actuator.isInProgress());
            when(world.getTime()).thenReturn(23000L);
            when(world.getGameTime()).thenReturn(9999L);
            actuator.checkNight();
            assertTrue(actuator.isInProgress());
            when(world.getGameTime()).thenReturn(10000L);
            actuator.checkNight();
            assertFalse(actuator.isInProgress());
            actuator.checkNight();
            assertFalse(actuator.isInProgress());
            assertFalse(actuator.StartBloodMoon());
        }
    }
    @Test void reloadingDuringTheNightDoesNotRepeatRewardsOrWarnings() {
        when(config.GetPreBloodMoonCommands()).thenReturn(new String[]{"say inicio;s"});
        when(config.GetPostBloodMoonCommands()).thenReturn(new String[]{"say fin;s"});
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertTrue(actuator.StartBloodMoon());
            actuator.reload();
            assertTrue(actuator.isInProgress());
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), eq("say inicio")), times(1));
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), eq("say fin")), never());
            actuator.StopBloodMoon();
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), eq("say fin")), times(1));
        }
    }
    @Test void daylightFromTimeSetOrSleepingEndsTheEvent() {
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertTrue(actuator.StartBloodMoon());
            when(world.getTime()).thenReturn(1000L);
            actuator.checkNight();
            assertFalse(actuator.isInProgress());
            verify(world, never()).setTime(anyLong());
        }
    }
    @Test void fixedNightDoesNotEndTheEventOrResetItsDurationWhenReloaded() {
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            assertTrue(actuator.StartBloodMoon());
            verify(world).setFullTime(19000L);
            verify(manager).setDayClockPaused(world, true);
            when(world.getTime()).thenReturn(23000L);
            when(world.getGameTime()).thenReturn(2000L);
            actuator.checkNight();
            assertTrue(actuator.isInProgress());
            assertEquals(7000, actuator.getRemainingTicks());
            actuator.reload();
            assertTrue(actuator.isInProgress());
            assertEquals(7000, actuator.getRemainingTicks());
            clearInvocations(manager);
            when(world.getGameTime()).thenReturn(9000L);
            actuator.checkNight();
            assertFalse(actuator.isInProgress());
            verify(manager).setDayClockPaused(world, false);
        }
    }
    @Test void shuttingDownReleasesTheDayClockAndCancelsTheEvent() {
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            actuator.shutdown();
            verify(manager).setDayClockPaused(world, false);
            assertFalse(actuator.isInProgress());
            assertFalse(actuator.StartBloodMoon());
        }
    }
    @Test void anUnavailableOrSharedDayClockIsNotMovedOrResumed() {
        when(manager.setDayClockPaused(world, true)).thenReturn(false);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            actuator.StopBloodMoon();
            verify(world, never()).setFullTime(anyLong());
            verify(manager, never()).setDayClockPaused(world, false);
        }
    }
    @Test void bloodMoonWithoutWeatherControlPreservesNaturalRain() {
        when(world.hasStorm()).thenReturn(true);
        when(world.isThundering()).thenReturn(true);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            clearInvocations(world);
            actuator.StopBloodMoon();
            verify(world, never()).setStorm(anyBoolean());
            verify(world, never()).setThundering(anyBoolean());
            verify(world, never()).setClearWeatherDuration(anyInt());
        }
    }
    @Test void regularBloodMoonMobsKeepTheirDamageAndResistanceMultipliers() {
        when(config.GetMobDamageMultConfig()).thenReturn(2);
        when(config.GetMobHealthMultConfig()).thenReturn(3);
        Zombie zombie = mock(Zombie.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(zombie.getWorld()).thenReturn(world);
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            var attack = mock(EntityDamageByEntityEvent.class);
            when(attack.getDamager()).thenReturn(zombie);
            when(attack.getEntity()).thenReturn(player);
            when(attack.getDamage()).thenReturn(6.0);
            when(attack.getFinalDamage()).thenReturn(6.0);
            actuator.onDamage(attack);
            verify(attack).setDamage(12);
            var defend = mock(EntityDamageByEntityEvent.class);
            when(defend.getDamager()).thenReturn(player);
            when(defend.getEntity()).thenReturn(zombie);
            when(defend.getDamage()).thenReturn(9.0);
            actuator.onDamage(defend);
            verify(defend).setDamage(3);
        }
    }
    @Test void playerDeathDoesNotClearInventoryOrResetExperience() {
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            actuator.StartBloodMoon();
            PlayerDeathEvent death = mock(PlayerDeathEvent.class);
            when(death.getEntity()).thenReturn(player);
            actuator.onDeath(death);
            verify(death, never()).getDrops();
            verify(death, never()).setDroppedExp(anyInt());
            verify(death, never()).setNewTotalExp(anyInt());
            verify(death, never()).setKeepInventory(anyBoolean());
        }
    }
}
