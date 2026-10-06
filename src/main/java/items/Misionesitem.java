package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Misionesitem {
    private static final NamespacedKey KEY = new NamespacedKey("quasoplugin", "libro_misiones");
    private static final int LEGACY_MODEL_DATA = 9999;

    public static ItemStack createMisiones() {
        ItemStack book = new ItemStack(Material.MAP);
        ItemMeta meta = book.getItemMeta();

        meta.setDisplayName(ChatColor.of("#FF8000") + "" + ChatColor.BOLD + "Misiones");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#c88310") + "Este pergamino contiene");
        lore.add(ChatColor.of("#c88310") + "las misiones diarias");
        lore.add("");
        lore.add(ChatColor.GRAY + "Click derecho para abrir");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("map"));

        book.setItemMeta(meta);
        return book;
    }

    // Los que se dieron antes de la 26.2 solo tienen el custom model data 9999
    @SuppressWarnings("deprecation")
    public static boolean isMisiones(ItemStack item) {
        if (item == null || item.getType() != Material.MAP || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE)) return true;
        return meta.hasCustomModelData() && meta.getCustomModelData() == LEGACY_MODEL_DATA;
    }
}
