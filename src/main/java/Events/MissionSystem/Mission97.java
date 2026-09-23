package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission97 extends BaseMission {

    public Mission97(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 97, "Sin vida social", MissionDifficulty.MUY_DIFICIL, 25,
                "Juega 400 horas en el server.");
        counter("horas", "Horas jugadas", 400);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BEACON, 2), item(Material.DIAMOND_BLOCK, 32));
    }

    // El tiempo jugado sale de la estadística de Minecraft (72.000 ticks = 1 hora)
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
