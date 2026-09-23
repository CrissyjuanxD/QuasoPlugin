package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission118 extends BabyNameMission {

    public Mission118(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 118, "Guardería", MissionDifficulty.MEDIA, 14,
                "Ten 10 mobs bebé distintos con Golden Dandelion y nombre.", 10);
        extraOf(80);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NAME_TAG, 10), item(Material.ENCHANTED_GOLDEN_APPLE, 2));
    }
}
