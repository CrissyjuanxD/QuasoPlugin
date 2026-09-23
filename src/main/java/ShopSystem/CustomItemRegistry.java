package ShopSystem;

import Managers.ItemManager;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItemRegistry {

    private static ItemManager itemManager;

    public static void init(QuasoPlugin pl, ItemManager manager) {
        itemManager = manager;
    }

    // Busca primero un item custom del plugin y si no existe lo trata como material vanilla
    public static ItemStack getCustomItem(String name, int amount) {
        ItemStack item = itemManager.getItem(name, amount, null);
        if (item != null) return item;
        try {
            return new ItemStack(Material.valueOf(name.toUpperCase()), amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<String> getAllCustomNames() {
        return itemManager.getRegisteredItems();
    }
}