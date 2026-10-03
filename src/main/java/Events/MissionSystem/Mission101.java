package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission101 extends BaseMission {

    public Mission101(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 101, "Lancero", MissionDifficulty.FACIL, 11,
                "Mata 30 mobs con una lanza.");
        counter("mobs", "Mobs con lanza", 30);
        extraOf(2);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.LUNGE, 2), custom("corrupted_steak", 16));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null && MissionUtils.isSpear(killer.getInventory().getItemInMainHand())) add(killer, "mobs", 1);
    }
}
