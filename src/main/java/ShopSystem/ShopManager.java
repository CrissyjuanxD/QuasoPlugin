package ShopSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
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

    public final Map<UUID, String> activeShops = new ConcurrentHashMap<>();
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

    // Aldeano quieto e invulnerable con 10 tradeos vacíos, identificado con un id en su PDC
    public Villager spawnShop(String name, Location location, Villager.Type type, Villager.Profession profession) {
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
        return villager;
    }

    private void initializeEmptyTrades(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ItemStack emptyItem = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
            recipe.addIngredient(emptyItem);
            recipes.add(recipe);
        }
        villager.setRecipes(recipes);
    }

    public ItemStack createEmptyTradeItem() {
        ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Vacío");
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType() == Material.STRUCTURE_VOID;
    }

    // Compara por custom model data o por nombre si el item pedido los tiene, si no solo por material
    public boolean isMatch(ItemStack invItem, ItemStack reqItem) {
        if (isEmpty(invItem) || isEmpty(reqItem)) return false;
        if (invItem.getType() != reqItem.getType()) return false;

        ItemMeta invMeta = invItem.getItemMeta();
        ItemMeta reqMeta = reqItem.getItemMeta();

        if (reqMeta != null && reqMeta.hasCustomModelData()) {
            return invMeta != null && invMeta.hasCustomModelData() && invMeta.getCustomModelData() == reqMeta.getCustomModelData();
        }
        if (reqMeta != null && reqMeta.hasDisplayName()) {
            return invMeta != null && invMeta.hasDisplayName() && invMeta.getDisplayName().equals(reqMeta.getDisplayName());
        }

        return true;
    }

    // Cambia un ingrediente o el producto de un tradeo desde el menú de configuración
    public void updateVillagerTrade(Villager villager, int tradeNumber, String slotType, ItemStack item) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());

        while (recipes.size() <= tradeNumber) {
            ItemStack emptyItem = createEmptyTradeItem();
            MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
            recipe.addIngredient(emptyItem);
            recipes.add(recipe);
        }

        MerchantRecipe currentRecipe = recipes.get(tradeNumber);
        List<ItemStack> ingredients = new ArrayList<>(currentRecipe.getIngredients());
        ItemStack result = currentRecipe.getResult();

        switch (slotType) {
            case "Ingrediente 1":
                if (isEmpty(item)) {
                    if (!ingredients.isEmpty()) ingredients.set(0, createEmptyTradeItem());
                } else {
                    if (ingredients.isEmpty()) ingredients.add(item);
                    else ingredients.set(0, item);
                }
                break;
            case "Ingrediente 2":
                if (isEmpty(item)) {
                    if (ingredients.size() > 1) ingredients.remove(1);
                } else {
                    if (ingredients.size() < 2) {
                        if (ingredients.isEmpty()) ingredients.add(createEmptyTradeItem());
                        ingredients.add(item);
                    } else {
                        ingredients.set(1, item);
                    }
                }
                break;
            case "Producto":
                result = isEmpty(item) ? createEmptyTradeItem() : item;
                break;
        }

        MerchantRecipe newRecipe = new MerchantRecipe(isEmpty(result) ? createEmptyTradeItem() : result, 9999);

        for (ItemStack ingredient : ingredients) {
            if (!isEmpty(ingredient)) newRecipe.addIngredient(ingredient);
        }
        if (newRecipe.getIngredients().isEmpty()) newRecipe.addIngredient(createEmptyTradeItem());

        recipes.set(tradeNumber, newRecipe);
        villager.setRecipes(recipes);
    }

    // Guarda los tradeos del aldeano en tradeos.yml
    public void saveShopTrades(String shopId, List<MerchantRecipe> recipes) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId + ".trades", null);

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            String basePath = "shops." + shopId + ".trades." + i;

            List<ItemStack> ingredients = recipe.getIngredients();
            for (int j = 0; j < ingredients.size(); j++) {
                ItemStack ingredient = ingredients.get(j);
                if (!isEmpty(ingredient)) config.set(basePath + ".ingredients." + j, ingredient);
            }

            ItemStack result = recipe.getResult();
            if (!isEmpty(result)) config.set(basePath + ".result", result);
            else config.set(basePath + ".result", createEmptyTradeItem());

            config.set(basePath + ".maxUses", recipe.getMaxUses());
        }

        try { config.save(tradesFile); } catch (IOException e) { plugin.getLogger().severe("Error guardando tradeos: " + e.getMessage()); }
    }

    // Carga los 10 tradeos guardados y rellena con vacíos los que falten
    public void loadShopTrades(String shopId, Villager villager) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        if (!config.contains("shops." + shopId)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String basePath = "shops." + shopId + ".trades." + i;

            if (config.contains(basePath)) {
                ItemStack result = config.getItemStack(basePath + ".result");

                if (result != null) {
                    MerchantRecipe recipe = new MerchantRecipe(result, 9999);
                    for (int j = 0; j < 2; j++) {
                        ItemStack ingredient = config.getItemStack(basePath + ".ingredients." + j);
                        if (!isEmpty(ingredient)) recipe.addIngredient(ingredient);
                    }
                    if (recipe.getIngredients().isEmpty()) {
                        recipe.addIngredient(createEmptyTradeItem());
                    }
                    recipes.add(recipe);
                } else {
                    ItemStack emptyItem = createEmptyTradeItem();
                    MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
                    recipe.addIngredient(emptyItem);
                    recipes.add(recipe);
                }
            } else {
                ItemStack emptyItem = createEmptyTradeItem();
                MerchantRecipe recipe = new MerchantRecipe(emptyItem, 9999);
                recipe.addIngredient(emptyItem);
                recipes.add(recipe);
            }
        }
        villager.setRecipes(recipes);
    }

    public void removeShopFromFile(String shopId) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId, null);
        try { config.save(tradesFile); } catch (IOException e) {}
    }

    // ---------------------------------------------------------------- Catálogo por secciones (/tienda)

    public int getSeccion() {
        return Math.max(1, YamlConfiguration.loadConfiguration(tradesFile).getInt("seccion", 1));
    }

    public void setSeccion(int seccion) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("seccion", seccion);
        try { config.save(tradesFile); } catch (IOException e) { plugin.getLogger().severe("Error guardando la sección de la tienda: " + e.getMessage()); }
    }

    // Qué tienda del catálogo es cada aldeano (shopId -> catálogo)
    public Map<String, String> getCatalogos() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        Map<String, String> catalogos = new LinkedHashMap<>();
        ConfigurationSection shops = config.getConfigurationSection("shops");
        if (shops == null) return catalogos;
        for (String shopId : shops.getKeys(false)) {
            String catalogo = shops.getString(shopId + ".catalogo");
            if (catalogo != null) catalogos.put(shopId, catalogo);
        }
        return catalogos;
    }

    public void setCatalogo(String shopId, String catalogo) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(tradesFile);
        config.set("shops." + shopId + ".catalogo", catalogo);
        try { config.save(tradesFile); } catch (IOException e) { plugin.getLogger().severe("Error guardando el catálogo: " + e.getMessage()); }
    }

    // Escribe los 10 tradeos que le tocan a ese aldeano en esa sección y, si está cargado, se los pone
    public void aplicarCatalogo(String shopId, CatalogoTienda.Tienda tienda, int seccion) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CatalogoTienda.Oferta oferta = i < tienda.ofertas().size() ? tienda.ofertas().get(i) : null;
            MerchantRecipe recipe = oferta != null && CatalogoTienda.abierta(oferta, seccion) ? tradeo(oferta, seccion) : null;
            if (recipe == null) {
                recipe = new MerchantRecipe(createEmptyTradeItem(), 9999);
                recipe.addIngredient(createEmptyTradeItem());
            }
            recipes.add(recipe);
        }
        saveShopTrades(shopId, recipes);
        Villager villager = getVillagerById(shopId);
        if (villager != null) villager.setRecipes(recipes);
    }

    // Precio de más de 64 DinoCoins: un stack en el primer lugar y el resto en el segundo
    private MerchantRecipe tradeo(CatalogoTienda.Oferta oferta, int seccion) {
        ItemStack producto = CustomItemRegistry.getCustomItem(oferta.producto(), oferta.cantidadProducto());
        int precio = CatalogoTienda.precio(oferta, seccion);
        ItemStack pago = CustomItemRegistry.getCustomItem(oferta.pago(), Math.min(64, precio));
        if (producto == null || pago == null) {
            plugin.getLogger().warning("Catálogo de la tienda: no existe el item " + (producto == null ? oferta.producto() : oferta.pago()));
            return null;
        }
        MerchantRecipe recipe = new MerchantRecipe(producto, 9999);
        recipe.addIngredient(pago);
        if (precio > 64) recipe.addIngredient(CustomItemRegistry.getCustomItem(oferta.pago(), precio - 64));
        return recipe;
    }

    public Villager getVillagerById(String shopId) {
        if (shopId == null) return null;
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