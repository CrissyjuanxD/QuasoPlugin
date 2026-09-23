package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission65 extends BaseMission {

    public Mission65(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 65, "Cuidado que vuela", MissionDifficulty.DIFICIL, 19,
                "Mata 500 Infested Ghasts.");
        counter("ghasts", "Infested Ghasts", 500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_ghast", 1), custom("potion_slow_falling", 3));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.INFESTED_GHAST)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "ghasts", 1);
    }
}
