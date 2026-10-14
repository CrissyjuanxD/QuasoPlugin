package items;

import Managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ElementalTotemsTest {
    private Plugin plugin;
    private BukkitScheduler scheduler;
    private Player player;
    private PlayerInventory inventory;
    private ElementalTotems listener;

    @BeforeAll static void registry() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.isOnline()).thenReturn(true);
        listener = new ElementalTotems(plugin);
    }

    private ItemStack totem(String id) {
        ItemStack stack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(stack.getType()).thenReturn(Material.TOTEM_OF_UNDYING);
        when(stack.hasItemMeta()).thenReturn(true);
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(EndItems.ITEM_KEY, PersistentDataType.STRING)).thenReturn(id);
        return stack;
    }

    private void activate(String id, EquipmentSlot slot) {
        ItemStack used = totem(id);
        when(inventory.getItem(slot)).thenReturn(used);
        listener.onResurrect(new EntityResurrectEvent(player, slot));
    }

    private Runnable pending() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        return task.getValue();
    }

    @Test void sculkAddsResistanceAfterVanillaFinishesWithoutReplacingItsOtherEffects() {
        activate("sculk_totem", EquipmentSlot.HAND);
        verify(player, never()).addPotionEffect(any());
        pending().run();
        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2));
        verify(player, times(1)).addPotionEffect(any());
        verify(player, never()).removePotionEffect(any());
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test void enderAddsThreeLevelsAndSixtySecondsToEachVanillaEffect() {
        activate("ender_totem", EquipmentSlot.OFF_HAND);
        pending().run();
        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2100, 4));
        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1300, 4));
        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 2000, 3));
        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
        verify(player, times(4)).addPotionEffect(any());
    }

    @Test void otherHandCannotGrantAnUnusedTotemsEffects() {
        ItemStack unused = totem("ender_totem");
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(unused);
        activate(null, EquipmentSlot.HAND);
        verifyNoInteractions(scheduler);
        verify(inventory, never()).getItem(EquipmentSlot.OFF_HAND);
    }

    @Test void cancelledResurrectionDoesNotScheduleEffects() {
        EntityResurrectEvent event = new EntityResurrectEvent(player, EquipmentSlot.HAND);
        event.setCancelled(true);
        listener.onResurrect(event);
        verifyNoInteractions(scheduler, inventory);
    }

    @Test void resurrectionWithoutAnItemDoesNotGrantEffects() {
        listener.onResurrect(new EntityResurrectEvent(player, null));
        verifyNoInteractions(scheduler, inventory);
    }

    @Test void offlineOrDeadPlayerDoesNotReceiveDelayedEffects() {
        activate("ender_totem", EquipmentSlot.HAND);
        Runnable task = pending();
        when(player.isOnline()).thenReturn(false);
        task.run();
        when(player.isOnline()).thenReturn(true);
        when(player.isDead()).thenReturn(true);
        task.run();
        verify(player, never()).addPotionEffect(any());
    }

    @Test void modelAndIdentityAreAssignedAndBothItemsAreInTheGiveCatalog() {
        for (ElementalTotems.Kind kind : ElementalTotems.Kind.values()) {
            ItemMeta meta = mock(ItemMeta.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(meta.getPersistentDataContainer()).thenReturn(data);
            try (MockedConstruction<ItemStack> construction = mockConstruction(ItemStack.class,
                    (stack, context) -> when(stack.getItemMeta()).thenReturn(meta))) {
                ElementalTotems.create(kind);
                verify(meta).setItemModel(NamespacedKey.minecraft(kind.id));
                verify(data).set(EndItems.ITEM_KEY, PersistentDataType.STRING, kind.id);
                verify(construction.constructed().getFirst()).setItemMeta(meta);
            }
            assertTrue(ItemManager.claves().contains(kind.id));
        }
    }

    @Test void matchingTagOnAnotherMaterialDoesNotActivateATotem() {
        ItemStack item = totem("sculk_totem");
        when(item.getType()).thenReturn(Material.STICK);
        assertNull(ElementalTotems.kindOf(item));
    }
}
