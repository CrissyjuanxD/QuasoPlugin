package items;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CustomPotions {

    // 1. Poción de Resistencia II por 10 minutos (Tomable)
    public static ItemStack getResistanceIIPotion() {
        return createPotion(
                Material.POTION,
                "§9§lPoción de Resistencia II",
                PotionEffectType.RESISTANCE,
                12000,
                1,
                Color.BLUE
        );
    }

    // 2. Poción de Resistencia III por 6 minutos (Lanzable)
    public static ItemStack getSplashResistanceIIIPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§9§lPoción de Resistencia III",
                PotionEffectType.RESISTANCE,
                7200,
                2,
                Color.BLUE
        );
    }

    // 3. Poción de Caída Lenta por 15 minutos (Tomable)
    public static ItemStack getSlowFallingPotion() {
        return createPotion(
                Material.POTION,
                "§7§lPoción de Caída Lenta",
                PotionEffectType.SLOW_FALLING,
                18000,
                0,
                Color.GRAY
        );
    }

    // 4. Poción de Regeneración III por 3 minutos (Lanzable)
    public static ItemStack getSplashRegenerationIIIPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§d§lPoción de Regeneración III",
                PotionEffectType.REGENERATION,
                3600,
                2,
                Color.FUCHSIA
        );
    }

    // 5. Poción de Prisa (Haste) III por 15 minutos (Tomable)
    public static ItemStack getHasteIIIPotion() {
        return createPotion(
                Material.POTION,
                "§e§lPoción de Prisa Minera III",
                PotionEffectType.HASTE,
                18000,
                2,
                Color.YELLOW
        );
    }

    // 6. Poción de Prisa (Haste) II por 15 minutos (Tomable)
    public static ItemStack getHasteIIPotion() {
        return createPotion(
                Material.POTION,
                "§e§lPoción de Prisa Minera II",
                PotionEffectType.HASTE,
                18000,
                1,
                Color.YELLOW
        );
    }

    // 7. Poción de Absorción X por 3 minutos (Lanzable)
    public static ItemStack getSplashAbsorptionXPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§6§lPoción de Absorción X",
                PotionEffectType.ABSORPTION,
                3600,
                9,
                Color.ORANGE
        );
    }

    public static ItemStack getSpeedHoneyBottle() {
        ItemStack honey = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = honey.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lFrasco de Velocidad");

            List<String> lore = new ArrayList<>();
            lore.add("§9Velocidad IV (6:00)");
            meta.setLore(lore);

            meta.setCustomModelData(8001);

            honey.setItemMeta(meta);
        }

        return honey;
    }

    /**
     * Método constructor base para no repetir código
     * * @param material   Material.POTION (Tomable) o Material.SPLASH_POTION (Lanzable)
     * @param name       Nombre customizado con códigos de color
     * @param effectType Tipo de efecto de poción
     * @param duration   Duración en Ticks (Segundos * 20)
     * @param amplifier  Amplificador (Nivel real - 1)
     * @param color      Color del líquido de la poción
     * @return ItemStack configurado
     */
    private static ItemStack createPotion(Material material, String name, PotionEffectType effectType, int duration, int amplifier, Color color) {
        ItemStack potion = new ItemStack(material);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            meta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);

            meta.setColor(color);

            potion.setItemMeta(meta);
        }

        return potion;
    }
}