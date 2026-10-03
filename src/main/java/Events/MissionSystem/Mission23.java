package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission23 extends BaseMission {

    public Mission23(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 23, "Cazador de Warden Zombies", MissionDifficulty.MEDIA, 15,
                "Mata 50 Warden Zombies.");
        counter("zombies", "Warden Zombies", 50);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_regeneration_3", 2), custom("corrupted_steak", 16));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.WARDEN_ZOMBIE)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "zombies", 1);
    }
}
