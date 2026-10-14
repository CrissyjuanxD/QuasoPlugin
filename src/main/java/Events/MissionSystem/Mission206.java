package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission206 extends MisionCrafteo {

    public Mission206(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 206, "Escudo floral", MissionDifficulty.MEDIA, 15,
                "Forja el Escudo Floral en la herrería: Mejora Floral, un escudo y un Bloque de Miel. Es irrompible.");
        objetivo("escudo", "Escudo Floral", id("escudo_floral"));
        extraOf(14);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.HONEY_BLOCK, 4), item(Material.GOLDEN_APPLE, 4));
    }
}
