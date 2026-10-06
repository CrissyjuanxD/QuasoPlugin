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

public class InfestedSoulsItems {

    private final JavaPlugin plugin;

    public InfestedSoulsItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createInfestedSkeletonSoul() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Alma de Infested Skeletons");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Esencia corrompida necesaria");
            lore.add(ChatColor.of("#008b8b") + "para forjar partes de armadura.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(201);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("alma_infested_skeleton"));
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createInfestedGhastSoul() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Alma de Infested Ghast");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Esencia corrompida necesaria");
            lore.add(ChatColor.of("#008b8b") + "para forjar partes de armadura.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(202);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("alma_infested_ghast"));
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createInfestedCreeperSoul() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Alma de Infested Creeper");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Esencia corrompida necesaria");
            lore.add(ChatColor.of("#008b8b") + "para forjar partes de armadura.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(203);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("alma_infested_creeper"));
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createInfestedCaveSpiderSoul() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Alma de Infested Cave Spider");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Esencia corrompida necesaria");
            lore.add(ChatColor.of("#008b8b") + "para forjar partes de armadura.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(204);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("alma_infested_cave_spider"));
            essence.setItemMeta(meta);
        }
        return essence;
    }
}