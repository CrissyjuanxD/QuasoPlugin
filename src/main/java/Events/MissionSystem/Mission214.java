package Events.MissionSystem;

import BloodMoon.BloodMoonMobs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission214 extends BaseMission {

    public Mission214(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 214, "Cazador de sangre", MissionDifficulty.MEDIA, 16,
                "Mata 30 mobs de BloodMoon (los de nombre rojo). Se suman entre noches.");
        counter("mobs", "Mobs de BloodMoon", 30);
        extraOf(58);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("blood_fragment", 5), custom("splash_regeneration_3", 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!BloodMoonMobs.es(event.getEntity())) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "mobs", 1);
    }
}
