package items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CurrencyAppearanceTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    @Test void oldCoinsLoseTheirFakeEnchantWithoutReplacingMetadataOrChangingAmounts() {
        for (Material material : new Material[]{Material.SUNFLOWER, Material.GOLD_NUGGET}) {
            ItemStack item=mock(ItemStack.class);
            ItemMeta meta=mock(ItemMeta.class);
            when(item.getType()).thenReturn(material);
            when(item.hasItemMeta()).thenReturn(true);
            when(item.getItemMeta()).thenReturn(meta);
            when(meta.hasCustomModelData()).thenReturn(true);
            when(meta.getCustomModelData()).thenReturn(material==Material.SUNFLOWER?2000:2010);
            assertTrue(CurrencyAppearance.refresh(item));
            verify(meta).setEnchantmentGlintOverride(false);
            verify(meta).removeEnchant(Enchantment.UNBREAKING);
            verify(meta).setDisplayName(contains(material==Material.SUNFLOWER?"DinoCoins":"DinoFichas"));
            verify(item).setItemMeta(meta);
            verify(item,never()).setAmount(anyInt());
            verify(meta,never()).setLore(anyList());
            verify(meta,never()).setItemModel(any());
        }
    }

    @Test void missionTokenKeepsItsMissionNumberAndLabel() {
        ItemStack item=mock(ItemStack.class); ItemMeta meta=mock(ItemMeta.class);
        PersistentDataContainer data=mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.POPPED_CHORUS_FRUIT);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(new NamespacedKey("quasoplugin","ficha_mision"),PersistentDataType.INTEGER)).thenReturn(106);
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn("§6Ficha de Misión Extra #6");
        assertTrue(CurrencyAppearance.refresh(item));
        verify(meta).setDisplayName(contains("Extra #6"));
        verify(meta).setEnchantmentGlintOverride(false);
        verify(data,never()).set(any(),any(),any());
        verify(item,never()).setAmount(anyInt());
    }
}
