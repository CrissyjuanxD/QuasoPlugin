package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

// Lo que sueltan los mobs florales: 16 de cada uno van en la Mejora Floral
public final class FloralMobItems {
    public static final NamespacedKey FLORAL_DROP = new NamespacedKey("quasoplugin", "floral_drop");

    private FloralMobItems() {}
    public static ItemStack createFlesh() {
        return create(Material.ROTTEN_FLESH, "carne_podrida_floral", "Carne Podrida Floral", "#ef94b6", "un Zombie Floral");
    }
    public static ItemStack createEye() {
        return create(Material.SPIDER_EYE, "ojo_arana_floral", "Ojo de Araña Floral", "#efbf62", "un Araña Floral");
    }
    public static ItemStack createGunpowder() {
        return create(Material.GUNPOWDER, "polvora_floral", "Pólvora Floral", "#a8c876", "un Creeper Floral");
    }
    public static ItemStack createStinger() {
        return create(Material.PRISMARINE_SHARD, "aguijon_floral", "Aguijón Floral", "#f7d36b", "una Mini Abeja Floral");
    }
    private static ItemStack create(Material material, String model, String name, String color, String mob) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(color) + "" + ChatColor.BOLD + name);
        // El lore queda igual que antes: si cambia, los drops viejos dejan de servir en la receta de la Mejora Floral
        meta.setLore(List.of("", ChatColor.GRAY + "Restos de " + mob + "."));
        meta.setCustomModelData(5); // Conserva la identificación anterior de carne y ojo.
        meta.setRarity(ItemRarity.EPIC);
        meta.setEnchantmentGlintOverride(false);
        meta.setItemModel(NamespacedKey.minecraft(model));
        meta.getPersistentDataContainer().set(FLORAL_DROP, PersistentDataType.STRING, model);
        item.setItemMeta(meta);
        return item;
    }

    // El id del drop floral (carne_podrida_floral, ojo_arana_floral, polvora_floral o aguijon_floral), o null
    public static String dropId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(FLORAL_DROP, PersistentDataType.STRING);
    }
}
