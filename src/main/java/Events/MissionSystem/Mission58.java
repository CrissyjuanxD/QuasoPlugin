package Events.MissionSystem;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission58 extends BaseMission {

    public Mission58(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 58, "Soy muy fuerte", MissionDifficulty.DIFICIL, 18,
                "Mata 500 mobs en BloodMoons (se suman entre noches).");
        counter("mobs", "Mobs en BloodMoon", 500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("corrupted_golden_apple", 2), potion(PotionType.STRONG_STRENGTH, 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null && MissionUtils.isBloodMoon(killer.getWorld())) add(killer, "mobs", 1);
    }
}
