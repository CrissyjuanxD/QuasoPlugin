package items;

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

    // =========================================
    //         BEBIDAS ALCOHÓLICAS (ROLEPLAY)
    // =========================================

    public static ItemStack getTequila() {
        return createDrink("§6§lCaballito de Tequila", Color.fromRGB(220, 180, 50),
                new PotionEffect(PotionEffectType.NAUSEA, 200, 1),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 240, 1)
        );
    }

    public static ItemStack getMargarita() {
        return createDrink("§a§lMargarita de Limón", Color.fromRGB(150, 255, 100),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 1),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1)
        );
    }

    public static ItemStack getMezcal() {
        return createDrink("§8§lTrago de Mezcal", Color.fromRGB(200, 200, 200),
                new PotionEffect(PotionEffectType.DARKNESS, 200, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 300, 2),
                new PotionEffect(PotionEffectType.SATURATION, 240, 1)
        );
    }

    public static ItemStack getPulque() {
        return createDrink("§f§lJarrito de Pulque", Color.fromRGB(255, 245, 230),
                new PotionEffect(PotionEffectType.DARKNESS, 300, 1),
                new PotionEffect(PotionEffectType.SATURATION, 300, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 2)
        );
    }

    public static ItemStack getBeer() {
        return createDrink("§c§lJarra de Cerveza", Color.fromRGB(102, 51, 0),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 1)
        );
    }

    public static ItemStack getRum() {
        return createDrink("§4§lRon Añejo", Color.fromRGB(139, 69, 19),
                new PotionEffect(PotionEffectType.NAUSEA, 300, 2),
                new PotionEffect(PotionEffectType.DARKNESS, 240, 1)
        );
    }

    public static ItemStack getVodka() {
        return createDrink("§b§lVaso de Vodka", Color.fromRGB(220, 240, 255),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 240, 1)
        );
    }

    public static ItemStack getWhisky() {
        return createDrink("§e§lVaso de Whisky", Color.fromRGB(205, 133, 63),
                new PotionEffect(PotionEffectType.SLOWNESS, 240, 2),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 240, 2),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1)
        );
    }

    public static ItemStack getSake() {
        return createDrink("§f§lVasito de Sake", Color.fromRGB(245, 255, 255),
                new PotionEffect(PotionEffectType.SATURATION, 200, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 300, 1),
                new PotionEffect(PotionEffectType.DARKNESS, 200, 1)
        );
    }

    public static ItemStack getGin() {
        return createDrink("§3§lCopa de Ginebra", Color.fromRGB(190, 255, 240),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 300, 1),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2)
        );
    }

    public static ItemStack getAzulito() {
        return createDrink("§b§lAzulito", Color.fromRGB(0, 200, 255),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 1),
                new PotionEffect(PotionEffectType.SATURATION, 240, 2),
                new PotionEffect(PotionEffectType.NAUSEA, 200, 1)
        );
    }

    public static ItemStack getMichelada() {
        return createDrink("§4§lVaso de Michelada", Color.fromRGB(150, 30, 0),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 300, 1),
                new PotionEffect(PotionEffectType.SLOWNESS, 200, 1),
                new PotionEffect(PotionEffectType.SATURATION, 240, 1)
        );
    }


    // =========================================
    //             MÉTODOS CREADORES
    // =========================================

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

    private static ItemStack createDrink(String name, Color color, PotionEffect... effects) {
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
                // Aplicamos el efecto real a la poción
                meta.addCustomEffect(effect, true);

                // Traducimos el nombre del efecto y le asignamos un color bonito
                String effectName = effect.getType().getName();
                String hexColor = "#FFFFFF";

                if (effectName.equals("NAUSEA") || effectName.equals("CONFUSION")) {
                    effectName = "Náuseas"; hexColor = "#99cc33";
                } else if (effectName.equals("SATURATION")) {
                    effectName = "Saturación"; hexColor = "#cc3300";
                } else if (effectName.equals("MINING_FATIGUE") || effectName.equals("SLOW_DIGGING")) {
                    effectName = "Fatiga Minera"; hexColor = "#8B4513";
                } else if (effectName.equals("NIGHT_VISION")) {
                    effectName = "Visión Nocturna"; hexColor = "#1E90FF";
                } else if (effectName.equals("DARKNESS")) {
                    effectName = "Oscuridad"; hexColor = "#4B0082";
                } else if (effectName.equals("SLOW") || effectName.equals("SLOWNESS")) {
                    effectName = "Lentitud"; hexColor = "#FFA500";
                }

                int level = effect.getAmplifier() + 1; // Nivel interno + 1 = Nivel Real
                int seconds = effect.getDuration() / 20; // Ticks a Segundos

                // Creamos la línea estilizada (> Lentitud 1 (15 s))
                lore.add(ChatColor.GRAY + "> " + ChatColor.of(hexColor) + effectName + " " + level +
                        ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + seconds + " s" + ChatColor.GRAY + ")");
            }

            lore.add("");
            meta.setLore(lore);
            meta.setColor(color);

            // Ocultamos la asquerosa tooltip default de Vanilla
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (Exception ignored) {}

            potion.setItemMeta(meta);
        }
        return potion;
    }
}