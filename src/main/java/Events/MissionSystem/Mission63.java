package Events.MissionSystem;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission63 extends BaseMission {

    public Mission63(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 63, "Gatillo fácil", MissionDifficulty.DIFICIL, 17,
                "Mata 100 mobs con la Warden Gun.");
        counter("mobs", "Mobs con la Warden Gun", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("energia_warden", 4), custom("artefacto_nivel_1", 1));
    }

    // La Warden Gun se reconoce por su id de item custom (warden_gun)
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null && MissionUtils.isWardenGun(killer.getInventory().getItemInMainHand())) add(killer, "mobs", 1);
    }
}
