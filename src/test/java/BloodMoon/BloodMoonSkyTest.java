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

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
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

    @Test void startingAndStoppingOnlyChangeTheDedicatedClockOfTheTargetWorld() {
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.isActive(world));
        assertTrue(sky.syncWorld(world, false));
        assertFalse(sky.isActive(world));
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 1");
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        verify(server, times(2)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon pause");
        verify(world, never()).setTime(anyLong());
        verify(world, never()).setFullTime(anyLong());
    }

    @Test void repeatedStateUpdatesDoNotSpamCommands() {
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.syncWorld(world, true));
        assertTrue(sky.syncWorld(world, true));
        verify(server, times(2)).dispatchCommand(eq(sender), anyString());
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
        verify(server, times(2)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        sky.refreshAvailability();
        assertTrue(sky.syncWorld(world, false));
        verify(server, times(3)).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
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
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon pause");
        verify(server).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 0");
        verify(server, never()).dispatchCommand(sender, "minecraft:execute in minecraft:world run minecraft:time of quaso:bloodmoon set 1");
    }
}
