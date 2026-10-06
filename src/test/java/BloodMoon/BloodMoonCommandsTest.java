package BloodMoon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BloodMoonCommandsTest {
    private BloodMoon manager;
    private BloodMoonCommands commands;
    private Player player;
    private World world;
    private Command command;
    @BeforeEach void setup() {
        manager = mock(BloodMoon.class);
        commands = new BloodMoonCommands(manager);
        player = mock(Player.class);
        world = mock(World.class);
        command = mock(Command.class);
        when(player.getWorld()).thenReturn(world);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(world.getName()).thenReturn("world");
    }
    @Test void firstArgumentCompletesAllOriginalSubcommandsAndFiltersPermissions() {
        assertEquals(5, commands.onTabComplete(player, command, "bloodmoon", new String[]{""}).size());
        when(player.hasPermission("bloodmoon.start")).thenReturn(false);
        assertEquals(List.of("show", "spawnhorde", "stop"), commands.onTabComplete(player, command, "bloodmoon", new String[]{"s"}));
    }
    @Test void playerNamesMatchTheOriginalPlayerSyntax() {
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Crosszy");
        when(world.getPlayers()).thenReturn(List.of(target));
        assertEquals(List.of("Crosszy"), commands.onTabComplete(player, command, "bloodmoon", new String[]{"spawnhorde", "cro"}));
        assertEquals(List.of(), commands.onTabComplete(player, command, "bloodmoon", new String[]{"reload", ""}));
    }
    @Test void consoleCompletesWorldThenPlayer() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        when(manager.getWorlds()).thenReturn(List.of(world));
        assertEquals(List.of("world"), commands.onTabComplete(console, command, "bloodmoon", new String[]{"start", "w"}));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            Player target = mock(Player.class);
            when(target.getName()).thenReturn("Crosszy");
            when(world.getPlayers()).thenReturn(List.of(target));
            assertEquals(List.of("Crosszy"), commands.onTabComplete(console, command, "bloodmoon", new String[]{"spawnhorde", "world", "C"}));
        }
    }
    @Test void consoleReloadDoesNotRequireADummyWorldOrCrash() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        // Usa el lector estático real con un coordinador de pruebas para la respuesta.
        try (var singleton = mockStatic(BloodMoon.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(manager);
            LocaleReader locales = mock(LocaleReader.class);
            when(manager.getLocaleReader()).thenReturn(locales);
            when(locales.GetLocaleString("PluginReloaded")).thenReturn("Recargado");
            assertTrue(commands.onCommand(console, command, "bloodmoon", new String[]{"reload"}));
            verify(manager).reload();
            verify(console).sendMessage("Recargado");
            verify(manager, never()).getActuator(any());
        }
    }
}
