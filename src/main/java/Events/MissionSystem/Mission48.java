package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission48 extends BaseMission {

    public Mission48(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 48, "Elite Infernal", MissionDifficulty.DIFICIL, 19,
                "Mata 40 Elite Wither Skeletons y 40 Elite Piglins.");
        counter("wither_skeletons", "Elite Wither Skeletons", 40);
        counter("piglins", "Elite Piglins", 40);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.SHARPNESS, 7), custom("panic_apple", 8));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = entity instanceof WitherSkeleton ? "wither_skeletons" : entity instanceof PiglinAbstract ? "piglins" : null;
        if (key == null || !MissionUtils.isElite(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
