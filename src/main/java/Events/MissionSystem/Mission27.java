package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission27 extends BiomeHuntMission {

    public Mission27(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 27, "Ecos del Cian", 18,
                "En el bioma Cian, mata 50 Infested Skeletons y mina 16 Minerales Profundos Cian.",
                MissionUtils.INFESTED_SKELETON, "Infested Skeletons", WardenBiome.CAVERNA_SCULK, "Minerales Cian");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_skeleton", 1), custom("potion_resistance_2", 2));
    }
}
