package Events.MissionSystem;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission17 extends BaseMission {

    public Mission17(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 17, "Cazador de Corruptos", MissionDifficulty.DIFICIL, 18,
                "Mata 25 Corrupted Zombies y 25 Corrupted Spiders. Salen en oleadas en las raids.");
        counter("zombies", "Corrupted Zombies", 25);
        counter("aranas", "Corrupted Spiders", 25);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("corrupted_steak", 32), custom("potion_haste_2", 3));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = null;
        if (entity instanceof Zombie && MissionUtils.isMob(entity, MissionUtils.CORRUPTED_ZOMBIE)) key = "zombies";
        else if (entity instanceof Spider && MissionUtils.isMob(entity, MissionUtils.CORRUPTED_SPIDER)) key = "aranas";
        if (key == null) return;

        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
