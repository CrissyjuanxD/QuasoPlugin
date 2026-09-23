package Events.MissionSystem;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission90 extends BaseMission {

    public Mission90(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 90, "Viajero incansable", MissionDifficulty.MEDIA, 15,
                "Recorre 100.000 bloques en total, de cualquier forma.");
        counter("bloques", "Bloques recorridos", 100000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("happy_ghast_enchant_2", 1), custom("gancho", 1));
    }

    @Override
    protected int tickSeconds() {
        return 10;
    }

    // Suma todas las estadísticas de distancia de Minecraft (caminar, nadar, volar, monturas...) menos las caídas
    @Override
    protected void tick(Player player) {
        long centimeters = 0;
        for (Statistic statistic : Statistic.values()) {
            if (statistic.name().endsWith("_ONE_CM") && statistic != Statistic.FALL_ONE_CM) {
                centimeters += player.getStatistic(statistic);
            }
        }
        int blocks = (int) (centimeters / 100);

        MissionData data = data(player);
        if (data.getProgressValue("base_bloques") == null) {
            data.setProgressValue("base_bloques", blocks);
            save(player, data);
        }
        set(player, "bloques", blocks - data.getProgressInt("base_bloques"));
    }
}
