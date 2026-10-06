package BloodMoon;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

class BloodMoonSkyTest {
    private BloodMoonSky sky;
    private Server server;
    private World world;
    private Command time;
    private CommandSender sender;
    private ConsoleCommandSender console;
    private JavaPlugin plugin;
    private final List<Runnable> callbacks = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Long> delays = new ArrayList<>();

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(call -> {
            callbacks.add(call.getArgument(1));
            delays.add(call.getArgument(2));
            BukkitTask task = mock(BukkitTask.class);
            tasks.add(task);
            return task;
        });
        CommandMap commands = mock(CommandMap.class);
        when(server.getCommandMap()).thenReturn(commands);
        time = mock(Command.class);
        when(commands.getCommand("minecraft:time")).thenReturn(time);
        console = mock(ConsoleCommandSender.class);
        when(server.getConsoleSender()).thenReturn(console);
        when(time.tabComplete(eq(console), eq("minecraft:time"), any(String[].class), isNull()))
                .thenReturn(List.of("minecraft:overworld", "quaso:bloodmoon"));
        sender = mock(CommandSender.class);
        when(server.createCommandSender(any())).thenReturn(sender);
        when(server.dispatchCommand(eq(sender), anyString())).thenReturn(true);
        world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getKey()).thenReturn(NamespacedKey.minecraft("world"));
        when(world.getUID()).thenReturn(UUID.randomUUID());
        sky = new BloodMoonSky(plugin);
    }

    @Test void aMissingDatapackDoesNotExecuteInvalidCommandsOnStartOrStop() {
        when(time.tabComplete(eq(console), anyString(), any(String[].class), isNull())).thenReturn(List.of("minecraft:overworld"));
        assertFalse(sky.syncWorld(world, true));
        assertFalse(sky.syncWorld(world, false));
        assertFalse(sky.isActive(world));
        verify(time).tabComplete(eq(console), eq("minecraft:time"), aryEq(new String[]{"of", ""}), isNull());
        verify(server, never()).dispatchCommand(any(), anyString());
    }

    @Test void detectingNativeClocksUsesTheLocationOverloadInsteadOfTheGenericPlayerSuggestions() {
        when(time.tabComplete(eq(console), anyString(), any(String[].class)))
                .thenReturn(List.of("JugadorConectado"));
        assertTrue(sky.syncWorld(world, false));
        verify(time).tabComplete(eq(console), eq("minecraft:time"), aryEq(new String[]{"of", ""}), isNull());
        verify(time, never()).tabComplete(any(), anyString(), any(String[].class));
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon pause");
    }

    @Test void fadesRunOnTheDedicatedClockAndStopAtTheirEndpointsWithoutChangingTheDay() {
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.isActive(world));
        assertEquals(600L, delays.getFirst());
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon resume");
        when(world.getGameTime()).thenReturn(600L);
        callbacks.getFirst().run();
        assertTrue(sky.syncWorld(world, false));
        assertFalse(sky.isActive(world));
        assertEquals(600L, delays.getLast());
        when(world.getGameTime()).thenReturn(1200L);
        callbacks.getLast().run();
        verify(server, times(2)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 600");
        verify(server, times(2)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        verify(world, never()).setTime(anyLong());
        verify(world, never()).setFullTime(anyLong());
    }

    @Test void interruptingAFadeAndReloadingContinueFromTheCurrentIntensityAndCancelOldTasks() {
        sky.syncWorld(world, true);
        Runnable obsolete = callbacks.getFirst();
        when(world.getGameTime()).thenReturn(200L);
        sky.refreshAvailability();
        sky.syncWorld(world, false);
        verify(tasks.getFirst()).cancel();
        assertEquals(200L, delays.getLast());
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 1000");
        when(world.getGameTime()).thenReturn(250L);
        sky.syncWorld(world, true);
        verify(tasks.get(1)).cancel();
        assertEquals(450L, delays.getLast());
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 150");
        clearInvocations(server);
        obsolete.run();
        verify(server, never()).dispatchCommand(any(), anyString());
    }

    @Test void shutdownResetsAnUnfinishedFadeAndItsStaleTaskCannotRestoreTheColor() {
        sky.syncWorld(world, true);
        Runnable obsolete = callbacks.getFirst();
        sky.shutdown();
        verify(tasks.getFirst()).cancel();
        assertFalse(sky.isActive(world));
        clearInvocations(server);
        obsolete.run();
        verify(server, never()).dispatchCommand(any(), anyString());
    }

    @Test void repeatedStateUpdatesDoNotSpamCommands() {
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.syncWorld(world, true));
        verify(server, times(3)).dispatchCommand(eq(sender), anyString());
    }

    @Test void excludedEnvironmentsKeepTheirOwnVisualsAndClocks() {
        when(world.getEnvironment()).thenReturn(World.Environment.THE_END);
        assertFalse(sky.syncWorld(world, true));
        verify(server, never()).dispatchCommand(any(), anyString());
        verifyNoInteractions(time);
    }

    @Test void loadingAnInactiveWorldResetsPersistedClockStateAndUnloadingRestoresIt() {
        assertTrue(sky.syncWorld(world, false));
        assertTrue(sky.syncWorld(world, true));
        sky.clearWorld(world);
        assertFalse(sky.isActive(world));
        verify(server, times(3)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        sky.refreshAvailability();
        assertTrue(sky.syncWorld(world, false));
        verify(server, times(4)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
    }

    @Test void failedExecutionDoesNotClaimThatRedSkyIsActive() {
        when(server.dispatchCommand(eq(sender), anyString())).thenReturn(false);
        assertFalse(sky.syncWorld(world, true));
        assertFalse(sky.isActive(world));
    }
    @Test void sharedClocksDisableTheEffectWithoutAffectingOtherWorldsAndWarnOnlyOnce() {
        Server.Spigot settings = mock(Server.Spigot.class);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("time.affects-all-worlds", true);
        when(server.spigot()).thenReturn(settings);
        when(settings.getPaperConfig()).thenReturn(configuration);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        assertFalse(sky.syncWorld(world, true));
        assertTrue(sky.syncWorld(world, false));
        assertFalse(sky.isActive(world));
        sky.refreshAvailability();
        assertFalse(sky.syncWorld(world, true));
        verify(logger).warning(contains("time.affects-all-worlds: false"));
        verify(server, times(3)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon pause");
        verify(server, times(3)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        verify(server, never()).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon resume");
    }
}
