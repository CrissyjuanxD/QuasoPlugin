package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission73 extends BaseMission {

    public Mission73(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 73, "Revancha", MissionDifficulty.MUY_DIFICIL, 22,
                "Mata al Ultra Warden 3 veces.");
        counter("ultra_warden", "Ultra Warden", 3);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("energia_warden", 6), custom("artefacto_nivel_2", 1));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("ultra_warden")) return;
        for (Player player : event.getPlayers()) add(player, "ultra_warden", 1);
    }
}
