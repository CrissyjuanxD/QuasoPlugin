package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission105 extends BabyNameMission {

    public Mission105(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 105, "Para siempre bebé", MissionDifficulty.FACIL, 11,
                "Usa Golden Dandelions en crías de 5 mobs distintos y ponles nombre.", 5);
        extraOf(13);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NAME_TAG, 5), item(Material.GOLDEN_APPLE, 5));
    }
}
