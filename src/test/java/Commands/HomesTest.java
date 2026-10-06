package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogInstancesProvider;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomesTest {
    @TempDir Path directory;
    private Homes homes;
    private QuasoPlugin plugin;
    private PluginManager manager;
    private Player player;
    private World world;
    private Command command;
    private AtomicReference<Location> position;
    private final List<Runnable> callbacks = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;

    @BeforeAll
    static void initializePaper() throws ClassNotFoundException {
        RegistryAccess access = PaperTestRegistry.initialize();
        try (MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class)) {
            registry.when(RegistryAccess::registryAccess).thenReturn(access);
            Class.forName(Dialog.class.getName());
        }
    }

    @BeforeEach
    void setup() throws Exception {
        Files.writeString(directory.resolve("homes.yml"), "Homes:\n  Crosszy:\n    base: 'world, 10, 70, 20'\n");
        plugin = mock(QuasoPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        Server server = mock(Server.class);
        manager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(manager);
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

    @Test
    void clientsFrom1216ReceiveTenDialogSlotsAndButtonsUseTheOrdinaryHomeCommand() {
        when(player.getProtocolVersion()).thenReturn(771);
        try (DialogFixture fixture = new DialogFixture()) {
            assertTrue(homes.onCommand(player, command, "home", new String[]{"list"}));
            verify(player).showDialog(fixture.dialog);
            verify(fixture.typeBuilder).columns(2);
            verify(fixture.baseBuilder).canCloseWithEscape(true);
            verify(fixture.baseBuilder).pause(false);
            verify(fixture.baseBuilder).afterAction(DialogBase.DialogAfterAction.CLOSE);
            assertEquals(10, fixture.buttons.size());
            ActionButton first = fixture.buttons.getFirst();
            assertEquals("1 · base", plain(first.label()));
            ClickEvent<?> click = ((DialogAction.StaticAction) first.action()).value();
            assertEquals(ClickEvent.Action.RUN_COMMAND, click.action());
            assertEquals("/home base", ((ClickEvent.Payload.Text) click.payload()).value());
            for (ActionButton button : fixture.buttons.subList(1, 10)) {
                assertNull(button.action(), "Las posiciones libres no deben ejecutar ningún comando");
            }
            assertNotTeleported();
            assertTrue(callbacks.isEmpty());
            homes.onCommand(player, command, "home", new String[]{"base"});
            assertNotTeleported();
            runToCompletion(callbacks.getFirst());
            verify(player).teleport(any(Location.class));
        }
    }

    @Test
    void olderClientsGetClickableChatWithoutReplacingAnExistingCountdown() {
        when(player.getProtocolVersion()).thenReturn(770);
        start();
        homes.onCommand(player, command, "home", new String[]{"list"});
        List<Component> messages = chatMessages();
        assertEquals(12, messages.size());
        assertTrue(plain(messages.getFirst()).contains("1/10"));
        ClickEvent<?> click = messages.get(1).clickEvent();
        assertNotNull(click);
        assertEquals("/home base", ((ClickEvent.Payload.Text) click.payload()).value());
        assertEquals(1, callbacks.size());
        verify(player, never()).showDialog(any());
        runToCompletion(callbacks.getFirst());
        verify(player).teleport(any(Location.class));
    }

    @Test
    void unknownTranslatedClientsFallBackToChatInsteadOfAssumingTheServerProtocol() {
        Plugin via = mock(Plugin.class);
        when(via.isEnabled()).thenReturn(true);
        when(manager.getPlugin("ViaVersion")).thenReturn(via);
        when(player.getProtocolVersion()).thenReturn(999);
        // La API de Via no está en el classpath del test; el protocolo de Paper puede ser el traducido.
        homes.onCommand(player, command, "home", new String[]{"list"});
        verify(player, never()).getProtocolVersion();
        verify(player, never()).showDialog(any());
        assertEquals(12, chatMessages().size());
    }

    @Test
    void emptyHomeListsShowAllFreeSlotsAndStillOfferTheListCompletion() throws Exception {
        Files.writeString(directory.resolve("homes.yml"), "Homes: {}\n");
        homes = new Homes(plugin);
        homes.onCommand(player, command, "home", new String[]{"list"});
        List<Component> messages = chatMessages();
        assertTrue(plain(messages.getFirst()).contains("0/10"));
        assertEquals(10, messages.stream().filter(message -> plain(message).contains("Disponible")).count());
        assertTrue(messages.stream().allMatch(message -> message.clickEvent() == null));
        assertEquals(List.of("list"), homes.onTabComplete(player, command, "home", new String[]{"l"}));
        assertNotTeleported();
    }

    @Test
    void persistedNamesCannotInjectACommandAndNewReservedOrPathNamesAreRejected() throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(directory.resolve("homes.yml").toFile());
        config.set("Homes.Crosszy.bad name", "world, 1, 70, 2");
        config.set("Homes.Crosszy.list", "world, 1, 70, 2");
        config.save(directory.resolve("homes.yml").toFile());
        homes = new Homes(plugin);
        homes.onCommand(player, command, "home", new String[]{"list"});
        List<Component> messages = chatMessages();
        for (Component message : messages) {
            if (plain(message).contains("bad name") || plain(message).contains("· list")) {
                assertNull(message.clickEvent());
            }
        }
        Command sethome = mock(Command.class);
        when(sethome.getName()).thenReturn("sethome");
        homes.onCommand(player, sethome, "sethome", new String[]{"list"});
        homes.onCommand(player, sethome, "sethome", new String[]{"injected.path"});
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(directory.resolve("homes.yml").toFile());
        assertEquals("world, 1, 70, 2", saved.getString("Homes.Crosszy.list"));
        assertFalse(saved.contains("Homes.Crosszy.injected"));
        assertNotTeleported();
    }

    @Test
    void malformedOrInfiniteSavedCoordinatesReportAnErrorWithoutSchedulingATeleport() throws Exception {
        for (String coordinates : List.of("world, invalid, 70, 20", "world, NaN, 70, 20")) {
            Files.writeString(directory.resolve("homes.yml"), "Homes:\n  Crosszy:\n    base: '" + coordinates + "'\n");
            homes = new Homes(plugin);
            assertDoesNotThrow(this::start);
        }
        verify(player, times(2)).sendMessage(contains("Error en los datos"));
        assertTrue(callbacks.isEmpty());
        assertNotTeleported();
    }

    private List<Component> chatMessages() {
        ArgumentCaptor<Component> messages = ArgumentCaptor.forClass(Component.class);
        verify(player, atLeastOnce()).sendMessage(messages.capture());
        return messages.getAllValues();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Proveedores de Paper simulados; conserva los componentes y acciones reales del diálogo. */
    private static final class DialogFixture implements AutoCloseable {
        final Dialog dialog = mock(Dialog.class);
        final DialogBase.Builder baseBuilder = mock(DialogBase.Builder.class, RETURNS_SELF);
        final MultiActionType.Builder typeBuilder = mock(MultiActionType.Builder.class, RETURNS_SELF);
        List<ActionButton> buttons;
        final MockedStatic<DialogInstancesProvider> instances = mockStatic(DialogInstancesProvider.class);
        final MockedStatic<Dialog> dialogs = mockStatic(Dialog.class);

        @SuppressWarnings("unchecked")
        DialogFixture() {
            DialogInstancesProvider provider = mock(DialogInstancesProvider.class);
            instances.when(DialogInstancesProvider::instance).thenReturn(provider);
            when(provider.dialogBaseBuilder(any())).thenReturn(baseBuilder);
            when(baseBuilder.build()).thenReturn(mock(DialogBase.class));
            when(provider.plainMessageDialogBody(any(), anyInt())).thenReturn(mock(PlainMessageDialogBody.class));
            when(provider.actionButtonBuilder(any())).thenAnswer(call -> new TestButtonBuilder(call.getArgument(0)));
            when(provider.staticAction(any())).thenAnswer(call -> {
                DialogAction.StaticAction action = mock(DialogAction.StaticAction.class);
                when(action.value()).thenReturn(call.getArgument(0));
                return action;
            });
            when(provider.multiAction(anyList())).thenAnswer(call -> {
                buttons = List.copyOf(call.getArgument(0));
                return typeBuilder;
            });
            when(typeBuilder.build()).thenReturn(mock(MultiActionType.class));
            RegistryBuilderFactory<Dialog, DialogRegistryEntry.Builder> factory = mock(RegistryBuilderFactory.class);
            when(factory.empty()).thenReturn(mock(DialogRegistryEntry.Builder.class, RETURNS_SELF));
            dialogs.when(() -> Dialog.create(any())).thenAnswer(call -> {
                Consumer<RegistryBuilderFactory<Dialog, ? extends DialogRegistryEntry.Builder>> builder = call.getArgument(0);
                builder.accept(factory);
                return dialog;
            });
        }

        @Override public void close() { dialogs.close(); instances.close(); }
    }

    private record TestButton(Component label, Component tooltip, int width, DialogAction action) implements ActionButton {}

    private static final class TestButtonBuilder implements ActionButton.Builder {
        private final Component label;
        private Component tooltip;
        private int width;
        private DialogAction action;
        TestButtonBuilder(Component label) { this.label = label; }
        @Override public ActionButton.Builder tooltip(Component value) { tooltip = value; return this; }
        @Override public ActionButton.Builder width(int value) { width = value; return this; }
        @Override public ActionButton.Builder action(DialogAction value) { action = value; return this; }
        @Override public ActionButton build() { return new TestButton(label, tooltip, width, action); }
    }
}
