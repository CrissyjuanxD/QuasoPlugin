package Dificultades;

import Armors.WardenArmor;
import Bosses.ReyEnderAltar;
import Dificultades.CustomMobs.EnderBlaze;
import Dificultades.CustomMobs.EnderCreeper;
import Dificultades.CustomMobs.EnderSpider;
import EndBiomes.BlackShulker;
import EndBiomes.EndDrops;
import EndBiomes.EndShulkers;
import EndBiomes.EndSpawns;
import EndBiomes.EnderInsect;
import items.EndItems;
import items.WardenCaveItems;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ThreeChanges implements Listener, Change {

    private static final String WINGED = "peto_warden_alado";

    private final JavaPlugin plugin;
    private boolean isApplied = false;

    private final EnderBlaze enderBlaze;
    private final EnderCreeper enderCreeper;
    private final EnderSpider enderSpider;
    private final BlackShulker blackShulker;
    private final WardenArmor armor;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private EnderInsect enderInsect;

    public ThreeChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enderBlaze = new EnderBlaze(plugin);
        this.enderCreeper = new EnderCreeper(plugin);
        this.enderSpider = new EnderSpider(plugin);
        this.blackShulker = new BlackShulker(plugin);
        this.armor = new WardenArmor(plugin);
    }

    @Override
    public String id() {
        return "tres";
    }

    @Override
    public String description() {
        return "End: mobs del End por bioma, shulkers, Celestita, Peto de Warden Alado y el Rey Ender";
    }

    @Override
    public boolean isApplied() {
        return isApplied;
    }

    // Abre la etapa del End: los mobs del End spawnean en sus biomas, los shulkers cambian, salen las drops del End,
    // las recetas de la Celestita y el altar del Rey Ender
    @Override
    public void apply() {
        if (isApplied) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        enderBlaze.apply();
        enderCreeper.apply();
        enderSpider.apply();

        enderInsect = new EnderInsect(plugin);
        ReyEnderAltar altar = new ReyEnderAltar(plugin);
        listeners.add(blackShulker);
        listeners.add(enderInsect);
        listeners.add(new EndDrops(plugin));
        listeners.add(new EndShulkers(plugin, blackShulker));
        listeners.add(new EndSpawns(plugin, enderBlaze, enderCreeper, enderSpider, blackShulker));
        listeners.add(altar);
        for (Listener listener : listeners) Bukkit.getPluginManager().registerEvents(listener, plugin);
        blackShulker.start();

        registerRecipes();
        altar.restoreLoaded();
        isApplied = true;
    }

    // Deshace todo lo de apply(): saca las recetas y los listeners y borra los Ender mobs
    @Override
    public void revert() {
        if (!isApplied) return;
        for (NamespacedKey key : recipeKeys) Bukkit.removeRecipe(key, true);
        recipeKeys.clear();
        for (Listener listener : listeners) HandlerList.unregisterAll(listener);
        listeners.clear();
        blackShulker.stop();
        enderInsect.stop();
        enderInsect = null;
        enderBlaze.revert();
        enderCreeper.revert();
        enderSpider.revert();
        HandlerList.unregisterAll(this);
        isApplied = false;
    }

    // Recetas del End: el Lingote de Celestita lleva un Lingote Profundo (Warden Cave), la plantilla lleva Fragmentos
    // Astrales y cada herramienta se mejora en la herrería. El Ojo del Rey Ender pide los dos biomas nuevos y el Peto
    // de Warden Alado la perla del Rey Ender. Los items vanilla van con exactChoice para que no entren items custom
    private void registerRecipes() {
        RecipeChoice crystal = RecipeChoice.exactChoice(EndItems.createCelestiteCrystal(1));
        RecipeChoice astral = RecipeChoice.exactChoice(EndItems.createAstralFragment(1));
        RecipeChoice essence = RecipeChoice.exactChoice(EndItems.createWitheredEssence(1));
        RecipeChoice ingot = RecipeChoice.exactChoice(EndItems.createCelestiteIngot());
        RecipeChoice template = RecipeChoice.exactChoice(EndItems.createCelestiteTemplate());
        RecipeChoice gold = RecipeChoice.exactChoice(new ItemStack(Material.GOLD_INGOT));
        RecipeChoice enderEye = RecipeChoice.exactChoice(new ItemStack(Material.ENDER_EYE));

        ShapedRecipe celestiteIngot = new ShapedRecipe(key("lingote_celestita"), EndItems.createCelestiteIngot());
        celestiteIngot.shape("CGC", "GDG", "CGC");
        celestiteIngot.setIngredient('C', crystal);
        celestiteIngot.setIngredient('G', gold);
        celestiteIngot.setIngredient('D', RecipeChoice.exactChoice(WardenCaveItems.createDeepIngot()));
        addRecipe(celestiteIngot);

        ShapedRecipe celestiteTemplate = new ShapedRecipe(key("plantilla_celestita"), EndItems.createCelestiteTemplate());
        celestiteTemplate.shape("AEA", "LTL", "AEA");
        celestiteTemplate.setIngredient('A', astral);
        celestiteTemplate.setIngredient('E', enderEye);
        celestiteTemplate.setIngredient('L', ingot);
        celestiteTemplate.setIngredient('T', RecipeChoice.exactChoice(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        addRecipe(celestiteTemplate);

        for (EndItems.Tool tool : EndItems.Tool.values()) {
            addRecipe(new SmithingTransformRecipe(key(tool.id), EndItems.createTool(tool), template,
                    new RecipeChoice.MaterialChoice(tool.base), ingot));
        }

        ShapedRecipe kingEye = new ShapedRecipe(key("ojo_rey_ender"), EndItems.createKingEye());
        kingEye.shape("EAE", "COC", "EAE");
        kingEye.setIngredient('E', essence);
        kingEye.setIngredient('A', astral);
        kingEye.setIngredient('C', crystal);
        kingEye.setIngredient('O', enderEye);
        addRecipe(kingEye);

        addRecipe(new SmithingTransformRecipe(key(WINGED), armor.createWingedWardenChestplate(),
                RecipeChoice.exactChoice(EndItems.createEnderKingPearl()),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_CHESTPLATE), new RecipeChoice.MaterialChoice(Material.ELYTRA)));
    }

    // La herrería deja la base como está y le pone lo del resultado encima, así que hay que mirar qué se mejora: el
    // Peto Alado solo sale de un Peto de Warden, y las herramientas de Celestita no salen de otro item custom
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getInventory().getRecipe() instanceof Keyed keyed)) return;
        NamespacedKey recipe = keyed.getKey();
        if (!recipeKeys.contains(recipe)) return;
        ItemStack base = event.getInventory().getInputEquipment();
        if (recipe.getKey().equals(WINGED)) {
            if (!armor.isWardenArmor(base) || WardenArmor.isWinged(base)) event.setResult(null);
        } else if (EndItems.toolById(recipe.getKey()) != null && EndItems.idOf(base) != null) {
            event.setResult(null);
        }
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    private void addRecipe(Recipe recipe) {
        NamespacedKey key = ((Keyed) recipe).getKey();
        if (Bukkit.getRecipe(key) == null) Bukkit.addRecipe(recipe, true);
        recipeKeys.add(key);
    }
}
