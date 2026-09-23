package items.IceBow;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class IceBowItem {
    private final JavaPlugin plugin;
    private final NamespacedKey iceBowKey;

    public IceBowItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iceBowKey = new NamespacedKey(plugin, "ice_bow");
    }

    public ItemStack createIceBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Arco de Hielo");

            meta.setCustomModelData(5);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.AQUA + "Un arco forjado con hielo eterno");
            lore.add(ChatColor.AQUA + "que congela a sus víctimas.");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Congela enemigos por " + ChatColor.AQUA + "10 segundos");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Cooldown de " + ChatColor.RED + "5 segundos");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Arma del Iceologer");
            meta.setLore(lore);

            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);

            meta.getPersistentDataContainer().set(iceBowKey, PersistentDataType.BYTE, (byte) 1);

            bow.setItemMeta(meta);
        }

        return bow;
    }

    public boolean isIceBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (meta.getPersistentDataContainer().has(iceBowKey, PersistentDataType.BYTE)) {
            return true;
        }

        return meta.hasCustomModelData() && meta.getCustomModelData() == 5;
    }

    public NamespacedKey getIceBowKey() {
        return iceBowKey;
    }
}