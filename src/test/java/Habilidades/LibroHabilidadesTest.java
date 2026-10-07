package Habilidades;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.PaperTestRegistry;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LibroHabilidadesTest {
    @BeforeAll
    static void initializePaper() { PaperTestRegistry.initialize(); }

    @Test
    void theTreeOpensWithTheBookOrWithLevelsBoughtBefore(@TempDir File folder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder);
        HabilidadesManager manager = new HabilidadesManager(plugin);
        UUID nuevo = UUID.randomUUID();
        UUID antiguo = UUID.randomUUID();

        assertFalse(manager.tieneAcceso(nuevo));
        manager.darAcceso(nuevo);
        manager.unlockHabilidad(antiguo, HabilidadesType.AGILIDAD, 1);

        HabilidadesManager recargado = new HabilidadesManager(plugin);
        assertTrue(recargado.tieneAcceso(nuevo));
        assertTrue(recargado.tieneAcceso(antiguo));
        assertFalse(recargado.tieneAcceso(UUID.randomUUID()));
    }

    @Test
    void theBookIsSpentOnlyTheFirstTime() {
        HabilidadesManager manager = mock(HabilidadesManager.class);
        HabilidadesEffects effects = mock(HabilidadesEffects.class);
        HabilidadesListener listener = new HabilidadesListener(mock(JavaPlugin.class), manager, effects);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack libro = libro(2);
        ItemStack queda = mock(ItemStack.class);
        when(libro.asQuantity(1)).thenReturn(queda);

        PlayerInteractEvent primera = click(player, libro);
        listener.onInteract(primera);
        verify(primera).setCancelled(true);
        verify(inventory).setItem(EquipmentSlot.HAND, queda);
        verify(manager).darAcceso(uuid);
        verify(effects).playLibroAnimation(player);

        when(manager.tieneAcceso(uuid)).thenReturn(true);
        PlayerInteractEvent segunda = click(player, libro);
        listener.onInteract(segunda);
        verify(segunda).setCancelled(true);
        verify(inventory, times(1)).setItem(any(EquipmentSlot.class), any());
        verify(manager, times(1)).darAcceso(uuid);
        verify(player).sendMessage(contains("/menu"));

        // El último libro se gasta entero y un libro cualquiera no hace nada
        when(manager.tieneAcceso(uuid)).thenReturn(false);
        listener.onInteract(click(player, libro(1)));
        verify(inventory).setItem(EquipmentSlot.HAND, null);
        ItemStack otro = mock(ItemStack.class);
        when(otro.getType()).thenReturn(Material.KNOWLEDGE_BOOK);
        PlayerInteractEvent comun = click(player, otro);
        listener.onInteract(comun);
        verify(comun, never()).setCancelled(true);
    }

    private static ItemStack libro(int cantidad) {
        ItemStack libro = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(libro.getType()).thenReturn(Material.KNOWLEDGE_BOOK);
        when(libro.hasItemMeta()).thenReturn(true);
        when(libro.getItemMeta()).thenReturn(meta);
        when(libro.getAmount()).thenReturn(cantidad);
        when(meta.hasCustomModelData()).thenReturn(true);
        when(meta.getCustomModelData()).thenReturn(9999);
        return libro;
    }

    private static PlayerInteractEvent click(Player player, ItemStack item) {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getItem()).thenReturn(item);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }
}
