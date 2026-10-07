package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import support.PaperTestRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AutoAnnouncerTest {
    @BeforeAll
    static void initializePaper() { PaperTestRegistry.initialize(); }

    private record Scheduled(Runnable callback, BukkitTask task, long delay, long period) {}

    private BukkitScheduler scheduler(List<Scheduled> scheduled) {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            BukkitTask task = mock(BukkitTask.class);
            when(task.getTaskId()).thenReturn(scheduled.size() + 1);
            scheduled.add(new Scheduled(call.getArgument(1), task, call.getArgument(2), call.getArgument(3)));
            return task;
        });
        return scheduler;
    }

    @Test
    void resumesNormalAnnouncementsAfterSeveralEmptyServerCycles() {
        List<Scheduled> tasks = new ArrayList<>();
        BukkitScheduler scheduler = scheduler(tasks);
        List<Player> online = new ArrayList<>();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(call -> online);
            AutoAnnouncer announcer = new AutoAnnouncer(mock(JavaPlugin.class));
            assertEquals(12000L, tasks.getFirst().period());
            for (int i = 0; i < 21; i++) tasks.getFirst().callback().run();
            Player player = mock(Player.class);
            online.add(player);
            assertDoesNotThrow(tasks.getFirst().callback()::run);
            verify(player).sendMessage(contains(AutoAnnouncer.MENSAJES.get(21 % AutoAnnouncer.MENSAJES.size())));
            for (int i = 0; i < 14; i++) tasks.getFirst().callback().run();
            verify(player, times(15)).sendMessage(anyString());
            announcer.shutdown();
            verify(tasks.getFirst().task()).cancel();
        }
    }

    @Test
    void burstCompletesExactlyOnePassAndRestartsNormalTimer() {
        List<Scheduled> tasks = new ArrayList<>();
        BukkitScheduler scheduler = scheduler(tasks);
        Player player = mock(Player.class);
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("viciont_hardcore3.command.autoanuncio")).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            AutoAnnouncer announcer = new AutoAnnouncer(mock(JavaPlugin.class));
            announcer.onCommand(sender, null, "autoanuncio", new String[0]);
            Scheduled burst = tasks.get(1);
            assertEquals(600L, burst.period());
            verify(tasks.getFirst().task()).cancel();
            int tips = AutoAnnouncer.MENSAJES.size();
            for (int i = 0; i < tips; i++) burst.callback().run();
            verify(player, times(tips)).sendMessage(anyString());
            burst.callback().run();
            assertEquals(3, tasks.size());
            assertEquals(12000L, tasks.get(2).period());
            verify(scheduler).cancelTask(2);
            tasks.get(2).callback().run();
            verify(player, times(tips + 1)).sendMessage(anyString());
            announcer.shutdown();
            verify(tasks.get(2).task()).cancel();
        }
    }

    @Test
    void theTipsTalkAboutThisServerAndFitInTheChat() {
        assertTrue(AutoAnnouncer.MENSAJES.size() >= 10);
        for (String tip : AutoAnnouncer.MENSAJES) {
            assertFalse(tip.contains("ManuCoins"), tip);
            assertFalse(tip.contains("/skin"), tip);
            assertTrue(tip.length() <= 256, tip);
        }
    }
}
