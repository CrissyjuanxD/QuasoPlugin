package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public final class FloralMobItems {
    private FloralMobItems() {}
    public static ItemStack createFlesh() {
        return create(Material.ROTTEN_FLESH, "carne_podrida_floral", "Carne Podrida Floral", "#ef94b6", "Zombie Floral");
    }
    public static ItemStack createEye() {
        return create(Material.SPIDER_EYE, "ojo_arana_floral", "Ojo de Araña Floral", "#efbf62", "Araña Floral");
    }
    public static ItemStack createGunpowder() {
        return create(Material.GUNPOWDER, "polvora_floral", "Pólvora Floral", "#a8c876", "Creeper Floral");
    }
    private static ItemStack create(Material material, String model, String name, String color, String mob) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(color) + "" + ChatColor.BOLD + name);
        meta.setLore(List.of("", ChatColor.GRAY + "Restos de un " + mob + "."));
        meta.setCustomModelData(5); // Conserva la identificación anterior de carne y ojo.
        meta.setRarity(ItemRarity.EPIC);
        meta.setEnchantmentGlintOverride(false);
        meta.setItemModel(NamespacedKey.minecraft(model));
        meta.getPersistentDataContainer().set(new NamespacedKey("quasoplugin", "floral_drop"), PersistentDataType.STRING, model);
        item.setItemMeta(meta);
        return item;
    }
}
