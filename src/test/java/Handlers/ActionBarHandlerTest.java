package Handlers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActionBarHandlerTest {
    private JavaPlugin plugin;
    private ActionBarHandler actionBars;
    private Player player;
    private PluginManager manager;
    private BukkitScheduler scheduler;
    private BukkitTask task;
    private MockedStatic<Bukkit> bukkit;
    private final AtomicReference<Runnable> timer = new AtomicReference<>();
    private final List<String> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        manager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        scheduler = mock(BukkitScheduler.class);
        task = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(10L), eq(10L)))
                .thenAnswer(call -> {
                    timer.set(call.getArgument(1));
                    return task;
                });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        player = player(messages);
        actionBars = ActionBarHandler.get(plugin);
    }

    @AfterEach
    void tearDown() {
        ActionBarHandler.shutdown(plugin);
        bukkit.close();
    }

    private Player player(List<String> output) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);
        doAnswer(call -> {
            output.add(BaseComponent.toPlainText((BaseComponent[]) call.getRawArguments()[1]));
            return null;
        }).when(spigot).sendMessage(eq(ChatMessageType.ACTION_BAR), any(BaseComponent[].class));
        return player;
    }

    private void tick(int refreshes) {
        for (int i = 0; i < refreshes; i++) timer.get().run();
    }

    private String visible() {
        return messages.getLast();
    }

    @Test
    void threeSimultaneousObjectivesKeepTheirOrderAndFiveSecondsEach() {
        actionBars.sendNotification(player, "one", "Primero");
        actionBars.sendNotification(player, "two", "Segundo");
        actionBars.sendNotification(player, "three", "Tercero");
        assertEquals("Primero", visible());
        tick(9);
        assertEquals("Primero", visible());
        tick(1);
        assertEquals("Segundo", visible());
        tick(9);
        assertEquals("Segundo", visible());
        tick(1);
        assertEquals("Tercero", visible());
        tick(10);
        assertEquals("Tercero", visible());
        assertFalse(messages.contains(""));
    }

    @Test
    void theLastMessageFadesOutOnTheClientInsteadOfBeingErased() {
        actionBars.sendNotification(player, "job", "Minería +5 XP");
        tick(4);
        // Último reenvío a los 2 segundos: el cliente lo deja 3 más y lo desvanece justo al llegar a los 5
        assertEquals(5, messages.size());
        tick(6);
        assertEquals(5, messages.size());
        assertEquals("Minería +5 XP", visible());
        actionBars.sendNotification(player, "next", "Siguiente");
        assertEquals("Siguiente", visible());
    }

    @Test
    void aMessageWithSomethingWaitingIsResentUntilItsTurnEnds() {
        actionBars.sendNotification(player, "one", "Primero");
        actionBars.sendNotification(player, "two", "Segundo");
        tick(7);
        assertEquals(8, messages.size());
        tick(2);
        assertEquals(8, messages.size());
        tick(1);
        assertEquals("Segundo", visible());
    }

    @Test
    void jobProgressStaysOnScreenWhileItKeepsChangingAndNothingElseWaits() {
        actionBars.sendProgress(player, "trabajo", "Minería +1 XP");
        tick(8);
        actionBars.sendProgress(player, "trabajo", "Minería +2 XP");
        tick(8);
        assertEquals("Minería +2 XP", visible());
        actionBars.sendProgress(player, "mission", "Misión 1/10");
        actionBars.sendProgress(player, "trabajo", "Minería +3 XP");
        assertEquals("Minería +3 XP", visible());
        tick(2);
        assertEquals("Misión 1/10", visible());
    }

    @Test
    void theAmuletBackgroundWaitsForTheMissionAndResumesWithItsLatestState() {
        actionBars.setBackground(player, "amulet", "Amuleto activado");
        actionBars.sendNotification(player, "objective", "Objetivo completado");
        for (int i = 0; i < 30; i++) {
            actionBars.setBackground(player, "amulet", "Amuleto: -1 diamante");
        }
        assertEquals("Objetivo completado", visible());
        tick(9);
        assertEquals("Objetivo completado", visible());
        tick(1);
        assertEquals("Amuleto: -1 diamante", visible());
    }

    @Test
    void deactivatingTheAmuletDoesNotEraseCurrentOrQueuedMissionMessages() {
        actionBars.setBackground(player, "amulet", "Amuleto");
        actionBars.sendNotification(player, "one", "Primero");
        actionBars.sendNotification(player, "two", "Segundo");
        actionBars.clearBackground(player, "amulet");
        assertEquals("Primero", visible());
        tick(10);
        assertEquals("Segundo", visible());
        tick(10);
        assertEquals("Segundo", visible());
        assertFalse(messages.contains(""));
    }

    @Test
    void frequentProgressUpdatesAreCoalescedWithoutExtendingTheCurrentTurn() {
        actionBars.sendProgress(player, "one", "Progreso A: 1");
        actionBars.sendProgress(player, "two", "Progreso B: 1");
        tick(5);
        for (int i = 2; i <= 1000; i++) {
            actionBars.sendProgress(player, "one", "Progreso A: " + i);
            actionBars.sendProgress(player, "two", "Progreso B: " + i);
        }
        assertEquals("Progreso A: 1000", visible());
        tick(5);
        assertEquals("Progreso B: 1000", visible());
        tick(10);
        assertEquals("Progreso B: 1000", visible());
    }

    @Test
    void completingAnObjectiveReplacesItsObsoletePendingProgress() {
        actionBars.sendNotification(player, "previous", "Aviso anterior");
        actionBars.sendProgress(player, "goal", "Objetivo 1/10");
        actionBars.sendProgress(player, "goal", "Objetivo 2/10");
        actionBars.sendNotification(player, "goal", "Objetivo 10/10");
        actionBars.sendProgress(player, "goal", "Objetivo 9/10");
        tick(10);
        assertEquals("Objetivo 10/10", visible());
        tick(10);
        assertEquals("Objetivo 10/10", visible());
        assertFalse(messages.contains("Objetivo 1/10"));
        assertFalse(messages.contains("Objetivo 2/10"));
        assertFalse(messages.contains("Objetivo 9/10"));
    }

    @Test
    void repeatedNotificationsForTheSameObjectiveDoNotBuildAnEndlessQueue() {
        actionBars.sendNotification(player, "goal", "Completado");
        for (int i = 0; i < 100; i++) actionBars.sendNotification(player, "goal", "Completado");
        tick(10);
        assertEquals("Completado", visible());
        int sent = messages.size();
        tick(20);
        assertEquals(sent, messages.size());
    }

    @Test
    void separateHandlerInstancesShareOneQueueAndOneTimerPerPlugin() {
        ActionBarHandler another = new ActionBarHandler(plugin);
        actionBars.setBackground(player, "amulet", "Amuleto");
        another.sendNotification(player, "mission", "Misión");
        actionBars.setBackground(player, "amulet", "Amuleto actualizado");
        assertEquals("Misión", visible());
        tick(10);
        assertEquals("Amuleto actualizado", visible());
        verify(scheduler, times(1)).runTaskTimer(eq(plugin), any(Runnable.class), eq(10L), eq(10L));
        verify(manager, times(1)).registerEvents(any(Listener.class), eq(plugin));
    }

    @Test
    void playersHaveIndependentQueuesAndLeavingDiscardsTheOldSession() {
        List<String> otherMessages = new ArrayList<>();
        Player other = player(otherMessages);
        actionBars.sendNotification(player, "goal", "Jugador A");
        actionBars.sendNotification(other, "goal", "Jugador B");
        actionBars.sendNotification(player, "pending", "Pendiente A");
        ArgumentCaptor<Listener> registered = ArgumentCaptor.forClass(Listener.class);
        verify(manager).registerEvents(registered.capture(), eq(plugin));
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        ((ActionBarHandler) registered.getValue()).onQuit(quit);
        tick(10);
        assertFalse(messages.contains("Pendiente A"));
        assertEquals("Jugador B", otherMessages.getLast());
        actionBars.sendNotification(player, "new", "Sesión nueva");
        assertEquals("Sesión nueva", visible());
    }

    @Test
    void offlinePlayersStopReceivingMessagesEvenWithoutTheQuitEvent() {
        actionBars.setBackground(player, "amulet", "Amuleto");
        actionBars.sendNotification(player, "pending", "Misión");
        when(player.isOnline()).thenReturn(false);
        int sent = messages.size();
        tick(20);
        assertEquals(sent, messages.size());
        when(player.isOnline()).thenReturn(true);
        actionBars.sendNotification(player, "new", "Otra sesión");
        assertEquals("Otra sesión", visible());
    }

    @Test
    void anOverloadedProgressQueueStaysBoundedAndStillAcceptsCompletionNotifications() {
        actionBars.sendNotification(player, "current", "Aviso actual");
        for (int i = 0; i < 1000; i++) actionBars.sendProgress(player, "progress:" + i, "Progreso " + i);
        actionBars.sendNotification(player, "important", "Objetivo importante completado");
        tick(700);
        int sent = messages.size();
        tick(20);
        assertEquals(sent, messages.size());
        assertTrue(messages.contains("Objetivo importante completado"));
        assertTrue(messages.stream().distinct().count() <= 66);
    }

    @Test
    void shutdownCancelsTheTimerAndRemovesMessagesBeforeAReplacementCoordinatorStarts() {
        actionBars.setBackground(player, "amulet", "Amuleto");
        actionBars.sendNotification(player, "old", "Misión antigua");
        ActionBarHandler.shutdown(plugin);
        verify(task).cancel();
        assertEquals("", visible());
        actionBars.sendNotification(player, "new", "Misión nueva");
        assertEquals("Misión nueva", visible());
        tick(10);
        assertEquals("Misión nueva", visible());
        verify(scheduler, times(2)).runTaskTimer(eq(plugin), any(Runnable.class), eq(10L), eq(10L));
    }
}
