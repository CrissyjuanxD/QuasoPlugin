package Gui;

import Managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ItemCatalogTest {
    private ItemCatalog catalog;
    private ItemManager items;
    private Player player;
    private InventoryClickEvent click;
    private InventoryView view;
    private JavaPlugin plugin;
    private Inventory inventory;

    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        items = mock(ItemManager.class);
        catalog = new ItemCatalog(plugin, items);
        player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.hasPermission(ItemCatalog.PERMISSION)).thenReturn(true);
        PlayerInventory bag = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(bag);
        when(bag.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());
        inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(54);
        List<String> keys = IntStream.range(0, 100).mapToObj(i -> "item_" + i).toList();
        ItemCatalog.Page page = new ItemCatalog.Page(id, keys, 1);
        page.inventory = inventory;
        when(inventory.getHolder()).thenReturn(page);
        view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        click = mock(InventoryClickEvent.class);
        when(click.getView()).thenReturn(view);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.isLeftClick()).thenReturn(true);
    }

    @Test void grantsFreshItemsFromTheSecondPageWithoutGivingOrCopyingPreviewIcons() {
        ItemStack first = mock(ItemStack.class), second = mock(ItemStack.class);
        when(items.getItem("item_45", 1, player)).thenReturn(first, second);
        when(click.getRawSlot()).thenReturn(0);
        when(click.isShiftClick()).thenReturn(true);
        catalog.onClick(click);
        catalog.onClick(click);
        verify(click, times(2)).setCancelled(true);
        verify(player.getInventory()).addItem(first);
        verify(player.getInventory()).addItem(second);
        verify(click, never()).getCurrentItem();
        verify(first, never()).clone();
        verify(first, never()).setAmount(anyInt());
    }

    @Test void bottomInventoryAndHotbarClicksCannotTakeOrGiveItems() {
        when(click.getRawSlot()).thenReturn(60);
        catalog.onClick(click);
        when(click.getRawSlot()).thenReturn(0);
        when(click.isLeftClick()).thenReturn(false); // number key / creative middle-click
        catalog.onClick(click);
        verify(click, times(2)).setCancelled(true);
        verifyNoInteractions(items);
    }

    @Test void navigationQueuesThePageChangeWithoutGrantingAnArrow() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(click.getRawSlot()).thenReturn(53);
            catalog.onClick(click);
            verify(scheduler).runTask(eq(plugin), any(Runnable.class));
            verifyNoInteractions(items);
        }
    }

    @Test void revokedPermissionClosesCatalogAndPreventsGrants() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(player.hasPermission(ItemCatalog.PERMISSION)).thenReturn(false);
            catalog.onClick(click);
            verify(click).setCancelled(true);
            verify(scheduler).runTask(eq(plugin), any(Runnable.class));
            verifyNoInteractions(items);
        }
    }

    @Test void draggingCannotInsertOrExtractItems() {
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        catalog.onDrag(drag);
        verify(drag).setCancelled(true);
    }
}
