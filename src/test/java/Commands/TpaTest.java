package Commands;

import Handlers.Dimensiones;
import imp.crissyjuanxd.QuasoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TpaTest {
    @TempDir Path directory;
    private Tpa tpa;
    private Dimensiones dimensiones;
    private Player alex;
    private Player crosszy;
    private World mundo;
    private AtomicReference<Location> posicionAlex;
    private final List<Runnable> callbacks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setup() {
        QuasoPlugin plugin = mock(QuasoPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(plugin.getServer()).thenReturn(server);
        mundo = mock(World.class);
        when(mundo.getName()).thenReturn("Crossiants");
        when(mundo.getEnvironment()).thenReturn(World.Environment.NORMAL);
        alex = jugador("Alex");
        crosszy = jugador("Crosszy");
        posicionAlex = new AtomicReference<>(new Location(mundo, 1, 64, 1));
        when(alex.getLocation()).thenAnswer(call -> posicionAlex.get());
        when(alex.getWorld()).thenAnswer(call -> posicionAlex.get().getWorld());
        when(crosszy.getLocation()).thenReturn(new Location(mundo, 100, 70, 100));
        when(crosszy.getWorld()).thenReturn(mundo);

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), eq(0L), eq(20L))).thenAnswer(call -> {
            callbacks.add(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        ScoreboardManager scoreboards = mock(ScoreboardManager.class);
        when(scoreboards.getMainScoreboard()).thenReturn(mock(Scoreboard.class));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(scoreboards);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(call -> List.of(alex, crosszy));
        for (Player player : List.of(alex, crosszy)) {
            bukkit.when(() -> Bukkit.getPlayerExact(player.getName())).thenReturn(player);
            bukkit.when(() -> Bukkit.getPlayer(player.getUniqueId())).thenReturn(player);
        }
        tpa = new Tpa(plugin);
        dimensiones = new Dimensiones(plugin);
    }

    @AfterEach
    void cleanup() {
        bukkit.close();
    }

    private Player jugador(String nombre) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(nombre);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.spigot()).thenReturn(mock(Player.Spigot.class));
        when(player.teleport(any(Location.class))).thenReturn(true);
        return player;
    }

    private void comando(CommandSender sender, String nombre, String... args) {
        Command command = mock(Command.class);
        when(command.getName()).thenReturn(nombre);
        if (nombre.equals("dimension")) dimensiones.onCommand(sender, command, nombre, args);
        else tpa.onCommand(sender, command, nombre, args);
    }

    private List<String> mensajes(Player player) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues().stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
    }

    private void cuentaCompleta() {
        for (int i = 0; i < 6; i++) callbacks.getLast().run();
    }

    @Test
    void acceptingWaitsFiveSecondsThenBringsTheRequesterOver() {
        comando(alex, "tpa", "Crosszy");
        assertTrue(mensajes(crosszy).contains("⚙ El jugador Alex quiere teletransportarse a donde tú estás [Aceptar] [Rechazar]"));
        assertTrue(mensajes(alex).contains("⚙ Has enviado una petición de teletransporte a Crosszy, espera a que la acepte."));

        comando(crosszy, "tpaccept");
        assertTrue(mensajes(crosszy).contains("⚙ Has aceptado la petición de teletransporte de Alex"));
        assertTrue(mensajes(alex).contains("⚙ Crosszy ha aceptado tu petición de teletransporte"));
        for (int i = 0; i < 5; i++) callbacks.getLast().run();
        verify(alex, never()).teleport(any(Location.class));
        callbacks.getLast().run();
        verify(alex).teleport(crosszy.getLocation());
    }

    @Test
    void denyingSendsBothMessagesAndNobodyMoves() {
        comando(alex, "tpa", "Crosszy");
        comando(crosszy, "tpdeny", "Alex");
        assertTrue(mensajes(crosszy).contains("⚙ Has rechazado la petición de teletransporte de Alex"));
        assertTrue(mensajes(alex).contains("⚙ Crosszy te ha rechazado la petición de teletransporte"));
        assertTrue(callbacks.isEmpty());
        comando(crosszy, "tpaccept");
        assertTrue(mensajes(crosszy).contains("⚙ No tienes peticiones de teletransporte pendientes."));
    }

    @Test
    void movingDuringTheCountdownCancelsIt() {
        comando(alex, "tpa", "Crosszy");
        comando(crosszy, "tpaccept");
        callbacks.getLast().run();
        Location antes = posicionAlex.get();
        posicionAlex.set(antes.clone().add(1, 0, 0));
        tpa.onMove(new PlayerMoveEvent(alex, antes, posicionAlex.get()));
        cuentaCompleta();
        verify(alex, never()).teleport(any(Location.class));
        assertTrue(mensajes(alex).contains("⚙ ¡Teletransporte cancelado por moverte!"));
    }

    @Test
    void aClosedDimensionStopsTheTpaUntilItOpensAgain() {
        World nether = mock(World.class);
        when(nether.getName()).thenReturn("Crossiants_nether");
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(crosszy.getWorld()).thenReturn(nether);
        CommandSender admin = mock(CommandSender.class);

        comando(admin, "dimension", "cerrar", "nether");
        assertTrue(Dimensiones.cerrada(Dimensiones.Dimension.NETHER));
        comando(alex, "tpa", "Crosszy");
        comando(crosszy, "tpaccept");
        assertTrue(callbacks.isEmpty());
        assertTrue(mensajes(alex).contains("⚙ La dimensión de Crosszy está cerrada temporalmente."));

        comando(admin, "dimension", "abrir", "nether");
        comando(alex, "tpa", "Crosszy");
        comando(crosszy, "tpaccept");
        cuentaCompleta();
        verify(alex).teleport(crosszy.getLocation());
    }
}
