package Events.MissionSystem;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission117 extends BaseMission {

    public Mission117(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 117, "Lancero legendario", MissionDifficulty.DIFICIL, 19,
                "Mata 500 mobs con lanzas.");
        counter("mobs", "Mobs con lanza", 500);
        extraOf(70);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(potion(PotionType.STRONG_STRENGTH, 3), custom("corrupted_golden_apple", 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null && MissionUtils.isSpear(killer.getInventory().getItemInMainHand())) add(killer, "mobs", 1);
    }
}
