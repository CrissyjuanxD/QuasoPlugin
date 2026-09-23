package Events.MissionSystem;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission82 extends BaseMission {

    public Mission82(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 82, "Señor de la Warden Cave", MissionDifficulty.MUY_DIFICIL, 22,
                "Mata 2.000 mobs en la Warden Cave.");
        counter("mobs", "Mobs en la Warden Cave", 2000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_2", 2), custom("energia_warden", 4));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob) || !MissionUtils.inWardenCave(event.getEntity())) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "mobs", 1);
    }
}
