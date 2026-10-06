package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CorruptedMobItems {

    public static ItemStack createCorruptedMeet() {
        ItemStack meat = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = meat.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#8B008B") + "" + ChatColor.BOLD + "Carne Podrida Corrupta");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            ItemModels.apply(meta, "corrupted_meat");
            meat.setItemMeta(meta);
        }

        return meat;
    }

    public static ItemStack createCorruptedSpiderEye() {
        ItemStack spiderEye = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = spiderEye.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#8B008B") + "" + ChatColor.BOLD + "Ojo de Araña Corrupto");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            ItemModels.apply(meta, "corrupted_spider_eye");
            spiderEye.setItemMeta(meta);
        }

        return spiderEye;

    }
}
