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

    public final Map<UUID, String> editingShops = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> editingTradeIndex = new ConcurrentHashMap<>();
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
            ItemStack emptyPaper = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyPaper, Integer.MAX_VALUE);
            recipe.addIngredient(emptyPaper);
            recipes.add(recipe);
        }
        villager.setRecipes(recipes);
    }

    public ItemStack createEmptyTradeItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Vacío");
        meta.setCustomModelData(100);
        paper.setItemMeta(meta);
        return paper;
    }

    // Método extraído literalmente de tu código antiguo
    public void updateVillagerTrade(Villager villager, int tradeNumber, String slotType, ItemStack item) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

        while (recipes.size() <= tradeNumber) {
            ItemStack emptyPaper = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyPaper, Integer.MAX_VALUE);
            recipe.addIngredient(emptyPaper);
            recipes.add(recipe);
        }

        MerchantRecipe currentRecipe = recipes.get(tradeNumber);
        List<ItemStack> ingredients = new ArrayList<>(currentRecipe.getIngredients());
        ItemStack result = currentRecipe.getResult();

        switch (slotType) {
            case "Ingrediente 1":
                if (item.getType() == Material.AIR) {
                    if (!ingredients.isEmpty()) {
                        ingredients.set(0, createEmptyTradeItem());
                    }
                } else {
                    if (ingredients.isEmpty()) {
                        ingredients.add(item);
                    } else {
                        ingredients.set(0, item);
                    }
                }
                break;
            case "Ingrediente 2":
                if (item.getType() == Material.AIR) {
                    if (ingredients.size() > 1) {
                        ingredients.remove(1);
                    }
                } else {
                    if (ingredients.size() < 2) {
                        if (ingredients.isEmpty()) {
                            ingredients.add(createEmptyTradeItem());
                        }
                        ingredients.add(item);
                    } else {
                        ingredients.set(1, item);
                    }
                }
                break;
            case "Resultado":
                result = (item.getType() == Material.AIR) ? createEmptyTradeItem() : item;
                break;
        }

        MerchantRecipe newRecipe = new MerchantRecipe(
                (result == null || result.getType() == Material.AIR) ? createEmptyTradeItem() : result, Integer.MAX_VALUE
        );

        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && !ingredient.getType().isAir()) {
                newRecipe.addIngredient(ingredient);
            }
        }

        if (newRecipe.getIngredients().isEmpty()) {
            newRecipe.addIngredient(createEmptyTradeItem());
        }

        recipes.set(tradeNumber, newRecipe);
        villager.setRecipes(recipes);
    }

    public void saveShopTrades(String shopId, List<MerchantRecipe> recipes) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId + ".trades", null);

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            String basePath = "shops." + shopId + ".trades." + i;

            List<ItemStack> ingredients = recipe.getIngredients();
            for (int j = 0; j < ingredients.size(); j++) {
                ItemStack ingredient = ingredients.get(j);
                if (ingredient != null && !ingredient.getType().isAir()) {
                    config.set(basePath + ".ingredients." + j, ingredient);
                }
            }

            ItemStack result = recipe.getResult();
            if (result != null && !result.getType().isAir()) {
                config.set(basePath + ".result", result);
            } else {
                config.set(basePath + ".result", createEmptyTradeItem());
            }

            config.set(basePath + ".maxUses", recipe.getMaxUses());
        }

        try {
            config.save(tradesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando tradeos: " + e.getMessage());
        }
    }

    public void loadShopTrades(String shopId, Villager villager) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        if (!config.contains("shops." + shopId)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            String basePath = "shops." + shopId + ".trades." + i;

            if (config.contains(basePath)) {
                ItemStack result = config.getItemStack(basePath + ".result");
                int maxUses = Integer.MAX_VALUE;

                if (result != null) {
                    MerchantRecipe recipe = new MerchantRecipe(result, maxUses);

                    for (int j = 0; j < 2; j++) {
                        ItemStack ingredient = config.getItemStack(basePath + ".ingredients." + j);
                        if (ingredient != null && !ingredient.getType().isAir()) {
                            recipe.addIngredient(ingredient);
                        }
                    }
                    recipes.add(recipe);
                } else {
                    ItemStack emptyPaper = createEmptyTradeItem();
                    MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
                    recipe.addIngredient(emptyPaper);
                    recipes.add(recipe);
                }
            } else {
                ItemStack emptyPaper = createEmptyTradeItem();
                MerchantRecipe recipe = new MerchantRecipe(emptyPaper, 999);
                recipe.addIngredient(emptyPaper);
                recipes.add(recipe);
            }
        }
        villager.setRecipes(recipes);
    }

    // MÉTODO EXACTO DE TU CÓDIGO ANTIGUO
    public void updateTradesForPlayer(Villager villager, Player player) {
        List<MerchantRecipe> currentRecipes = new ArrayList<>(villager.getRecipes());
        boolean needsUpdate = false;
        List<MerchantRecipe> updatedRecipes = new ArrayList<>();

        for (MerchantRecipe recipe : currentRecipes) {
            MerchantRecipe newRecipe = new MerchantRecipe(recipe.getResult(), 0, Integer.MAX_VALUE, recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());
            List<ItemStack> ingredients = recipe.getIngredients();
            List<ItemStack> newIngredients = new ArrayList<>();

            boolean recipeChanged = false;

            for (ItemStack ingredient : ingredients) {
                if (hasCustomModelData(ingredient)) {
                    ItemStack playerMatchingItem = findMatchingItemInPlayerInventory(player, ingredient);

                    if (playerMatchingItem != null) {
                        ItemStack requirement = playerMatchingItem.clone();
                        requirement.setAmount(ingredient.getAmount());
                        newIngredients.add(requirement);
                        recipeChanged = true;
                    } else {
                        newIngredients.add(ingredient);
                    }
                } else {
                    newIngredients.add(ingredient);
                }
            }

            if (recipeChanged) {
                newRecipe.setIngredients(newIngredients);
                updatedRecipes.add(newRecipe);
                needsUpdate = true;
            } else {
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

    public JavaPlugin getPlugin() {
        return plugin;
    }
}