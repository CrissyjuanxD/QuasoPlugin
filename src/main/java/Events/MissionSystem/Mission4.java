package Events.MissionSystem;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission4 extends BaseMission {

    public Mission4(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 4, "Vecino del Happy Ghast", MissionDifficulty.FACIL, 12,
                "Vuela 1.000 bloques montado en un Happy Ghast.");
        counter("bloques", "Bloques volados", 1000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("happy_ghast_enchant", 1), custom("flytotem", 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Minecraft ya cuenta los centímetros recorridos en Happy Ghast
    @Override
    protected void tick(Player player) {
        statSince(player, "bloques", Statistic.HAPPY_GHAST_ONE_CM, 100);
    }
}
