package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission207 extends MisionCrafteo {

    public Mission207(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 207, "Armadura de Warden", MissionDifficulty.DIFICIL, 20,
                "Forja las 4 piezas de la armadura de Warden: cada mejora de Warden, la pieza floral y un Lingote "
                        + "Profundo en la herrería.");
        objetivo("casco", "Casco de Warden", warden(Material.NETHERITE_HELMET));
        objetivo("peto", "Peto de Warden", warden(Material.NETHERITE_CHESTPLATE));
        objetivo("pantalon", "Pantalón de Warden", warden(Material.NETHERITE_LEGGINGS));
        objetivo("botas", "Botas de Warden", warden(Material.NETHERITE_BOOTS));
        extraOf(20);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ECHO_SHARD, 8), item(Material.GOLD_INGOT, 16));
    }
}
