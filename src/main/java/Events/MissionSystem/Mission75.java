package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission75 extends BaseMission {

    public Mission75(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 75, "Warden Hunter", MissionDifficulty.MUY_DIFICIL, 23,
                "Mata 200 Wardens.");
        counter("wardens", "Wardens", 200);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("amuleto_invisiblidad", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "wardens", 1);
    }
}
