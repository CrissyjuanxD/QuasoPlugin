package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission50 extends BaseMission {

    public Mission50(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 50, "Día del Omega", MissionDifficulty.DIFICIL, 20,
                "Mata al prieto de Crosszy.");
        flag("crosszy", "Crosszy eliminado");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(crosszyHead(), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (victim.getName().equalsIgnoreCase("Crosszy") && killer != null && !killer.equals(victim)) {
            mark(killer, "crosszy");
        }
    }
}
