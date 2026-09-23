package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class WardenCaveItems {

    private final JavaPlugin plugin;

    public WardenCaveItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createWardenEnergy() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Energía de Warden");
            meta.setCustomModelData(700);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Un eco cristalizado que aún vibra");
            lore.add(ChatColor.of("#008b8b") + "con la acústica de la oscuridad.");
            lore.add(ChatColor.of("#006666") + "Contiene la esencia pura de las");
            lore.add(ChatColor.of("#006666") + "profundidades de las Warden Caves.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createDeepFragment() {
        ItemStack item = new ItemStack(Material.COPPER_NUGGET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00cccc") + "" + ChatColor.BOLD + "Fragmento Profundo");
            meta.setCustomModelData(701);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#006666") + "Un pequeño remanente metálico arrancado");
            lore.add(ChatColor.of("#006666") + "de las entrañas del abismo.");
            lore.add(ChatColor.of("#004d4d") + "El Sculk ha comenzado a consumir");
            lore.add(ChatColor.of("#004d4d") + "su estructura natural.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.UNCOMMON);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createDeepRawOre() {
        ItemStack item = new ItemStack(Material.RAW_COPPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00b3b3") + "" + ChatColor.BOLD + "Mineral Crudo Profundo");
            meta.setCustomModelData(702);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Roca densa e infectada extraída de las");
            lore.add(ChatColor.of("#008b8b") + "paredes inexploradas de la dimensión.");
            lore.add(ChatColor.of("#004d4d") + "Su peso es anormal y parece susurrar");
            lore.add(ChatColor.of("#004d4d") + "cuando la sostienes en tus manos.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.RARE);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createDeepIngot() {
        ItemStack item = new ItemStack(Material.COPPER_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Lingote Profundo");
            meta.setCustomModelData(703);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Una aleación pesada forjada bajo la");
            lore.add(ChatColor.of("#008b8b") + "presión del vacío y la oscuridad extrema.");
            lore.add(ChatColor.of("#006666") + "Irradia un frío penetrante característico");
            lore.add(ChatColor.of("#006666") + "del subsuelo más profundo.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createWardenBossHeart() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Corazón de Warden Boss");
            meta.setCustomModelData(704);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Palpita con una furia incontrolable y");
            lore.add(ChatColor.of("#008b8b") + "una energía oscura inmensurable.");
            lore.add(ChatColor.of("#006666") + "El núcleo vital que alguna vez gobernó");
            lore.add(ChatColor.of("#006666") + "las temibles Warden Caves.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Se obtiene al derrotar al gigantesco");
            lore.add(ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Infested Warden Boss.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }
}