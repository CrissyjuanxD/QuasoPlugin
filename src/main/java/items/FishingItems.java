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

public class FishingItems {

    private static final int CMD_CHATARRA            = 1000;
    private static final int CMD_MANZANA_PODRIDA     = 1001;
    private static final int CMD_ZANAHORIA_ENCANTADA = 1002;
    private static final int CMD_PEPITAS_OXIDADAS    = 1003;
    private static final int CMD_PEPITAS_DIAMANTE    = 1004;
    private static final int CMD_FRAGMENTOS_AMBAR    = 1005;
    private static final int CMD_FOSILES_PEQUENOS    = 1006;
    private static final int CMD_LINGOTE_PLATINO     = 1007;

    public static ItemStack createChatarra() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#A87F5A") + "" + ChatColor.BOLD + "Chatarra");
            meta.setCustomModelData(CMD_CHATARRA);
            meta.setRarity(ItemRarity.COMMON);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#8C6B4A") + "Restos oxidados arrastrados");
            lore.add(ChatColor.of("#8C6B4A") + "por la corriente del río.");
            lore.add(ChatColor.of("#6B4F33") + "No vale mucho, pero algo es algo.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Común");
            meta.setLore(lore);
            ItemModels.apply(meta, "chatarra");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createManzanaPodrida() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#7A3B1E") + "" + ChatColor.BOLD + "Manzana Podrida");
            meta.setCustomModelData(CMD_MANZANA_PODRIDA);
            meta.setRarity(ItemRarity.COMMON);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#6B3018") + "Una manzana que lleva demasiado");
            lore.add(ChatColor.of("#6B3018") + "tiempo bajo el agua. Huele terrible.");
            lore.add(ChatColor.of("#4A1F0F") + "Quizás alguien la quiera cambiar.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Común");
            meta.setLore(lore);
            ItemModels.apply(meta, "manzana_podrida");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createZanahoriaEncantada() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#5B8EC4") + "" + ChatColor.BOLD + "Zanahoria Encantada");
            meta.setCustomModelData(CMD_ZANAHORIA_ENCANTADA);
            meta.setRarity(ItemRarity.UNCOMMON);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#4A7BAD") + "Brilla con una energía misteriosa.");
            lore.add(ChatColor.of("#4A7BAD") + "Fue arrastrada al fondo del lago");
            lore.add(ChatColor.of("#3A6090") + "hace mucho tiempo.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Poco común");
            meta.setLore(lore);
            ItemModels.apply(meta, "zanahoria_encantada");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createPepitasHierroOxidadas() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#9C7B5C") + "" + ChatColor.BOLD + "Pepitas de Hierro Oxidadas");
            meta.setCustomModelData(CMD_PEPITAS_OXIDADAS);
            meta.setRarity(ItemRarity.UNCOMMON);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#8A6B4A") + "Pequeñas pepitas corroídas por");
            lore.add(ChatColor.of("#8A6B4A") + "siglos de humedad y sal.");
            lore.add(ChatColor.of("#5C4030") + "Aún conservan algo de valor");
            lore.add(ChatColor.of("#5C4030") + "para los comerciantes.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Poco común");
            meta.setLore(lore);
            ItemModels.apply(meta, "pepitas_hierro_oxidadas");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createPepitasDiamante() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#62B8D4") + "" + ChatColor.BOLD + "Pepitas de Diamante");
            meta.setCustomModelData(CMD_PEPITAS_DIAMANTE);
            meta.setRarity(ItemRarity.RARE);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#4FAFC8") + "Fragmentos cristalinos que reflejan");
            lore.add(ChatColor.of("#4FAFC8") + "la luz del sol con un destello azul.");
            lore.add(ChatColor.of("#3A8FB0") + "Altamente codiciadas en la tienda.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Raro");
            meta.setLore(lore);
            ItemModels.apply(meta, "pepitas_diamante");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createFragmentosAmbar() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#D4900A") + "" + ChatColor.BOLD + "Fragmentos de Ámbar");
            meta.setCustomModelData(CMD_FRAGMENTOS_AMBAR);
            meta.setRarity(ItemRarity.RARE);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#C07800") + "Resina fosilizada atrapada en el");
            lore.add(ChatColor.of("#C07800") + "lecho del río durante milenios.");
            lore.add(ChatColor.of("#A06000") + "A veces contiene pequeños insectos");
            lore.add(ChatColor.of("#A06000") + "perfectamente conservados.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Raro");
            meta.setLore(lore);
            ItemModels.apply(meta, "fragmentos_ambar");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createFosilesP() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#B09070") + "" + ChatColor.BOLD + "Fósiles Pequeños");
            meta.setCustomModelData(CMD_FOSILES_PEQUENOS);
            meta.setRarity(ItemRarity.EPIC);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#9A7A5A") + "Huesos petrificados de criaturas");
            lore.add(ChatColor.of("#9A7A5A") + "que habitaron estas aguas hace eras.");
            lore.add(ChatColor.of("#7A5A3A") + "Los arqueólogos pagarían bien");
            lore.add(ChatColor.of("#7A5A3A") + "por algo así.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Épico");
            meta.setLore(lore);
            ItemModels.apply(meta, "fosiles_pequenos");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLingotePlatino() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#7EB8D4") + "" + ChatColor.BOLD + "Lingote de Platino");
            meta.setCustomModelData(CMD_LINGOTE_PLATINO);
            meta.setRarity(ItemRarity.EPIC);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#5FA0BF") + "Un lingote de metal plateado");
            lore.add(ChatColor.of("#5FA0BF") + "de origen desconocido, con un brillo");
            lore.add(ChatColor.of("#4080A0") + "que no se apaga ni bajo el agua.");
            lore.add(ChatColor.of("#4080A0") + "Extremadamente valioso.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Loot de pesca · Épico");
            meta.setLore(lore);
            ItemModels.apply(meta, "lingote_platino");
            item.setItemMeta(meta);
        }
        return item;
    }
}