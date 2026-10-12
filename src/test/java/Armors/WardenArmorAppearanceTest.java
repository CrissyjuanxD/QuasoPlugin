package Armors;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import items.EndItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WardenArmorAppearanceTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    private record Fixture(WardenArmor armor, ItemStack item, ItemMeta meta,
                           PersistentDataContainer data, EquippableComponent equipment,
                           AtomicReference<NamespacedKey> model) {}

    private Fixture oldPiece(Material material, EquipmentSlot slot, boolean winged, boolean tagged) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("QuasoPlugin");
        when(plugin.namespace()).thenReturn("quasoplugin");
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        EquippableComponent equipment = mock(EquippableComponent.class);
        AtomicReference<NamespacedKey> model = new AtomicReference<>(NamespacedKey.minecraft("netherite"));
        when(item.getType()).thenReturn(material);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.has(new NamespacedKey(plugin, "warden_armor"), PersistentDataType.BYTE)).thenReturn(tagged);
        when(data.get(EndItems.ITEM_KEY, PersistentDataType.STRING)).thenReturn(winged ? "peto_warden_alado" : null);
        when(meta.hasEquippable()).thenReturn(true);
        when(meta.getEquippable()).thenReturn(equipment);
        when(equipment.getSlot()).thenReturn(slot);
        when(equipment.getModel()).thenAnswer(call -> model.get());
        doAnswer(call -> { model.set(call.getArgument(0)); return null; }).when(equipment).setModel(any());
        return new Fixture(new WardenArmor(plugin), item, meta, data, equipment, model);
    }

    @Test void migratesEveryOldArmorSlotOnceWithoutReplacingItsOtherMetadata() {
        Map<Material, EquipmentSlot> slots = Map.of(
                Material.NETHERITE_HELMET, EquipmentSlot.HEAD,
                Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST,
                Material.NETHERITE_LEGGINGS, EquipmentSlot.LEGS,
                Material.NETHERITE_BOOTS, EquipmentSlot.FEET);
        slots.forEach((material, slot) -> {
            Fixture f = oldPiece(material, slot, false, true);
            assertTrue(f.armor.refreshEquipmentModel(f.item));
            assertEquals(NamespacedKey.minecraft("warden"), f.model.get());
            assertFalse(f.armor.refreshEquipmentModel(f.item));
            verify(f.item, times(1)).setItemMeta(f.meta);
            verify(f.meta, never()).setAttributeModifiers(any());
            verify(f.meta, never()).setItemModel(any());
            verify(f.meta, never()).setLore(anyList());
            verify(f.meta, never()).setDisplayName(anyString());
            verify(f.meta, never()).setGlider(anyBoolean());
            verify(f.equipment, never()).setEquipSound(any());
            verify(f.data, never()).set(any(), any(), any());
            verify(f.item, never()).setAmount(anyInt());
        });
    }

    @Test void wingedChestplateUsesTheWingedAssetWithoutChangingItsGlidingFlag() {
        Fixture f = oldPiece(Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, true, true);
        assertTrue(f.armor.refreshEquipmentModel(f.item));
        assertEquals(NamespacedKey.minecraft("warden_alado"), f.model.get());
        verify(f.meta, never()).setGlider(anyBoolean());
        assertFalse(f.armor.refreshEquipmentModel(f.item));
    }

    @Test void ordinaryNetheriteAndUnsupportedTaggedItemsStayUnchanged() {
        Fixture ordinary = oldPiece(Material.NETHERITE_HELMET, EquipmentSlot.HEAD, false, false);
        Fixture unsupported = oldPiece(Material.DIAMOND_HELMET, EquipmentSlot.HEAD, false, true);
        for (Fixture f : new Fixture[]{ordinary, unsupported}) {
            assertFalse(f.armor.refreshEquipmentModel(f.item));
            verify(f.item, never()).setItemMeta(any());
            verifyNoInteractions(f.equipment);
        }
        assertFalse(ordinary.armor.refreshEquipmentModel(null));
    }

    @Test void equipmentEventWritesTheUpdatedItemBackOnlyOnce() {
        Fixture f = oldPiece(Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, true, true);
        LivingEntity entity = mock(LivingEntity.class);
        EntityEquipment inventory = mock(EntityEquipment.class);
        when(entity.getEquipment()).thenReturn(inventory);
        when(inventory.getItem(EquipmentSlot.CHEST)).thenReturn(f.item);
        EntityEquipmentChangedEvent event = new EntityEquipmentChangedEvent(entity,
                Map.of(EquipmentSlot.CHEST, mock(EntityEquipmentChangedEvent.EquipmentChange.class)));
        f.armor.onEquipmentChanged(event);
        f.armor.onEquipmentChanged(event);
        verify(inventory, times(1)).setItem(EquipmentSlot.CHEST, f.item);
    }
}
