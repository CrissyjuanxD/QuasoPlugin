package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemsTotems {

    private final JavaPlugin plugin;

    public ItemsTotems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createSpecialTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            // Nombre
            meta.setDisplayName(ChatColor.of("#749cec") + "" + ChatColor.BOLD + "Totem Especial");

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#9172da") + "Este " + ChatColor.BOLD + "tótem" + ChatColor.of("#9172da") + " permite");
            lore.add(ChatColor.of("#9172da") + "mejorar un tótem normal");
            lore.add(ChatColor.of("#9172da") + "a uno con un " + ChatColor.BOLD + "poder oculto" + ChatColor.of("#9172da") + ".");
            meta.setLore(lore);

            // Propiedades base conservadas
            meta.setRarity(ItemRarity.RARE);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(Enchantment.KNOCKBACK, 2, true);

            totem.setItemMeta(meta);
        }

        return totem;
    }

    public static ItemStack createIceCrystal() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nombre
            meta.setDisplayName(ChatColor.of("#66ffff") + "" + ChatColor.BOLD + "Cristal de Hielo");

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#4b95c3") + "Este cristal lo dropean");
            lore.add(ChatColor.of("#4b95c3") + "los " + ChatColor.of("#8bd2e4") + ChatColor.BOLD + "Iceologers" + ChatColor.of("#4b95c3") + ".");
            lore.add("");
            lore.add(ChatColor.of("#4b95c3") + "Los " + ChatColor.of("#8bd2e4") + ChatColor.BOLD + "Iceologers" + ChatColor.of("#4b95c3") + " aparecen");
            lore.add(ChatColor.of("#4b95c3") + "durante las Raids.");
            meta.setLore(lore);

            // Propiedades base conservadas
            meta.setCustomModelData(100);
            meta.setRarity(ItemRarity.EPIC);

            item.setItemMeta(meta);
        }

        return item;
    }
}