package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission203 extends MisionCrafteo {

    public Mission203(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 203, "Cocina corrupta", MissionDifficulty.FACIL, 10,
                "Fabrica una Carne Corrupta: un filete cocido rodeado de 4 carnes podridas.");
        objetivo("carne", "Carne Corrupta", item -> item.getType() == Material.COOKED_BEEF && item.hasItemMeta()
                && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2);
        extraOf(8);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.COOKED_BEEF, 32), custom("tarta_calabaza_mejorada", 8));
    }
}
