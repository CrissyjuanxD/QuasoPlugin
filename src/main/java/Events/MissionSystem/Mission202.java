package Events.MissionSystem;

import items.FloralMobItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission202 extends MisionCrafteo {

    public Mission202(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 202, "Botín floral", MissionDifficulty.FACIL, 11,
                "Consigue un drop floral: Carne Podrida, Ojo de Araña, Pólvora o Aguijón Floral. Los mobs florales "
                        + "salen en las raids y los sueltan al 10% (desde el día 12, al 40% con Saqueo).");
        objetivo("drop", "Drop floral", item -> FloralMobItems.dropId(item) != null);
        extraOf(2);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.GOLDEN_APPLE, 4), custom("corrupted_steak", 16));
    }
}
