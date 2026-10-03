package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission70 extends BaseMission {

    public Mission70(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 70, "Élite Experto V2", MissionDifficulty.MUY_DIFICIL, 21,
                "Mata 400 Elite Skeletons y 400 Elite Creepers.");
        counter("skeletons", "Elite Skeletons", 400);
        counter("creepers", "Elite Creepers", 400);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.POWER, 6), item(Material.GOLDEN_APPLE, 15));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = entity instanceof AbstractSkeleton ? "skeletons" : entity instanceof Creeper ? "creepers" : null;
        if (key == null || !MissionUtils.isElite(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
