package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission76 extends BaseMission {

    public Mission76(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 76, "Flechas peligrosas", MissionDifficulty.DIFICIL, 19,
                "Mata 500 Infested Skeletons.");
        counter("skeletons", "Infested Skeletons", 500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_skeleton", 1), item(Material.SPECTRAL_ARROW, 64));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.INFESTED_SKELETON)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "skeletons", 1);
    }
}
