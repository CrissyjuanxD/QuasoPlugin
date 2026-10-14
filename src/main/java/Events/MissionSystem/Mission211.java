package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission211 extends MisionCrafteo {

    public Mission211(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 211, "El ojo del rey", MissionDifficulty.MEDIA, 15,
                "Fabrica el Ojo del Rey Ender: 4 Esencias Marchitas, 2 Fragmentos Astrales, 2 Cristales de Celestita "
                        + "y un Ojo de Ender. Con él se invoca al Rey Ender.");
        objetivo("ojo", "Ojo del Rey Ender", id("ojo_rey_ender"));
        extraOf(54);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENDER_PEARL, 16), item(Material.GOLDEN_APPLE, 8));
    }
}
