package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission7 extends BaseMission {

    public Mission7(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 7, "Elite Superior", MissionDifficulty.MEDIA, 15,
                "Mata 25 Elite Spiders y 25 Elite Skeletons.");
        counter("spiders", "Elite Spiders", 25);
        counter("skeletons", "Elite Skeletons", 25);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_regeneration_3", 3), item(Material.DIAMOND_BLOCK, 8));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = entity instanceof Spider ? "spiders" : entity instanceof AbstractSkeleton ? "skeletons" : null;
        if (key == null || !MissionUtils.isElite(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
