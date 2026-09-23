package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission100 extends BaseMission {

    public Mission100(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 100, "Jugador Experto", MissionDifficulty.MUY_DIFICIL, 25,
                "Completa 95 de las 99 misiones anteriores. Da el rol DinoLeyenda.");
        counter("misiones", "Misiones de la 1 a la 99", 95);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENCHANTED_GOLDEN_APPLE, 20), custom("dinofichas", 60));
    }

    // Las extras (101 a 119) no cuentan; el rol lo da MissionHandler al completarla
    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        int completed = 0;
        for (int number = 1; number <= 99; number++) {
            if (handler.isMissionCompleted(player, number)) completed++;
        }
        return Math.min(objective.target(), completed);
    }

    @Override
    protected int tickSeconds() {
        return 10;
    }

    @Override
    protected void tick(Player player) {
        check(player);
    }
}
