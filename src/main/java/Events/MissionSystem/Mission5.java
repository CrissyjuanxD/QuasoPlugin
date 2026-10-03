package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission5 extends BaseMission {

    public Mission5(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 5, "Elite Principiante", MissionDifficulty.MEDIA, 13,
                "Mata 25 Elite Zombies.");
        counter("zombies", "Elite Zombies", 25);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("corrupted_steak", 16), item(Material.GOLDEN_APPLE, 10));
    }

    // En LOWEST para revisar si es Elite antes de que EliteMobs lo suelte
    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || !MissionUtils.isElite(zombie)) return;
        Player killer = MissionUtils.killer(zombie);
        if (killer != null) add(killer, "zombies", 1);
    }
}
