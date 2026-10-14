package Handlers;

import items.ArcoMejorado;
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

    // Deja pasar encantamientos por encima del nivel máximo usando libros en el yunque, con coste fijo de 12. Solo en
    // items que aceptan ese encantamiento y sin chocar con los que ya tienen. El Arco Mejorado además acepta
    // Perforación y sube a Poder VI juntando Poder V con un libro de Poder V
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);

        if (firstItem == null || secondItem == null) return;

        if (secondItem.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) secondItem.getItemMeta();
            if (bookMeta == null || !bookMeta.hasStoredEnchants()) return;

            ItemStack resultItem = event.getResult();
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                resultItem = firstItem.clone();
            }

            ItemMeta resultMeta = resultItem.getItemMeta();
            if (resultMeta == null) return;

            boolean hasOverEnchant = false;
            boolean isResultBook = resultItem.getType() == Material.ENCHANTED_BOOK;
            boolean arcoMejorado = ArcoMejorado.es(firstItem);

            for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                Enchantment enchant = entry.getKey();
                int bookLevel = entry.getValue();

                if (isResultBook) {
                    EnchantmentStorageMeta resultStorageMeta = (EnchantmentStorageMeta) resultMeta;
                    int currentLevel = resultStorageMeta.getStoredEnchantLevel(enchant);

                    if (bookLevel > enchant.getMaxLevel() || bookLevel > currentLevel) {
                        resultStorageMeta.addStoredEnchant(enchant, bookLevel, true);
                        hasOverEnchant = true;
                    }
                } else {
                    boolean acepta = enchant.canEnchantItem(firstItem) || (arcoMejorado && enchant.equals(Enchantment.PIERCING));
                    if (!acepta || choca(resultMeta, enchant)) continue;

                    int currentLevel = resultMeta.getEnchantLevel(enchant);
                    int level = enchant.equals(Enchantment.POWER)
                            ? poder(currentLevel, bookLevel, ArcoMejorado.poderMaximo(firstItem), arcoMejorado)
                            : bookLevel;

                    if (level > currentLevel) {
                        resultMeta.addEnchant(enchant, level, true);
                        hasOverEnchant = true;
                    }
                }
            }

            if (hasOverEnchant) {
                resultItem.setItemMeta(resultMeta);
                event.setResult(resultItem);

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> event.getView().setRepairCost(12)
                );
            }
        }
    }

    // Choca con algún encantamiento que ya tiene (Infinidad con Reparación, Filo con Castigo...)
    private static boolean choca(ItemMeta meta, Enchantment enchant) {
        for (Enchantment existing : meta.getEnchants().keySet()) {
            if (!existing.equals(enchant) && existing.conflictsWith(enchant)) return true;
        }
        return false;
    }

    // Poder en un arco: dos iguales suben uno (como en el juego) y el tope es V, o VI en el Arco Mejorado
    static int poder(int actual, int libro, int maximo, boolean arcoMejorado) {
        int nivel = arcoMejorado && libro == actual ? actual + 1 : libro;
        return Math.min(nivel, maximo);
    }
}
