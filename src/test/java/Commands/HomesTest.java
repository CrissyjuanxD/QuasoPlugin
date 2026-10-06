package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import support.PaperTestRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomesTest {
    @TempDir Path directory;
    private Homes homes;
    private Player player;
    private World world;
    private Command command;
    private AtomicReference<Location> position;
    private final List<Runnable> callbacks = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;

    @BeforeAll
    static void initializePaper() { PaperTestRegistry.initialize(); }

    @BeforeEach
    void setup() throws Exception {
        Files.writeString(directory.resolve("homes.yml"), "Homes:\n  Crosszy:\n    base: 'world, 10, 70, 20'\n");
        QuasoPlugin plugin = mock(QuasoPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(plugin.getServer()).thenReturn(server);
        world = mock(World.class);
        player = mock(Player.class);
        when(player.getName()).thenReturn("Crosszy");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.spigot()).thenReturn(mock(Player.Spigot.class));
        when(player.teleport(any(Location.class))).thenReturn(true);
        position = new AtomicReference<>(new Location(world, 1.25, 64, 2.5));
        when(player.getLocation()).thenAnswer(call -> position.get());
        command = mock(Command.class);
        when(command.getName()).thenReturn("home");
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), eq(0L), eq(20L))).thenAnswer(call -> {
            callbacks.add(call.getArgument(1));
            BukkitTask task = mock(BukkitTask.class);
            when(task.getTaskId()).thenReturn(tasks.size() + 1);
            tasks.add(task);
            return task;
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        homes = new Homes(plugin);
    }

    @AfterEach
    void cleanup() { bukkit.close(); }

    private void start() { assertTrue(homes.onCommand(player, command, "home", new String[0])); }
    private void runToCompletion(Runnable callback) { for (int i = 0; i < 6; i++) callback.run(); }
    private void assertNotTeleported() { verify(player, never()).teleport(any(Location.class)); }

    @Test
    void ordinaryPlayersWaitFiveSecondsThenTeleportToTheStoredHome() {
        start();
        for (int i = 0; i < 5; i++) callbacks.getFirst().run();
        assertNotTeleported();
        callbacks.getFirst().run();
        ArgumentCaptor<Location> destination = ArgumentCaptor.forClass(Location.class);
        verify(player).teleport(destination.capture());
        assertEquals(10.5, destination.getValue().getX());
        assertEquals(70, destination.getValue().getY());
        assertEquals(20.5, destination.getValue().getZ());
        verify(tasks.getFirst()).cancel();
    }

    @Test
    void operatorsTeleportImmediatelyWithoutSchedulingACountdown() {
        when(player.isOp()).thenReturn(true);
        start();
        verify(player).teleport(any(Location.class));
        assertTrue(callbacks.isEmpty());
    }

    @Test
    void movingCancelsEvenAnAlreadyQueuedCountdownCallback() {
        start();
        PlayerMoveEvent move = new PlayerMoveEvent(player, position.get(), new Location(world, 1.3, 64, 2.5));
        homes.onPlayerMove(move);
        runToCompletion(callbacks.getFirst());
        assertNotTeleported();
        verify(tasks.getFirst(), atLeastOnce()).cancel();
    }

    @Test
    void actualDamageCancelsEvenAnAlreadyQueuedCountdownCallback() {
        start();
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        when(damage.getFinalDamage()).thenReturn(2.0);
        homes.onPlayerDamage(damage);
        runToCompletion(callbacks.getFirst());
        assertNotTeleported();
        verify(tasks.getFirst(), atLeastOnce()).cancel();
    }

    @Test
    void lookingAroundAndCancelledDamageDoNotCancelTheCountdown() {
        start();
        Location looked = position.get().clone();
        looked.setYaw(90);
        looked.setPitch(30);
        homes.onPlayerMove(new PlayerMoveEvent(player, position.get(), looked));
        position.set(looked);
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        when(damage.getFinalDamage()).thenReturn(2.0);
        when(damage.isCancelled()).thenReturn(true);
        homes.onPlayerDamage(damage);
        runToCompletion(callbacks.getFirst());
        verify(player).teleport(any(Location.class));
    }

    @Test
    void onlyTheLatestHomeRequestCanTeleport() {
        start();
        start();
        runToCompletion(callbacks.getFirst());
        assertNotTeleported();
        runToCompletion(callbacks.get(1));
        verify(player).teleport(any(Location.class));
    }

    @Test
    void quitShutdownAndMovementWithoutAnEventLeaveNoPendingTeleport() {
        start();
        position.set(new Location(world, 4, 64, 2.5));
        runToCompletion(callbacks.getFirst());
        start();
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        homes.onPlayerQuit(quit);
        runToCompletion(callbacks.get(1));
        start();
        homes.shutdown();
        runToCompletion(callbacks.get(2));
        assertNotTeleported();
    }
}
