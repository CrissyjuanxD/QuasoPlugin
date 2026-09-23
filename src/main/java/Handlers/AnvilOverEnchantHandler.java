package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class AnvilOverEnchantHandler implements Listener {

    private final JavaPlugin plugin;

    public AnvilOverEnchantHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);

        if (firstItem == null || secondItem == null) return;

        if (secondItem.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) secondItem.getItemMeta();
            if (bookMeta == null || !bookMeta.hasStoredEnchants()) return;

            ItemStack resultItem = event.getResult();
            // Si el yunque por defecto no da resultado (por ej. coste alto o enchants incompatibles en vanilla), clonamos el primero
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                resultItem = firstItem.clone();
            }

            ItemMeta resultMeta = resultItem.getItemMeta();
            if (resultMeta == null) return;

            boolean hasOverEnchant = false;
            boolean isResultBook = resultItem.getType() == Material.ENCHANTED_BOOK;

            for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                Enchantment enchant = entry.getKey();
                int bookLevel = entry.getValue();

                // Lógica separada para evitar el bug visual de enchants duplicados
                if (isResultBook) {
                    EnchantmentStorageMeta resultStorageMeta = (EnchantmentStorageMeta) resultMeta;
                    int currentLevel = resultStorageMeta.getStoredEnchantLevel(enchant);

                    if (bookLevel > enchant.getMaxLevel() || bookLevel > currentLevel) {
                        resultStorageMeta.addStoredEnchant(enchant, bookLevel, true);
                        hasOverEnchant = true;
                    }
                } else {
                    int currentLevel = resultItem.getEnchantmentLevel(enchant);

                    if (bookLevel > enchant.getMaxLevel() || bookLevel > currentLevel) {
                        resultMeta.addEnchant(enchant, bookLevel, true);
                        hasOverEnchant = true;
                    }
                }
            }

            if (hasOverEnchant) {
                resultItem.setItemMeta(resultMeta);
                event.setResult(resultItem);

                // Forzar el costo de reparación
                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> event.getView().setRepairCost(12)
                );
            }
        }
    }
}