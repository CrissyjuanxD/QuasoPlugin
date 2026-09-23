package Events.MissionSystem;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission85 extends BaseMission {

    public Mission85(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 85, "Misión imposible", MissionDifficulty.MUY_DIFICIL, 24,
                "Pasa 100 días de Minecraft sin morir (unas 33 horas de juego).");
        counter("dias", "Días sin morir", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 2), custom("amuleto_inmortalidad", 1));
    }

    // Minecraft cuenta el tiempo desde la última muerte; un día son 24.000 ticks
    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        return Math.min(objective.target(), player.getStatistic(Statistic.TIME_SINCE_DEATH) / 24000);
    }

    @Override
    protected int tickSeconds() {
        return 30;
    }

    @Override
    protected void tick(Player player) {
        check(player);
    }
}
