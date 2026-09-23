package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission74 extends BaseMission {

    public Mission74(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 74, "El dragón otra vez", MissionDifficulty.DIFICIL, 20,
                "Mata al Ender Dragon 3 veces.");
        counter("dragones", "Ender Dragons", 3);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ELYTRA, 1), item(Material.FIREWORK_ROCKET, 64));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("ender_dragon")) return;
        for (Player player : event.getPlayers()) add(player, "dragones", 1);
    }
}
