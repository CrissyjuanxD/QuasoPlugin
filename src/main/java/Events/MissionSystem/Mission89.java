package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission89 extends BaseMission {

    public Mission89(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 89, "Larga vida al Rey", MissionDifficulty.MUY_DIFICIL, 24,
                "Mata al Rey Ender 3 veces.");
        counter("reyes", "Rey Ender", 3);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_5", 1), custom("doubletotem", 1));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("rey_ender")) return;
        for (Player player : event.getPlayers()) add(player, "reyes", 1);
    }
}
