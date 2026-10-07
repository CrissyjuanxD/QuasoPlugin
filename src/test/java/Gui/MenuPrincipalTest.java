package Gui;

import Events.MissionSystem.MissionGUI;
import Habilidades.HabilidadesGUI;
import Habilidades.HabilidadesManager;
import Trabajos.TrabajosManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import support.PaperTestRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuPrincipalTest {
    @BeforeAll
    static void initializePaper() { PaperTestRegistry.initialize(); }

    @Test
    void theZonesUseTheSlotsOfTheDrawing() {
        assertArrayEquals(new int[]{1, 2, 3, 10, 11, 12, 19, 20, 21}, SeccionMenu.MISIONES.slots());
        assertArrayEquals(new int[]{5, 6, 7, 14, 15, 16, 23, 24, 25}, SeccionMenu.TRABAJOS.slots());
        assertArrayEquals(new int[]{27, 28, 29, 36, 37, 38, 45, 46, 47}, SeccionMenu.HABILIDADES.slots());
        assertArrayEquals(new int[]{33, 34, 35, 42, 43, 44, 51, 52, 53}, SeccionMenu.PROTECCIONES.slots());
        assertArrayEquals(new int[]{39, 40, 41, 48, 49, 50}, SeccionMenu.HOMES.slots());

        Set<Integer> used = new HashSet<>();
        for (SeccionMenu seccion : SeccionMenu.values()) {
            for (int slot : seccion.slots()) {
                assertTrue(used.add(slot), "slot repetido " + slot);
                assertEquals(seccion, SeccionMenu.porSlot(slot));
            }
        }
        int[] fondo = IntStream.range(0, 54).filter(slot -> SeccionMenu.porSlot(slot) == null).toArray();
        assertArrayEquals(new int[]{0, 4, 8, 9, 13, 17, 18, 22, 26, 30, 31, 32}, fondo);
        assertNull(SeccionMenu.porSlot(54));
    }

    @Test
    void eachZoneOpensItsScreenAndTheTreeNeedsTheBook() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        MissionGUI misiones = mock(MissionGUI.class);
        TrabajosManager trabajos = mock(TrabajosManager.class);
        HabilidadesManager habilidades = mock(HabilidadesManager.class);
        HabilidadesGUI arbol = mock(HabilidadesGUI.class);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(54);
        AtomicReference<InventoryHolder> holder = new AtomicReference<>();
        when(inventory.getHolder()).thenAnswer(call -> holder.get());
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return null;
        });

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<ItemStack> stacks = mockConstruction(ItemStack.class,
                     (stack, context) -> when(stack.getItemMeta()).thenReturn(mock(ItemMeta.class)))) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), anyString())).thenAnswer(call -> {
                holder.set(call.getArgument(0));
                return inventory;
            });
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            MenuPrincipal menu = new MenuPrincipal(plugin, misiones, trabajos, habilidades, arbol);

            assertTrue(menu.onCommand(player, null, "menu", new String[0]));
            verify(player).openInventory(inventory);
            for (int slot = 0; slot < 54; slot++) verify(inventory).setItem(eq(slot), any());
            // 5 botones invisibles con nombre y 1 relleno sin tooltip
            assertEquals(6, stacks.constructed().size());
            for (ItemStack stack : stacks.constructed()) verify(stack.getItemMeta()).setItemModel(NamespacedKey.minecraft("air"));
            verify(stacks.constructed().getLast().getItemMeta()).setHideTooltip(true);

            click(menu, inventory, player, 1);
            verify(misiones).openMissionGUI(player);
            click(menu, inventory, player, 25);
            verify(trabajos).abrirMenu(player);

            click(menu, inventory, player, 27);
            verify(arbol, never()).openHabilidadesGUI(player);
            verify(player).sendMessage(contains("Libro de Habilidades"));
            when(habilidades.tieneAcceso(uuid)).thenReturn(true);
            click(menu, inventory, player, 47);
            verify(arbol).openHabilidadesGUI(player);

            click(menu, inventory, player, 33);
            verify(player).performCommand("proteccion");
            click(menu, inventory, player, 50);
            verify(player).performCommand("home list");
            verify(player, times(2)).closeInventory();

            // El fondo y el inventario del jugador no hacen nada, pero igual se cancelan
            click(menu, inventory, player, 31);
            InventoryClickEvent abajo = mock(InventoryClickEvent.class);
            when(abajo.getInventory()).thenReturn(inventory);
            when(abajo.getClickedInventory()).thenReturn(mock(Inventory.class));
            when(abajo.getWhoClicked()).thenReturn(player);
            when(abajo.getRawSlot()).thenReturn(60);
            menu.onClick(abajo);
            verify(abajo).setCancelled(true);
            verify(scheduler, times(5)).runTask(any(Plugin.class), any(Runnable.class));

            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getInventory()).thenReturn(inventory);
            menu.onDrag(drag);
            verify(drag).setCancelled(true);
        }
    }

    private static InventoryClickEvent click(MenuPrincipal menu, Inventory inventory, Player player, int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot);
        menu.onClick(event);
        verify(event).setCancelled(true);
        return event;
    }
}
