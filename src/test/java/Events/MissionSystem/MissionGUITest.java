package Events.MissionSystem;

import items.ItemModels;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MissionGUITest {
    @Test
    void extraMissionsKeepTheirParentAndOrderButHideTheReleaseExplanationInTheirLore() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        MissionHandler handler = mock(MissionHandler.class);
        Mission parent = mock(Mission.class);
        when(parent.getMissionNumber()).thenReturn(6);
        when(parent.getDescription()).thenReturn("Misión principal.");
        Mission extra = mock(Mission.class);
        when(extra.getMissionNumber()).thenReturn(36);
        when(extra.getParentMission()).thenReturn(6);
        when(extra.getDescription()).thenReturn("Misión adicional.");
        LinkedHashMap<Integer, Mission> missions = new LinkedHashMap<>();
        missions.put(6, parent);
        missions.put(36, extra);
        when(handler.getMissions()).thenReturn(missions);
        when(handler.getExtras(6)).thenReturn(List.of(36));
        when(handler.displayName(6)).thenReturn("#6 Principal");
        when(handler.displayName(36)).thenReturn("Extra #6 Adicional");
        MissionData data = new MissionData();
        data.setActive(true);
        Player player = mock(Player.class);
        when(handler.getData(eq(player), anyInt())).thenReturn(data);
        Inventory inventory = mock(Inventory.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ItemModels> models = mockStatic(ItemModels.class);
             MockedConstruction<ItemStack> stacks = mockConstruction(ItemStack.class,
                     (stack, context) -> when(stack.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), anyString())).thenReturn(inventory);
            new MissionGUI(plugin, handler).openMissionGUI(player);

            assertEquals(4, stacks.constructed().size());
            ItemStack extraItem = stacks.constructed().getLast();
            @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> lore = ArgumentCaptor.forClass(List.class);
            verify(extraItem.getItemMeta()).setLore(lore.capture());
            List<String> plainLore = lore.getValue().stream().map(ChatColor::stripColor).toList();
            assertTrue(plainLore.contains("Misión extra"));
            assertTrue(plainLore.stream().noneMatch(line -> line.contains("sale con")));
            verify(inventory).setItem(19, extraItem);
            assertEquals(6, extra.getParentMission());
        }
    }

    @Test
    void keepsHeaderEmptyAndBlocksClickAndDragTransfers() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        MissionHandler handler = mock(MissionHandler.class);
        when(handler.getMissions()).thenReturn(new LinkedHashMap<>());
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        AtomicReference<InventoryHolder> holder = new AtomicReference<>();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<ItemStack> stacks = mockConstruction(ItemStack.class,
                     (stack, context) -> when(stack.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), anyString()))
                    .thenAnswer(call -> {
                        holder.set(call.getArgument(0));
                        return inventory;
                    });
            when(inventory.getHolder()).thenAnswer(call -> holder.get());
            MissionGUI gui = new MissionGUI(plugin, handler);
            gui.openMissionGUI(player);

            for (int slot = 0; slot < 18; slot++) {
                verify(inventory, never()).setItem(eq(slot), any());
            }
            verify(inventory).setItem(eq(45), any());
            verify(inventory).setItem(eq(53), any());
            verify(player).openInventory(inventory);
            assertEquals(2, stacks.constructed().size());

            // Empty header and lower inventory must be protected too (including shift clicks).
            for (int slot : new int[]{0, 17, 60}) {
                InventoryClickEvent click = mock(InventoryClickEvent.class);
                when(click.getInventory()).thenReturn(inventory);
                when(click.getWhoClicked()).thenReturn(player);
                when(click.getRawSlot()).thenReturn(slot);
                gui.onInventoryClick(click);
                verify(click).setCancelled(true);
            }
            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getInventory()).thenReturn(inventory);
            gui.onInventoryDrag(drag);
            verify(drag).setCancelled(true);
        }
    }
}
