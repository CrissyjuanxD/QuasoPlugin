package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

// Items del End: Cristal de Celestita (racimos de las geodas del Bosque Prismático), Fragmento Astral (mobs del End),
// Esencia Marchita (shulkers negros y wither skeletons del Páramo) y el Ojo del Rey Ender que se craftea con los tres
public final class EndItems {

    // La misma PDC "custom_item" que leen las misiones (MissionUtils.customId)
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("quasoplugin", "custom_item");
    private static final NamespacedKey KING_EYE_RECIPE = new NamespacedKey("quasoplugin", "ojo_rey_ender");

    private EndItems() {}

    public static ItemStack createCelestiteCrystal(int amount) {
        return item(Material.PRISMARINE_CRYSTALS, amount, "cristal_celestita", "#8fd3ff", "Cristal de Celestita", ItemRarity.RARE,
                "Sale de los racimos de amatista", "de las geodas del Bosque Prismático.");
    }

    public static ItemStack createAstralFragment(int amount) {
        return item(Material.FIREWORK_STAR, amount, "fragmento_astral", "#c58cff", "Fragmento Astral", ItemRarity.RARE,
                "Lo sueltan los mobs del End.", "Va en la Plantilla de Celestita.");
    }

    public static ItemStack createWitheredEssence(int amount) {
        return item(Material.BLACK_DYE, amount, "esencia_marchita", "#9a8fb0", "Esencia Marchita", ItemRarity.UNCOMMON,
                "La sueltan los Shulkers Negros y los", "Wither Skeletons del Páramo Marchito.");
    }

    public static ItemStack createKingEye() {
        return item(Material.ENDER_EYE, 1, "ojo_rey_ender", "#d36bff", "Ojo del Rey Ender", ItemRarity.EPIC,
                "Sirve para invocar al Rey Ender.", "No se puede tirar ni poner en un portal.");
    }

    private static ItemStack item(Material type, int amount, String id, String color, String name, ItemRarity rarity, String... lines) {
        ItemStack item = new ItemStack(type, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(color) + name);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        meta.setLore(lore);
        meta.setRarity(rarity);
        ItemModels.apply(meta, id);
        item.setItemMeta(meta);
        return item;
    }

    public static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    public static boolean isEndItem(ItemStack item) {
        String id = idOf(item);
        return "cristal_celestita".equals(id) || "fragmento_astral".equals(id) || "esencia_marchita".equals(id) || "ojo_rey_ender".equals(id);
    }

    // Ojo del Rey Ender: un ojo de ender rodeado de 4 Esencias Marchitas, 2 Cristales de Celestita y 2 Fragmentos
    // Astrales, así hace falta ir a los dos biomas nuevos y pelear con los mobs del End
    public static void registerRecipes(JavaPlugin plugin) {
        if (Bukkit.getRecipe(KING_EYE_RECIPE) != null) return;
        ShapedRecipe recipe = new ShapedRecipe(KING_EYE_RECIPE, createKingEye());
        recipe.shape("EAE", "COC", "EAE");
        recipe.setIngredient('E', new RecipeChoice.ExactChoice(createWitheredEssence(1)));
        recipe.setIngredient('A', new RecipeChoice.ExactChoice(createAstralFragment(1)));
        recipe.setIngredient('C', new RecipeChoice.ExactChoice(createCelestiteCrystal(1)));
        recipe.setIngredient('O', Material.ENDER_EYE);
        Bukkit.addRecipe(recipe);
    }
}
