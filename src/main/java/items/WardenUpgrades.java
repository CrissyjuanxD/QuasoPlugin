package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class WardenUpgrades {
    private final JavaPlugin plugin;
    private final NamespacedKey WARDEN_UPGRADE_KEY;

    public WardenUpgrades(JavaPlugin plugin) {
        this.plugin = plugin;
        this.WARDEN_UPGRADE_KEY = new NamespacedKey(plugin, "warden_upgrade");
    }

    public ItemStack createHelmetWardenUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00b3b3") + "" + ChatColor.BOLD + "Mejora de Casco Warden");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#006666") + "Plantilla de herrería necesaria");
            lore.add(ChatColor.of("#006666") + "para forjar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#00ffff") + ChatColor.BOLD + "Casco de Warden");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(400);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(WARDEN_UPGRADE_KEY, PersistentDataType.STRING, "helmet");

            ItemModels.apply(meta, "mejora_casco_warden");
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createChestplateWardenUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00b3b3") + "" + ChatColor.BOLD + "Mejora de Peto Warden");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#006666") + "Plantilla de herrería necesaria");
            lore.add(ChatColor.of("#006666") + "para forjar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#00ffff") + ChatColor.BOLD + "Peto de Warden");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(405);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(WARDEN_UPGRADE_KEY, PersistentDataType.STRING, "chestplate");

            ItemModels.apply(meta, "mejora_peto_warden");
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createLeggingsWardenUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00b3b3") + "" + ChatColor.BOLD + "Mejora de Pantalón Warden");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#006666") + "Plantilla de herrería necesaria");
            lore.add(ChatColor.of("#006666") + "para forjar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#00ffff") + ChatColor.BOLD + "Pantalón de Warden");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(410);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(WARDEN_UPGRADE_KEY, PersistentDataType.STRING, "leggings");

            ItemModels.apply(meta, "mejora_pantalon_warden");
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createBootsWardenUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00b3b3") + "" + ChatColor.BOLD + "Mejora de Botas Warden");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#006666") + "Plantilla de herrería necesaria");
            lore.add(ChatColor.of("#006666") + "para forjar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#00ffff") + ChatColor.BOLD + "Botas de Warden");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(415);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(WARDEN_UPGRADE_KEY, PersistentDataType.STRING, "boots");

            ItemModels.apply(meta, "mejora_bota_warden");
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public NamespacedKey getWardenUpgradeKey() {
        return WARDEN_UPGRADE_KEY;
    }
}