package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class EconomyItems {

    public static ItemStack createVithiumCoin() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#FFCC80") + "DinoCoins " + ChatColor.GRAY + "۞");
        meta.setCustomModelData(2000);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Moneda oficial del servidor.");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "> Cambiala por DinoFichas en el spawn.");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createVithiumToken() {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#3a86b6") + "DinoFichas " + ChatColor.GRAY + "۞");
        meta.setCustomModelData(2010);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Fichas que se utilizan para");
        lore.add(ChatColor.GRAY + "el " + ChatColor.YELLOW + "casino" + ChatColor.GRAY + ".");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBloodFragment() {
        ItemStack item = new ItemStack(Material.COPPER_NUGGET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#d46868") + "Fragmento de " + ChatColor.of("#d46868") + ChatColor.BOLD + "BloodMoon");
        meta.setCustomModelData(2060);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#b86b42") + "Este fragmento lo puedes");
        lore.add(ChatColor.of("#b86b42") + "cambiar por " + ChatColor.of("#ffcc80") + "DinoCoins " + ChatColor.GRAY + "۞");
        lore.add(ChatColor.of("#b86b42") + "en la tienda de Monedas.");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true); // Brillo escondido
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNormalMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Mochila Nivel 1");
        meta.setCustomModelData(2020);
        meta.setItemModel(NamespacedKey.minecraft("lime_bundle"));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGreenMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Mochila Nivel 2");
        meta.setCustomModelData(2021);
        meta.setItemModel(NamespacedKey.minecraft("blue_bundle"));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRedMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Mochila Nivel 3");
        meta.setCustomModelData(2022);
        meta.setItemModel(NamespacedKey.minecraft("orange_bundle"));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBlueMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Mochila Nivel 4");
        meta.setCustomModelData(2023);
        meta.setItemModel(NamespacedKey.minecraft("red_bundle"));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPurpleMochila() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Mochila Nivel 5");
        meta.setCustomModelData(2024);
        meta.setItemModel(NamespacedKey.minecraft("purple_bundle"));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack createEnderBag() {
        ItemStack item = new ItemStack(Material.ENDERMITE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#cc99cc") + "" + ChatColor.BOLD + "Ender Bag");
        meta.setCustomModelData(2030);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Abre el inventario del");
        lore.add(ChatColor.LIGHT_PURPLE + "Ender Chest" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGancho() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ffffcc") + "" + ChatColor.BOLD + "Gancho");
        meta.setCustomModelData(10);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#66cc99") + "Hace que te desplaces");
        lore.add(ChatColor.of("#66cc99") + "más rápido" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createManzanaPanico() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ff6666") + "" + ChatColor.BOLD + "Manzana del Pánico");
        meta.setCustomModelData(10);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#ffcc99") + "Esta manzana te otorga estos");
        lore.add(ChatColor.of("#ffcc99") + "efectos por 5 segundos" + ChatColor.GRAY + ":");
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffff66") + "Absorción 5");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc99cc") + "Regeneración 3");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createYunqueReparadorNivel1() {
        ItemStack item = new ItemStack(Material.SKELETON_HORSE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#99ccff") + "" + ChatColor.BOLD + "Artefacto de Hierro Reparador");
        meta.setCustomModelData(2040);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#eeeeee") + "Repara un 25% la armadura");
        lore.add(ChatColor.of("#eeeeee") + "equipada" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createYunqueReparadorNivel2() {
        ItemStack item = new ItemStack(Material.BLAZE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ffcc00") + "" + ChatColor.BOLD + "Artefacto de Oro Reparador");
        meta.setCustomModelData(2050);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.of("#ff9999") + "Repara un 100% la armadura");
        lore.add(ChatColor.of("#ff9999") + "equipada" + ChatColor.GRAY + ".");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Uso:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMaterialMochila(org.bukkit.Material material) {
        return material == Material.ECHO_SHARD;
    }

    public static void applyPanicAppleEffects(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                100,
                4,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                100,
                2,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION,
                100,
                0,
                false,
                false
        ));
    }
}