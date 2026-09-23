package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission99 extends BaseMission {

    public Mission99(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 99, "No tengo amigos", MissionDifficulty.MUY_DIFICIL, 25,
                "Juega 800 horas en el server.");
        counter("horas", "Horas jugadas", 800);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BEACON, 3), item(Material.ENCHANTED_GOLDEN_APPLE, 20));
    }

    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        return Math.min(objective.target(), player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000);
    }

    @Override
    protected int tickSeconds() {
        return 60;
    }

    @Override
    protected void tick(Player player) {
        check(player);
    }
}
