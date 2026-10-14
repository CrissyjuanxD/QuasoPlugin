package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

// Si la pieza ya se mejoró a Warden también cuenta (la de Warden solo sale de la floral)
public class Mission204 extends MisionCrafteo {

    public Mission204(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 204, "Armadura floral", MissionDifficulty.DIFICIL, 20,
                "Forja las 4 piezas de la armadura floral en la herrería: Mejora Floral, la pieza de Netherite y un "
                        + "Bloque de Miel.");
        objetivo("casco", "Casco Floral", id("casco_floral").or(warden(Material.NETHERITE_HELMET)));
        objetivo("peto", "Peto Floral", id("peto_floral").or(warden(Material.NETHERITE_CHESTPLATE)));
        objetivo("pantalon", "Pantalón Floral", id("pantalon_floral").or(warden(Material.NETHERITE_LEGGINGS)));
        objetivo("botas", "Botas Florales", id("botas_floral").or(warden(Material.NETHERITE_BOOTS)));
        extraOf(12);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.HONEY_BLOCK, 8), item(Material.GOLDEN_APPLE, 8));
    }
}
