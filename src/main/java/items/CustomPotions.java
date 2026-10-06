package items;

import org.bukkit.NamespacedKey;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomPotions {

    public static ItemStack getResistanceIIPotion() {
        return createPotion("potion_resistance_2",
                Material.POTION,
                "§9§lPoción de Resistencia II",
                PotionEffectType.RESISTANCE,
                12000,
                1,
                Color.BLUE
        );
    }

    public static ItemStack getSplashResistanceIIIPotion() {
        return createPotion("splash_resistance_3",
                Material.SPLASH_POTION,
                "§9§lPoción de Resistencia III",
                PotionEffectType.RESISTANCE,
                7200,
                2,
                Color.BLUE
        );
    }

    public static ItemStack getSlowFallingPotion() {
        return createPotion("potion_slow_falling",
                Material.POTION,
                "§7§lPoción de Caída Lenta",
                PotionEffectType.SLOW_FALLING,
                18000,
                0,
                Color.GRAY
        );
    }

    public static ItemStack getSplashRegenerationIIIPotion() {
        return createPotion("splash_regeneration_3",
                Material.SPLASH_POTION,
                "§d§lPoción de Regeneración III",
                PotionEffectType.REGENERATION,
                3600,
                2,
                Color.FUCHSIA
        );
    }

    public static ItemStack getHasteIIIPotion() {
        return createPotion("potion_haste_3",
                Material.POTION,
                "§e§lPoción de Prisa Minera III",
                PotionEffectType.HASTE,
                18000,
                2,
                Color.YELLOW
        );
    }

    public static ItemStack getHasteIIPotion() {
        return createPotion("potion_haste_2",
                Material.POTION,
                "§e§lPoción de Prisa Minera II",
                PotionEffectType.HASTE,
                18000,
                1,
                Color.YELLOW
        );
    }

    public static ItemStack getSplashAbsorptionXPotion() {
        return createPotion("splash_absorption_10",
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

            meta.setItemModel(NamespacedKey.minecraft("frasco_de_velocidad"));
            honey.setItemMeta(meta);
        }

        return honey;
    }

    public static ItemStack getTequila() {
        return createDrink("bar_tequila", "§6§lCaballito de Tequila", Color.fromRGB(220, 180, 50),
                new PotionEffect(PotionEffectType.NAUSEA, 200, 1),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 240, 1)
        );
    }

    public static ItemStack getMargarita() {
        return createDrink("bar_margarita", "§a§lMargarita de Limón", Color.fromRGB(150, 255, 100),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 1),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1)
        );
    }

    public static ItemStack getMezcal() {
        return createDrink("bar_mezcal", "§8§lTrago de Mezcal", Color.fromRGB(200, 200, 200),
                new PotionEffect(PotionEffectType.DARKNESS, 200, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 300, 2),
                new PotionEffect(PotionEffectType.SATURATION, 240, 1)
        );
    }

    public static ItemStack getPulque() {
        return createDrink("bar_pulque", "§f§lJarrito de Pulque", Color.fromRGB(255, 245, 230),
                new PotionEffect(PotionEffectType.DARKNESS, 300, 1),
                new PotionEffect(PotionEffectType.SATURATION, 300, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 2)
        );
    }

    public static ItemStack getBeer() {
        return createDrink("bar_cerveza", "§c§lJarra de Cerveza", Color.fromRGB(102, 51, 0),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 1)
        );
    }

    public static ItemStack getRum() {
        return createDrink("bar_ron", "§4§lRon Añejo", Color.fromRGB(139, 69, 19),
                new PotionEffect(PotionEffectType.NAUSEA, 300, 2),
                new PotionEffect(PotionEffectType.DARKNESS, 240, 1)
        );
    }

    public static ItemStack getVodka() {
        return createDrink("bar_vodka", "§b§lVaso de Vodka", Color.fromRGB(220, 240, 255),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 1)
        );
    }

    public static ItemStack getWhisky() {
        return createDrink("bar_whisky", "§e§lVaso de Whisky", Color.fromRGB(205, 133, 63),
                new PotionEffect(PotionEffectType.SLOWNESS, 240, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 240, 2),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1)
        );
    }

    public static ItemStack getSake() {
        return createDrink("bar_sake", "§f§lVasito de Sake", Color.fromRGB(245, 255, 255),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 300, 1),
                new PotionEffect(PotionEffectType.DARKNESS, 200, 1)
        );
    }

    public static ItemStack getGin() {
        return createDrink("bar_ginebra", "§3§lCopa de Ginebra", Color.fromRGB(190, 255, 240),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 300, 1),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2)
        );
    }

    public static ItemStack getAzulito() {
        return createDrink("bar_azulito", "§b§lAzulito", Color.fromRGB(0, 200, 255),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1),
                new PotionEffect(PotionEffectType.SATURATION, 240, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 200, 1)
        );
    }

    public static ItemStack getMichelada() {
        return createDrink("bar_michelada", "§4§lVaso de Michelada", Color.fromRGB(150, 30, 0),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 1),
                new PotionEffect(PotionEffectType.SATURATION, 240, 1)
        );
    }

    private static ItemStack createPotion(String modelKey, Material material, String name, PotionEffectType effectType, int duration, int amplifier, Color color) {
        ItemStack potion = new ItemStack(material);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
            meta.setColor(color);
            meta.setItemModel(NamespacedKey.minecraft(modelKey));
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private static ItemStack createDrink(String modelKey, String name, Color color, PotionEffect... effects) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Esta bebida te otorga estos");
            lore.add(ChatColor.of("#ffcc99") + "efectos" + ChatColor.GRAY + ":");
            lore.add("");

            for (PotionEffect effect : effects) {
                meta.addCustomEffect(effect, true);

                String effectName = effect.getType().getKey().getKey().toUpperCase(Locale.ROOT);
                String hexColor = "#FFFFFF";

                if (effectName.equals("NAUSEA")) {
                    effectName = "Náuseas"; hexColor = "#99cc33";
                } else if (effectName.equals("SATURATION")) {
                    effectName = "Saturación"; hexColor = "#cc3300";
                } else if (effectName.equals("MINING_FATIGUE")) {
                    effectName = "Fatiga Minera"; hexColor = "#8B4513";
                } else if (effectName.equals("NIGHT_VISION")) {
                    effectName = "Visión Nocturna"; hexColor = "#1E90FF";
                } else if (effectName.equals("DARKNESS")) {
                    effectName = "Oscuridad"; hexColor = "#4B0082";
                } else if (effectName.equals("SLOWNESS")) {
                    effectName = "Lentitud"; hexColor = "#FFA500";
                }

                int level = effect.getAmplifier() + 1;
                int seconds = effect.getDuration() / 20;

                lore.add(ChatColor.GRAY + "> " + ChatColor.of(hexColor) + effectName + " " + level +
                        ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + seconds + " s" + ChatColor.GRAY + ")");
            }

            lore.add("");
            meta.setLore(lore);
            meta.setColor(color);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (Exception ignored) {}

            meta.setItemModel(NamespacedKey.minecraft(modelKey));
            potion.setItemMeta(meta);
        }
        return potion;
    }
}
