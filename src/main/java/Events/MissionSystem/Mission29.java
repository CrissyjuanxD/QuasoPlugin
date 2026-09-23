package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission29 extends BiomeHuntMission {

    public Mission29(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 29, "Sobre el vacío", 19,
                "En el bioma Morado, mata 50 Infested Ghasts y mina 16 Minerales Profundos Morados.",
                MissionUtils.INFESTED_GHAST, "Infested Ghasts", WardenBiome.ABISMO_FLOTANTE, "Minerales Morados");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_ghast", 1), custom("potion_slow_falling", 3));
    }
}
