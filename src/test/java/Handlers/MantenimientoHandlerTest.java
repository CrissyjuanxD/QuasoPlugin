package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MantenimientoHandlerTest {
    private JavaPlugin plugin(YamlConfiguration config) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return plugin;
    }

    @Test
    void persistsMaintenanceKicksNonOperatorsAndRejectsTheirLoginsUntilDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        JavaPlugin plugin = plugin(config);
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission("viciont_hardcore3.mantenimiento")).thenReturn(true);
        Player operator = mock(Player.class);
        when(operator.isOp()).thenReturn(true);
        Player user = mock(Player.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(operator, user));
            MantenimientoHandler maintenance = new MantenimientoHandler(plugin);
            maintenance.onCommand(admin, null, "mantenimiento", new String[]{"on"});
            assertTrue(maintenance.isActivo());
            assertTrue(config.getBoolean("mantenimiento.activo"));
            verify(plugin).saveConfig();
            verify(user).kickPlayer(contains("El servidor está en mantenimiento"));
            verify(operator, never()).kickPlayer(anyString());
            MantenimientoHandler restarted = new MantenimientoHandler(plugin);
            PlayerLoginEvent login = mock(PlayerLoginEvent.class);
            when(login.getPlayer()).thenReturn(user);
            restarted.onLogin(login);
            verify(login).disallow(eq(PlayerLoginEvent.Result.KICK_OTHER), contains("revisa Discord"));
            PlayerLoginEvent opLogin = mock(PlayerLoginEvent.class);
            when(opLogin.getPlayer()).thenReturn(operator);
            restarted.onLogin(opLogin);
            verify(opLogin, never()).disallow(any(), anyString());
            restarted.onCommand(admin, null, "mantenimiento", new String[]{"off"});
            PlayerLoginEvent reopened = mock(PlayerLoginEvent.class);
            when(reopened.getPlayer()).thenReturn(user);
            restarted.onLogin(reopened);
            assertFalse(config.getBoolean("mantenimiento.activo"));
            verify(reopened, never()).disallow(any(), anyString());
        }
    }

    @Test
    void commandRequiresPermissionAndReloadUsesTheSavedSetting() {
        YamlConfiguration config = new YamlConfiguration();
        JavaPlugin plugin = plugin(config);
        MantenimientoHandler maintenance = new MantenimientoHandler(plugin);
        maintenance.onCommand(mock(CommandSender.class), null, "mantenimiento", new String[]{"on"});
        assertFalse(maintenance.isActivo());
        verify(plugin, never()).saveConfig();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());
            config.set("mantenimiento.activo", true);
            maintenance.reload();
            assertTrue(maintenance.isActivo());
            config.set("mantenimiento.activo", false);
            maintenance.reload();
            assertFalse(maintenance.isActivo());
        }
    }
}
