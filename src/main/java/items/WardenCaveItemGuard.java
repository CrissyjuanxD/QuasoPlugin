package items;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class WardenCaveItemGuard implements Listener {

    // Los items de la Warden Cave son cristales, sílex o cobre por debajo; así no se gastan en recetas vanilla.
    // Las recetas del plugin (las de TwoChanges) no se tocan
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!isVanilla(event.getRecipe())) return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (WardenCaveItems.isWardenCaveItem(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!isVanilla(event.getRecipe())) return;
        if (!(event.getBlock().getState() instanceof Crafter crafter)) return;
        for (ItemStack item : crafter.getInventory().getContents()) {
            if (WardenCaveItems.isWardenCaveItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Ningún item de la Warden Cave se puede fundir con una receta vanilla; las del plugin (alto horno) sí
    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (WardenCaveItems.isWardenCaveItem(event.getSource()) && isVanilla(event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    private boolean isVanilla(Recipe recipe) {
        return recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(NamespacedKey.MINECRAFT);
    }
}
