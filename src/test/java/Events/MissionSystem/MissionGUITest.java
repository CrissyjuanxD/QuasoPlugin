package Events.MissionSystem;

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

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MissionGUITest {
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
