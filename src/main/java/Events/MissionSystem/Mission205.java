package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

// Si la herramienta ya se mejoró a Celestita también cuenta (la de Celestita solo sale de la floral)
public class Mission205 extends MisionCrafteo {

    public Mission205(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 205, "Herramientas florales", MissionDifficulty.DIFICIL, 20,
                "Forja la espada, el hacha, el pico y la pala florales en la herrería, con una Mejora Floral cada una.");
        objetivo("espada", "Espada Floral", id("espada_floral", "espada_celestita"));
        objetivo("hacha", "Hacha Floral", id("hacha_floral", "hacha_celestita"));
        objetivo("pico", "Pico Floral", id("pico_floral", "pico_celestita"));
        objetivo("pala", "Pala Floral", id("pala_floral", "pala_celestita"));
        extraOf(12);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1), item(Material.HONEY_BLOCK, 8));
    }
}
