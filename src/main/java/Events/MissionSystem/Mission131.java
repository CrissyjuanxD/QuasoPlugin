package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission131 extends BaseMission {
    private static final List<String> INFESTED = List.of(
            MissionUtils.INFESTED_SKELETON, MissionUtils.INFESTED_CAVE_SPIDER, MissionUtils.INFESTED_GHAST,
            MissionUtils.INFESTED_CREEPER, MissionUtils.WARDEN_ZOMBIE
    );

    public Mission131(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 131, "Limpieza profunda", MissionDifficulty.FACIL, 12,
                "Mata 20 mobs infestados en la Warden Cave.");
        counter("infestados", "Mobs infestados", 20);
        extraOf(30);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ARROW, 64), item(Material.GOLDEN_APPLE, 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!MissionUtils.inWardenCave(entity) || !isInfested(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, "infestados", 1);
    }

    private boolean isInfested(LivingEntity entity) {
        for (String mob : INFESTED) {
            if (MissionUtils.isMob(entity, mob)) return true;
        }
        return false;
    }
}
