package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission209 extends MisionCrafteo {

    public Mission209(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 209, "Herramientas de celestita", MissionDifficulty.DIFICIL, 20,
                "Forja el pico, la pala y la azada de Celestita: Plantilla de Celestita, la herramienta floral y un "
                        + "Lingote de Celestita en la herrería.");
        objetivo("pico", "Pico de Celestita", id("pico_celestita"));
        objetivo("pala", "Pala de Celestita", id("pala_celestita"));
        objetivo("azada", "Azada de Celestita", id("azada_celestita"));
        extraOf(35);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENDER_EYE, 8), item(Material.GOLD_INGOT, 16));
    }
}
