package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission30 extends BiomeHuntMission {

    public Mission30(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 30, "Ceniza y pólvora", 19,
                "En el bioma Gris, mata 50 Infested Creepers y mina 16 Minerales Profundos Grises.",
                MissionUtils.INFESTED_CREEPER, "Infested Creepers", WardenBiome.RUINAS_DE_CENIZA, "Minerales Grises");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_creeper", 1), custom("splash_resistance_3", 1));
    }
}
