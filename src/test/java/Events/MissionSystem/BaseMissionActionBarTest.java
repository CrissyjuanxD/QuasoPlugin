package Events.MissionSystem;

import Handlers.ActionBarHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BaseMissionActionBarTest {
    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }

    @Test
    void completingThreeObjectivesTogetherQueuesEveryObjectiveIncludingTheLastOne() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        MissionHandler handler = mock(MissionHandler.class);
        Player player = mock(Player.class);
        MissionData data = new MissionData();
        data.setActive(true);
        when(handler.isMissionActive(player, 7)).thenReturn(true);
        when(handler.getData(player, 7)).thenReturn(data);
        when(handler.tag(7)).thenReturn("#7");
        ActionBarHandler actionBars = mock(ActionBarHandler.class);

        try (MockedStatic<ActionBarHandler> shared = mockStatic(ActionBarHandler.class)) {
            shared.when(() -> ActionBarHandler.get(plugin)).thenReturn(actionBars);
            ThreeObjectives mission = new ThreeObjectives(plugin, handler);
            mission.finish(player, "first");
            mission.finish(player, "second");
            mission.finish(player, "third");

            InOrder order = inOrder(actionBars, handler);
            order.verify(actionBars).sendNotification(eq(player), eq("mission:7:first"), contains("Primero"));
            order.verify(actionBars).sendNotification(eq(player), eq("mission:7:second"), contains("Segundo"));
            order.verify(actionBars).sendNotification(eq(player), eq("mission:7:third"), contains("Tercero"));
            order.verify(handler).completeMission(player, 7);
            verify(actionBars, never()).sendProgress(any(), anyString(), anyString());
        }
    }

    @Test
    void differentProgressObjectivesUseDifferentQueueKeysAndCompletedObjectivesUseNotifications() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        MissionHandler handler = mock(MissionHandler.class);
        Player player = mock(Player.class);
        MissionData data = new MissionData();
        data.setActive(true);
        when(handler.isMissionActive(player, 8)).thenReturn(true);
        when(handler.getData(player, 8)).thenReturn(data);
        when(handler.tag(8)).thenReturn("#8");
        ActionBarHandler actionBars = mock(ActionBarHandler.class);

        try (MockedStatic<ActionBarHandler> shared = mockStatic(ActionBarHandler.class)) {
            shared.when(() -> ActionBarHandler.get(plugin)).thenReturn(actionBars);
            CountingObjectives mission = new CountingObjectives(plugin, handler);
            mission.increment(player, "first");
            mission.increment(player, "second");
            mission.increment(player, "first");

            InOrder order = inOrder(actionBars);
            order.verify(actionBars).sendProgress(eq(player), eq("mission:8:first"), contains("Primero"));
            order.verify(actionBars).sendProgress(eq(player), eq("mission:8:second"), contains("Segundo"));
            order.verify(actionBars).sendNotification(eq(player), eq("mission:8:first"), contains("Primero"));
            verify(handler, never()).completeMission(any(Player.class), anyInt());
        }
    }

    @Test
    void objectivesCalculatedFromLiveStatisticsAreAlsoQueuedBeforeCompletingTheMission() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        MissionHandler handler = mock(MissionHandler.class);
        Player player = mock(Player.class);
        MissionData data = new MissionData();
        data.setActive(true);
        when(handler.isMissionActive(player, 9)).thenReturn(true);
        when(handler.getData(player, 9)).thenReturn(data);
        when(handler.tag(9)).thenReturn("#9");
        ActionBarHandler actionBars = mock(ActionBarHandler.class);
        try (MockedStatic<ActionBarHandler> shared = mockStatic(ActionBarHandler.class)) {
            shared.when(() -> ActionBarHandler.get(plugin)).thenReturn(actionBars);
            LiveObjectives mission = new LiveObjectives(plugin, handler);
            mission.poll(player);
            InOrder order = inOrder(actionBars, handler);
            order.verify(actionBars).sendNotification(eq(player), eq("mission:9:first"), contains("Primero"));
            order.verify(actionBars).sendNotification(eq(player), eq("mission:9:second"), contains("Segundo"));
            order.verify(actionBars).sendNotification(eq(player), eq("mission:9:third"), contains("Tercero"));
            order.verify(handler).completeMission(player, 9);
        }
    }

    private static final class ThreeObjectives extends BaseMission {
        ThreeObjectives(JavaPlugin plugin, MissionHandler handler) {
            super(plugin, handler, 7, "Prueba", MissionDifficulty.MEDIA, 1, "Prueba de objetivos.");
            flag("first", "Primero");
            flag("second", "Segundo");
            flag("third", "Tercero");
        }
        void finish(Player player, String key) { mark(player, key); }
        @Override protected List<List<ItemStack>> rewardItems() { return List.of(); }
    }

    private static final class CountingObjectives extends BaseMission {
        CountingObjectives(JavaPlugin plugin, MissionHandler handler) {
            super(plugin, handler, 8, "Prueba", MissionDifficulty.MEDIA, 1, "Prueba de progreso.");
            counter("first", "Primero", 2);
            counter("second", "Segundo", 2);
        }
        void increment(Player player, String key) { add(player, key, 1); }
        @Override protected List<List<ItemStack>> rewardItems() { return List.of(); }
    }

    private static final class LiveObjectives extends BaseMission {
        LiveObjectives(JavaPlugin plugin, MissionHandler handler) {
            super(plugin, handler, 9, "Prueba", MissionDifficulty.MEDIA, 1, "Prueba de estadísticas.");
            flag("first", "Primero");
            flag("second", "Segundo");
            flag("third", "Tercero");
        }
        @Override protected int value(Player player, MissionData data, MissionObjective objective) { return 1; }
        void poll(Player player) { check(player); }
        @Override protected List<List<ItemStack>> rewardItems() { return List.of(); }
    }
}
