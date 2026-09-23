package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission45 extends BaseMission {

    public Mission45(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 45, "Elite Profesional", MissionDifficulty.DIFICIL, 17,
                "Mata 40 Elite Endermans y 40 Elite Creepers.");
        counter("endermans", "Elite Endermans", 40);
        counter("creepers", "Elite Creepers", 40);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.SHARPNESS, 6), item(Material.GOLDEN_APPLE, 15));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = entity instanceof Enderman ? "endermans" : entity instanceof Creeper ? "creepers" : null;
        if (key == null || !MissionUtils.isElite(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
