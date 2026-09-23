package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission72 extends BaseMission {

    public Mission72(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 72, "Veneno mortal", MissionDifficulty.DIFICIL, 19,
                "Mata 500 Infested Cave Spiders.");
        counter("aranas", "Infested Cave Spiders", 500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("alma_infested_cave_spider", 1), custom("splash_regeneration_3", 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.INFESTED_CAVE_SPIDER)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "aranas", 1);
    }
}
