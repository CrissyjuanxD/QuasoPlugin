package items;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class WardenCaveItemGuard implements Listener {

    // Los items de la Warden Cave y del End son cristales, sílex, tinte o cobre por debajo; así no se gastan en
    // recetas vanilla. Las recetas del plugin (las de TwoChanges y el Ojo del Rey Ender) no se tocan
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!isVanilla(event.getRecipe())) return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isCustom(item)) {
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
            if (isCustom(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Ningún item custom se puede fundir con una receta vanilla; las del plugin (alto horno) sí
    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (isCustom(event.getSource()) && isVanilla(event.getRecipe())) {
            event.setCancelled(true);
        }
    }

    // El Ojo del Rey Ender es un ojo de ender por debajo: no se tira ni se pone en un marco del portal
    @EventHandler
    public void onKingEye(PlayerInteractEvent event) {
        if (!"ojo_rey_ender".equals(EndItems.idOf(event.getItem()))) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        event.setCancelled(true);
    }

    private static boolean isCustom(ItemStack item) {
        return WardenCaveItems.isWardenCaveItem(item) || EndItems.isEndItem(item);
    }

    private boolean isVanilla(Recipe recipe) {
        return recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(NamespacedKey.MINECRAFT);
    }
}
