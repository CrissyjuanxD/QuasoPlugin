package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission55 extends BaseMission {

    public Mission55(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 55, "Rey de los Endermans", MissionDifficulty.MUY_DIFICIL, 25,
                "Derrota al Rey Ender.");
        flag("rey", "Rey Ender derrotado");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("amuleto_invisiblidad", 1), custom("doubletotem", 1));
    }

    // El jefe tiene que llevar la PDC boss_id = rey_ender
    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("rey_ender")) return;
        for (Player player : event.getPlayers()) mark(player, "rey");
    }
}
