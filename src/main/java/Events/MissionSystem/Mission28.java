package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission28 extends BiomeHuntMission {

    public Mission28(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 28, "Veneno en el pantano", 18,
                "En el bioma Verde, mata 50 Infested Cave Spiders y mina 16 Minerales Profundos Verdes.",
                MissionUtils.INFESTED_CAVE_SPIDER, "Infested Cave Spiders", WardenBiome.PANTANO_PROFUNDO, "Minerales Verdes");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_cave_spider", 1), custom("splash_regeneration_3", 2));
    }
}
