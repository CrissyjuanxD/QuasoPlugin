package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission210 extends MisionCrafteo {

    public Mission210(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 210, "Armas de celestita", MissionDifficulty.DIFICIL, 20,
                "Forja la espada, el hacha y la lanza de Celestita. La espada le pega el doble a los mobs del End.");
        objetivo("espada", "Espada de Celestita", id("espada_celestita"));
        objetivo("hacha", "Hacha de Celestita", id("hacha_celestita"));
        objetivo("lanza", "Lanza de Celestita", id("lanza_celestita"));
        extraOf(35);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENDER_EYE, 8), item(Material.GOLDEN_APPLE, 8));
    }
}
