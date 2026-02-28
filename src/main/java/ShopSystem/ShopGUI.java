package ShopSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI {

    private final ShopManager shopManager;

    public ShopGUI(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void openConfigGUI(Player player, Villager villager) {
        String shopId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
        String title = ChatColor.GOLD + "Configurar: " + (villager.getCustomName() != null ? villager.getCustomName() : "Tienda");

        if (title.length() > 32) title = title.substring(0, 32);

        Inventory gui = Bukkit.createInventory(null, 54, title);
        shopManager.editingShops.put(player.getUniqueId(), shopId);
        shopManager.loadShopTrades(shopId, villager);

        ItemStack pane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);

        for (int i = 0; i < 54; i++) {
            if (i % 9 == 4) gui.setItem(i, pane);
        }

        List<MerchantRecipe> recipes = villager.getRecipes();

        for (int i = 0; i < 12; i++) {
            int row = (i < 6) ? i : (i - 6);
            int baseSlot = (row * 9) + ((i < 6) ? 0 : 5);

            MerchantRecipe recipe = (i < recipes.size()) ? recipes.get(i) : null;
            ItemStack ing1 = null, ing2 = null, res = null;

            if (recipe != null) {
                if (recipe.getIngredients().size() > 0) ing1 = recipe.getIngredients().get(0);
                if (recipe.getIngredients().size() > 1) ing2 = recipe.getIngredients().get(1);
                res = recipe.getResult();
            }

            gui.setItem(baseSlot, createDisplayItem(ing1, "Ingrediente 1", i));
            gui.setItem(baseSlot + 1, createDisplayItem(ing2, "Ingrediente 2", i));
            gui.setItem(baseSlot + 2, createDisplayItem(res, "Resultado", i));
        }

        player.openInventory(gui);
    }

    private ItemStack createDisplayItem(ItemStack item, String type, int tradeIndex) {
        boolean isPlaceholder = false;

        // Detecta el "Papel Vacío" igual que en tu código viejo
        if (item == null || item.getType() == Material.AIR ||
                (item.getType() == Material.PAPER && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 100)) {
            isPlaceholder = true;
        }

        ItemStack displayItem;
        if (isPlaceholder) {
            if (type.equals("Resultado")) {
                displayItem = new ItemStack(Material.PURPLE_DYE);
            } else {
                displayItem = new ItemStack(Material.PAPER);
            }
        } else {
            displayItem = item.clone();
        }

        ItemMeta meta = displayItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());

        lore.add("");
        lore.add(ChatColor.GOLD + "Rol: " + ChatColor.YELLOW + type);
        lore.add(ChatColor.GOLD + "Tradeo #: " + ChatColor.YELLOW + (tradeIndex + 1));
        lore.add("");
        lore.add(ChatColor.GREEN + "Click para seleccionar y editar.");
        lore.add(ChatColor.AQUA + "Luego usa: /trade <item> <cantidad>");
        lore.add(ChatColor.AQUA + "O Shift+Click Derecho al aldeano con un item.");
        lore.add(ChatColor.RED + "Click Derecho aquí para borrar slot.");

        meta.setLore(lore);

        String displayName;
        if (isPlaceholder) {
            displayName = ChatColor.WHITE + "Vacío (Configurar)";
        } else {
            displayName = ChatColor.WHITE + getNiceName(displayItem);
        }

        meta.setDisplayName(displayName);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private String getNiceName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            return item.getItemMeta().getDisplayName();
        return item.getType().name();
    }
}