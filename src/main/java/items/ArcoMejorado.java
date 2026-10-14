package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

// El Arco Mejorado (cambio uno): un arco con 3 bloques de hierro, 3 de oro y un lingote de Netherite. Es el único arco
// que acepta Perforación y Poder VI en el yunque (AnvilOverEnchantHandler); la Perforación hace que la flecha atraviese
// mobs como con la ballesta
public class ArcoMejorado implements Listener {

    public static final String ID = "arco_mejorado";
    public static final int PODER_MAXIMO = 6;
    private static final String COLOR = "#E3C16F";

    private final JavaPlugin plugin;
    private final NamespacedKey recipeKey;
    private boolean registrada;

    public ArcoMejorado(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, ID);
    }

    public static ItemStack create() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        meta.setDisplayName(ChatColor.of(COLOR) + "" + ChatColor.BOLD + "Arco Mejorado");
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, ID);
        meta.setRarity(ItemRarity.EPIC);
        meta.setItemModel(NamespacedKey.minecraft(ID));
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of(EquipoLore.tono(COLOR, 0.75)) + "Reforzado con hierro, oro");
        lore.add(ChatColor.of(EquipoLore.tono(COLOR, 0.75)) + "y Netherite.");
        lore.add("");
        lore.add(ChatColor.GRAY + "En el yunque:");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of(EquipoLore.tono(COLOR, 1.45)) + "Acepta Perforación");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of(EquipoLore.tono(COLOR, 1.45)) + "Poder hasta VI (Poder V + libro V)");
        lore.add("");
        meta.setLore(lore);
        bow.setItemMeta(meta);
        return bow;
    }

    public static boolean es(ItemStack item) {
        return ID.equals(EndItems.idOf(item));
    }

    public void apply() {
        if (registrada) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (Bukkit.getRecipe(recipeKey) == null) Bukkit.addRecipe(receta(), true);
        registrada = true;
    }

    public void revert() {
        if (!registrada) return;
        Bukkit.removeRecipe(recipeKey, true);
        HandlerList.unregisterAll(this);
        registrada = false;
    }

    // Hierro a la izquierda, oro a la derecha y el Netherite arriba del arco
    public ShapedRecipe receta() {
        ShapedRecipe receta = new ShapedRecipe(recipeKey, create());
        receta.shape("HNO", "HBO", "H O");
        receta.setIngredient('H', Material.IRON_BLOCK);
        receta.setIngredient('O', Material.GOLD_BLOCK);
        receta.setIngredient('N', Material.NETHERITE_INGOT);
        receta.setIngredient('B', new RecipeChoice.MaterialChoice(Material.BOW));
        return receta;
    }

    public List<Recipe> recetas() {
        return List.of(receta());
    }

    // Solo con un arco normal (no los custom) y se quedan sus encantamientos
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed) || !keyed.getKey().equals(recipeKey)) return;
        ItemStack arco = null;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.BOW) arco = item;
        }
        event.getInventory().setResult(arco == null || esCustom(arco) ? null : mejorar(arco));
    }

    static boolean esCustom(ItemStack arco) {
        if (!arco.hasItemMeta()) return false;
        ItemMeta meta = arco.getItemMeta();
        return meta.hasItemModel() || meta.hasCustomModelData() || !meta.getPersistentDataContainer().isEmpty();
    }

    static ItemStack mejorar(ItemStack arco) {
        ItemStack resultado = create();
        ItemMeta meta = resultado.getItemMeta();
        arco.getEnchantments().forEach((enchant, nivel) -> meta.addEnchant(enchant, nivel, true));
        resultado.setItemMeta(meta);
        return resultado;
    }

    // El nivel de Poder que acepta un arco en el yunque
    public static int poderMaximo(ItemStack item) {
        return es(item) ? PODER_MAXIMO : Enchantment.POWER.getMaxLevel();
    }
}
