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

public class WardenCaveItems {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey("quasoplugin", "warden_cave_item");

    private final JavaPlugin plugin;

    public WardenCaveItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Cada bioma de la Warden Cave tiene su mineral y su fragmento, y cada uno va en la mejora de una pieza
    public enum Variant {
        CIAN("Cian", "cian", "#3ad4d4", "de la Caverna Sculk", "Casco", "prismarine_shard", "prismarine_crystals", 705, 709),
        VERDE("Verde", "verde", "#5fd16a", "del Pantano Profundo", "Botas", "emerald", "turtle_scute", 706, 710),
        MORADO("Morado", "morado", "#b46be0", "del Abismo Flotante", "Peto", "amethyst_shard", "chorus_fruit", 707, 711),
        GRIS("Gris", "gris", "#a7a7b0", "de las Ruinas de Ceniza", "Pantalón", "flint", "iron_nugget", 708, 712);

        private final String displayName;
        private final String id;
        private final String color;
        private final String biome;
        private final String piece;
        private final String rawModel;
        private final String fragmentModel;
        private final int rawModelData;
        private final int fragmentModelData;

        Variant(String displayName, String id, String color, String biome, String piece,
                String rawModel, String fragmentModel, int rawModelData, int fragmentModelData) {
            this.displayName = displayName;
            this.id = id;
            this.color = color;
            this.biome = biome;
            this.piece = piece;
            this.rawModel = rawModel;
            this.fragmentModel = fragmentModel;
            this.rawModelData = rawModelData;
            this.fragmentModelData = fragmentModelData;
        }

        public String displayName() {
            return displayName;
        }

        public String rawId() {
            return "mineral_crudo_" + id;
        }

        public String fragmentId() {
            return "fragmento_profundo_" + id;
        }
    }

    // Marca el item con su id para reconocerlo aunque cambien el nombre o el lore
    private static void mark(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
    }

    public static boolean isWardenCaveItem(ItemStack item) {
        return idOf(item) != null;
    }

    public static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    // Por debajo es un fragmento de prismarina (no se funde en ningún horno vanilla) con otra textura;
    // así solo entra en la receta del alto horno de la etapa 2
    public static ItemStack createRawOre(Variant variant) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(variant.color) + "" + ChatColor.BOLD + "Mineral Crudo " + variant.displayName);
            meta.setCustomModelData(variant.rawModelData);
            meta.setItemModel(NamespacedKey.minecraft(variant.rawModel));
            mark(meta, variant.rawId());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Roca infectada sacada de las vetas");
            lore.add(ChatColor.of(variant.color) + variant.biome + ChatColor.GRAY + ".");
            lore.add("");
            lore.add(ChatColor.GRAY + "Se funde en el " + ChatColor.WHITE + "alto horno" + ChatColor.GRAY + " (4 minutos).");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.UNCOMMON);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createFragment(Variant variant) {
        ItemStack item = new ItemStack(Material.COPPER_NUGGET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(variant.color) + "" + ChatColor.BOLD + "Fragmento Profundo " + variant.displayName);
            meta.setCustomModelData(variant.fragmentModelData);
            meta.setItemModel(NamespacedKey.minecraft(variant.fragmentModel));
            mark(meta, variant.fragmentId());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Fundido de las vetas");
            lore.add(ChatColor.of(variant.color) + variant.biome + ChatColor.GRAY + ".");
            lore.add("");
            lore.add(ChatColor.GRAY + "Va en los " + ChatColor.WHITE + "Lingotes Profundos" + ChatColor.GRAY + " y en la mejora de:");
            lore.add(ChatColor.of(variant.color) + "> " + variant.piece + " de Warden");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.RARE);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<ItemStack> allFragments() {
        List<ItemStack> fragments = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            fragments.add(createFragment(variant));
        }
        return fragments;
    }

    public static ItemStack createWardenEnergy() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Energía de Warden");
            meta.setCustomModelData(700);
            mark(meta, "energia_warden");

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

    public static ItemStack createDeepIngot() {
        ItemStack item = new ItemStack(Material.COPPER_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Lingote Profundo");
            meta.setCustomModelData(703);
            mark(meta, "lingote_profundo");

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
            mark(meta, "corazon_warden_boss");

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
