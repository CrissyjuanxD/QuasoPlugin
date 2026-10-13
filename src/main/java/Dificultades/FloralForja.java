package Dificultades;

import items.EndItems;
import items.FloralItems;
import items.FloralMobItems;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Las recetas del equipo floral (van con la etapa uno, donde está la Abeja Floral):
// - Mejora Floral en la mesa de crafteo: 64 de cada drop floral (carne, ojo, pólvora y aguijón), 1 Esencia Floral y una
//   plantilla de mejora de Netherite. Cada stack se gasta entero
// - 2 Mejoras Florales con 1 mejora, 7 diamantes y 1 Aguijón Floral (como la plantilla de Netherite)
// - En la herrería: Mejora Floral + pieza de Netherite + Bloque de Miel. Se quedan los encantamientos
public class FloralForja implements Listener {

    static final int POR_STACK = 64;
    private static final String MEJORA = "mejora_floral";
    private static final String COPIA = "mejora_floral_copia";

    private final JavaPlugin plugin;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    // Mientras no es null, addRecipe solo junta las recetas (para la web) en vez de registrarlas
    private List<Recipe> recolectando;

    public FloralForja(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerRecipes();
    }

    public void revert() {
        for (NamespacedKey key : recipeKeys) Bukkit.removeRecipe(key, true);
        recipeKeys.clear();
        HandlerList.unregisterAll(this);
    }

    public List<Recipe> recetas() {
        List<Recipe> lista = new ArrayList<>();
        recolectando = lista;
        try {
            registerRecipes();
        } finally {
            recolectando = null;
        }
        return lista;
    }

    private void registerRecipes() {
        RecipeChoice stinger = RecipeChoice.exactChoice(FloralMobItems.createStinger());

        ShapedRecipe mejora = new ShapedRecipe(key(MEJORA), FloralItems.createUpgrade());
        mejora.shape("COP", "AET", "   ");
        mejora.setIngredient('C', RecipeChoice.exactChoice(FloralMobItems.createFlesh()));
        mejora.setIngredient('O', RecipeChoice.exactChoice(FloralMobItems.createEye()));
        mejora.setIngredient('P', RecipeChoice.exactChoice(FloralMobItems.createGunpowder()));
        mejora.setIngredient('A', stinger);
        mejora.setIngredient('E', RecipeChoice.exactChoice(FloralItems.createEssence()));
        mejora.setIngredient('T', RecipeChoice.exactChoice(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        addRecipe(mejora);

        ItemStack dos = FloralItems.createUpgrade();
        dos.setAmount(2);
        ShapedRecipe copia = new ShapedRecipe(key(COPIA), dos);
        copia.shape("DMD", "DAD", "DDD");
        copia.setIngredient('D', RecipeChoice.exactChoice(new ItemStack(Material.DIAMOND)));
        copia.setIngredient('M', RecipeChoice.exactChoice(FloralItems.createUpgrade()));
        copia.setIngredient('A', stinger);
        addRecipe(copia);

        RecipeChoice plantilla = RecipeChoice.exactChoice(FloralItems.createUpgrade());
        RecipeChoice miel = RecipeChoice.exactChoice(new ItemStack(Material.HONEY_BLOCK));
        for (FloralItems.Pieza pieza : FloralItems.Pieza.values()) {
            addRecipe(new SmithingTransformRecipe(key(pieza.id), FloralItems.create(pieza), plantilla,
                    new RecipeChoice.MaterialChoice(pieza.base), miel));
        }
    }

    // Sin 64 de cada drop no sale nada
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!esMejora(event.getRecipe())) return;
        if (!alcanza(event.getInventory().getMatrix())) event.getInventory().setResult(null);
    }

    // El juego gasta 1 de cada casilla; acá se gasta el stack entero de cada drop. Se hace a mano (y una sola
    // vez aunque sea con shift)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!esMejora(event.getRecipe()) || !(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        CraftingInventory inventory = event.getInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack[] matrix = inventory.getMatrix();
            if (!completo(matrix)) return;
            gastar(matrix);
            inventory.setMatrix(matrix);
            player.getInventory().addItem(FloralItems.createUpgrade()).values()
                    .forEach(resto -> player.getWorld().dropItemNaturally(player.getLocation(), resto));
            player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.3f);
            player.updateInventory();
        });
    }

    static boolean alcanza(ItemStack[] matrix) {
        int drops = 0;
        for (ItemStack item : matrix) {
            if (FloralMobItems.dropId(item) == null) continue;
            if (item.getAmount() < POR_STACK) return false;
            drops++;
        }
        return drops == 4;
    }

    // Lo mismo más la esencia y la plantilla de Netherite (por si cambió algo entre el clic y el tick siguiente)
    static boolean completo(ItemStack[] matrix) {
        boolean esencia = false, plantilla = false;
        for (ItemStack item : matrix) {
            if (item == null) continue;
            if ("esencia_floral".equals(EndItems.idOf(item))) esencia = true;
            else if (item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE && EndItems.idOf(item) == null) plantilla = true;
        }
        return esencia && plantilla && alcanza(matrix);
    }

    // De los drops se sacan 64; de la esencia y la plantilla, 1
    static void gastar(ItemStack[] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null || item.getType().isAir()) continue;
            int quitar = FloralMobItems.dropId(item) != null ? POR_STACK : 1;
            int queda = item.getAmount() - quitar;
            if (queda <= 0) matrix[i] = null;
            else item.setAmount(queda);
        }
    }

    private boolean esMejora(Recipe recipe) {
        return recipe instanceof Keyed keyed && keyed.getKey().equals(key(MEJORA));
    }

    // La herrería no mejora items custom (armadura de Warden, Celestita o lo que ya es floral)
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getInventory().getRecipe() instanceof Keyed keyed) || !recipeKeys.contains(keyed.getKey())) return;
        ItemStack base = event.getInventory().getInputEquipment();
        if (EndItems.idOf(base) != null || (base != null && base.hasItemMeta()
                && base.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "warden_armor")))) {
            event.setResult(null);
        }
    }

    // La armadura floral tiene 15% de no gastarse
    @EventHandler(ignoreCancelled = true)
    public void onDesgaste(PlayerItemDamageEvent event) {
        if (FloralItems.isArmor(event.getItem()) && ThreadLocalRandom.current().nextDouble() < FloralItems.Pieza.SIN_DESGASTE) {
            event.setCancelled(true);
        }
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    private void addRecipe(Recipe recipe) {
        if (recolectando != null) {
            recolectando.add(recipe);
            return;
        }
        NamespacedKey key = ((Keyed) recipe).getKey();
        if (Bukkit.getRecipe(key) == null) Bukkit.addRecipe(recipe, true);
        recipeKeys.add(key);
    }
}
