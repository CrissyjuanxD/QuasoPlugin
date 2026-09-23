package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission62 extends BaseMission {

    public Mission62(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 62, "Élite Experto", MissionDifficulty.MUY_DIFICIL, 21,
                "Mata 400 Elite Zombies y 400 Elite Spiders.");
        counter("zombies", "Elite Zombies", 400);
        counter("spiders", "Elite Spiders", 400);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.SHARPNESS, 8), item(Material.GOLDEN_APPLE, 15));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = entity instanceof Zombie ? "zombies" : entity instanceof Spider ? "spiders" : null;
        if (key == null || !MissionUtils.isElite(entity)) return;
        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
