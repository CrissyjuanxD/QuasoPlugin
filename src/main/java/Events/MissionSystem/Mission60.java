package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission60 extends BaseMission {

    public Mission60(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 60, "Qué explosivo", MissionDifficulty.DIFICIL, 19,
                "Mata 500 Infested Creepers.");
        counter("creepers", "Infested Creepers", 500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_creeper", 1), custom("splash_resistance_3", 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.INFESTED_CREEPER)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "creepers", 1);
    }
}
