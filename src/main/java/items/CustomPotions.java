package items;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CustomPotions {

    // 1. Poción de Resistencia II por 10 minutos (Tomable)
    public static ItemStack getResistanceIIPotion() {
        return createPotion(
                Material.POTION,
                "§9§lPoción de Resistencia II",
                PotionEffectType.RESISTANCE,
                12000, // 10 minutos (10 * 60 * 20)
                1,     // Nivel II (Amplificador 1)
                Color.BLUE
        );
    }

    // 2. Poción de Resistencia III por 6 minutos (Lanzable)
    public static ItemStack getSplashResistanceIIIPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§9§lPoción de Resistencia III",
                PotionEffectType.RESISTANCE,
                7200, // 6 minutos (6 * 60 * 20)
                2,    // Nivel III (Amplificador 2)
                Color.BLUE
        );
    }

    // 3. Poción de Caída Lenta por 15 minutos (Tomable)
    public static ItemStack getSlowFallingPotion() {
        return createPotion(
                Material.POTION,
                "§7§lPoción de Caída Lenta",
                PotionEffectType.SLOW_FALLING,
                18000, // 15 minutos (15 * 60 * 20)
                0,     // Nivel I
                Color.GRAY
        );
    }

    // 4. Poción de Regeneración III por 3 minutos (Lanzable)
    public static ItemStack getSplashRegenerationIIIPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§d§lPoción de Regeneración III",
                PotionEffectType.REGENERATION,
                3600, // 3 minutos (3 * 60 * 20)
                2,    // Nivel III
                Color.FUCHSIA
        );
    }

    // 5. Poción de Prisa (Haste) III por 15 minutos (Tomable)
    public static ItemStack getHasteIIIPotion() {
        return createPotion(
                Material.POTION,
                "§e§lPoción de Prisa Minera III",
                PotionEffectType.HASTE,
                18000, // 15 minutos
                2,     // Nivel III
                Color.YELLOW
        );
    }

    // 6. Poción de Prisa (Haste) II por 15 minutos (Tomable)
    public static ItemStack getHasteIIPotion() {
        return createPotion(
                Material.POTION,
                "§e§lPoción de Prisa Minera II",
                PotionEffectType.HASTE,
                18000, // 15 minutos
                1,     // Nivel II
                Color.YELLOW
        );
    }

    // 7. Poción de Absorción X por 3 minutos (Lanzable)
    public static ItemStack getSplashAbsorptionXPotion() {
        return createPotion(
                Material.SPLASH_POTION,
                "§6§lPoción de Absorción X",
                PotionEffectType.ABSORPTION,
                3600, // 3 minutos
                9,    // Nivel X (Amplificador 9)
                Color.ORANGE
        );
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

            // Añadir el efecto principal. El true fuerza a que sobrescriba otros efectos si los hay
            meta.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);

            // Colorear el líquido de la poción para que combine visualmente con el nombre
            meta.setColor(color);

            potion.setItemMeta(meta);
        }

        return potion;
    }
}