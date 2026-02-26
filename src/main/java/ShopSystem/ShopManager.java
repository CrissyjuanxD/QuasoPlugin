package ShopSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {

    private final JavaPlugin plugin;
    private final File tradesFile;
    public final NamespacedKey shopKey;
    public final NamespacedKey shopIdKey;

    // Control de edición
    // UUID del jugador -> ID de la tienda que edita
    public final Map<UUID, String> editingShops = new ConcurrentHashMap<>();
    // UUID del jugador -> Número de tradeo (0-11)
    public final Map<UUID, Integer> editingTradeIndex = new ConcurrentHashMap<>();
    // UUID del jugador -> Tipo de slot ("Ingrediente 1", "Resultado", etc)
    public final Map<UUID, String> editingSlotType = new ConcurrentHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopKey = new NamespacedKey(plugin, "shop_type");
        this.shopIdKey = new NamespacedKey(plugin, "shop_id");
        this.tradesFile = new File(plugin.getDataFolder(), "tradeos.yml");

        if (!tradesFile.exists()) {
            try {
                tradesFile.getParentFile().mkdirs();
                tradesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creando tradeos.yml: " + e.getMessage());
            }
        }
    }

    public void spawnShop(String name, Location location, Villager.Type type, Villager.Profession profession) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        String shopId = UUID.randomUUID().toString();

        String coloredName = ChatColor.translateAlternateColorCodes('&', name);

        villager.setCustomName(coloredName);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);

        // Aplicar personalización
        villager.setVillagerType(type != null ? type : Villager.Type.PLAINS);
        villager.setProfession(profession != null ? profession : Villager.Profession.NONE);

        if (villager.getAttribute(Attribute.MOVEMENT_SPEED) != null)
            villager.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0);

        villager.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, "custom_shop");
        villager.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, shopId);

        initializeEmptyTrades(villager);
        saveShopTrades(shopId, villager.getRecipes());
    }

    private void initializeEmptyTrades(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            recipes.add(createEmptyRecipe());
        }
        villager.setRecipes(recipes);
    }

    public MerchantRecipe createEmptyRecipe() {
        ItemStack placeholder = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Vacío");
        // meta.setCustomModelData(1); // Opcional: para evitar conflictos visuales
        placeholder.setItemMeta(meta);

        MerchantRecipe recipe = new MerchantRecipe(placeholder, Integer.MAX_VALUE);
        recipe.addIngredient(placeholder);

        recipe.setExperienceReward(false);
        recipe.setPriceMultiplier(0.0f);
        return recipe;
    }

    public void saveShopTrades(String shopId, List<MerchantRecipe> recipes) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId + ".trades", null); // Limpiar previo

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            String path = "shops." + shopId + ".trades." + i;

            // Ingredientes
            for (int j = 0; j < recipe.getIngredients().size(); j++) {
                config.set(path + ".ingredients." + j, recipe.getIngredients().get(j));
            }
            // Resultado
            config.set(path + ".result", recipe.getResult());
        }

        try {
            config.save(tradesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadShopTrades(String shopId, Villager villager) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        if (!config.contains("shops." + shopId)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            String path = "shops." + shopId + ".trades." + i;
            if (config.contains(path)) {
                ItemStack result = config.getItemStack(path + ".result");

                // Validación para evitar el crash de "Empty Result"
                if (result == null || result.getType() == Material.AIR) {
                    result = new ItemStack(Material.STRUCTURE_VOID);
                    ItemMeta meta = result.getItemMeta();
                    meta.setDisplayName(ChatColor.GRAY + "Vacío");
                    result.setItemMeta(meta);
                }

                MerchantRecipe recipe = new MerchantRecipe(result, Integer.MAX_VALUE);
                recipe.setPriceMultiplier(0.0f);
                recipe.setExperienceReward(false);

                for (int j = 0; j < 2; j++) {
                    ItemStack ing = config.getItemStack(path + ".ingredients." + j);
                    if (ing != null && !ing.getType().isAir()) {
                        recipe.addIngredient(ing);
                    }
                }

                if (recipe.getIngredients().isEmpty()) {
                    // Ingrediente placeholder
                    ItemStack ph = new ItemStack(Material.STRUCTURE_VOID);
                    ItemMeta m = ph.getItemMeta();
                    m.setDisplayName(ChatColor.GRAY + "Vacío");
                    ph.setItemMeta(m);
                    recipe.addIngredient(ph);
                }

                recipes.add(recipe);
            } else {
                recipes.add(createEmptyRecipe());
            }
        }
        villager.setRecipes(recipes);
    }

    public void updateTradesForPlayer(Villager villager, Player player) {
        List<MerchantRecipe> currentRecipes = new ArrayList<>(villager.getRecipes());
        boolean needsUpdate = false;
        List<MerchantRecipe> updatedRecipes = new ArrayList<>();

        for (MerchantRecipe recipe : currentRecipes) {
            // LÓGICA ANTIGUA: Preparamos ingredientes
            List<ItemStack> ingredients = recipe.getIngredients();
            List<ItemStack> newIngredients = new ArrayList<>();
            boolean recipeChanged = false;

            for (ItemStack ingredient : ingredients) {
                // Verificamos si es un item especial (mochila/custom)
                if (hasCustomModelData(ingredient)) {
                    // Buscamos coincidencia en el jugador
                    ItemStack playerMatchingItem = findMatchingItemInPlayerInventory(player, ingredient);

                    if (playerMatchingItem != null) {
                        // ¡ENCONTRADO! Usamos el item del jugador como ingrediente
                        // IMPORTANTE: Clonamos el item del jugador para capturar su UUID/Lore
                        ItemStack requirement = playerMatchingItem.clone();
                        requirement.setAmount(ingredient.getAmount());
                        newIngredients.add(requirement);
                        recipeChanged = true; // Marcamos que HUBO un cambio
                    } else {
                        // No lo tiene, mantenemos el ingrediente original limpio
                        newIngredients.add(ingredient);
                    }
                } else {
                    // Item normal, mantenemos
                    newIngredients.add(ingredient);
                }
            }

            // --- CORRECCIÓN CRÍTICA ---
            // Solo creamos una receta nueva si REALMENTE cambiamos algo.
            // Si creamos una receta nueva para tradeos que no cambiaron, el resultado se vuelve invisible.
            if (recipeChanged) {
                MerchantRecipe newRecipe = new MerchantRecipe(
                        recipe.getResult(), // Usamos el resultado original DIRECTO (Sin clone, como en tu código viejo)
                        0, // Reiniciamos usos
                        Integer.MAX_VALUE, // Max usos infinito
                        recipe.hasExperienceReward(),
                        recipe.getVillagerExperience(),
                        recipe.getPriceMultiplier()
                );
                newRecipe.setIngredients(newIngredients);
                updatedRecipes.add(newRecipe);
                needsUpdate = true;
            } else {
                // Si no cambió nada, añadimos la receta ORIGINAL EXACTA.
                // Esto mantiene la estabilidad visual del cliente.
                updatedRecipes.add(recipe);
            }
        }

        if (needsUpdate) {
            villager.setRecipes(updatedRecipes);
        }
    }

    private boolean hasCustomModelData(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    private ItemStack findMatchingItemInPlayerInventory(Player player, ItemStack shopIngredient) {
        int targetModelData = shopIngredient.getItemMeta().getCustomModelData();
        Material targetMaterial = shopIngredient.getType();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == targetMaterial && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasCustomModelData() && meta.getCustomModelData() == targetModelData) {
                    return item;
                }
            }
        }
        return null;
    }

    public void removeShopFromFile(String shopId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId, null);
        try { config.save(tradesFile); } catch (IOException e) {}
    }


    // Encuentra el aldeano en el mundo por su ID persistente
    public Villager getVillagerById(String shopId) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Villager v : world.getEntitiesByClass(Villager.class)) {
                if (shopId.equals(v.getPersistentDataContainer().get(shopIdKey, PersistentDataType.STRING))) {
                    return v;
                }
            }
        }
        return null;
    }
}