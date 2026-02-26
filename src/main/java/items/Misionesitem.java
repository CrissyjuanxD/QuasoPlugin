package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Misionesitem {

    public static ItemStack createMisiones() {
        ItemStack book = new ItemStack(Material.FILLED_MAP);
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
        meta.setCustomModelData(9999);

        book.setItemMeta(meta);
        return book;
    }
}
