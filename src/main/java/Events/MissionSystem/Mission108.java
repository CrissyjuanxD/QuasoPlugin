package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission108 extends BaseMission {

    public Mission108(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 108, "Nautilus zombi", MissionDifficulty.MEDIA, 15,
                "Mata 15 Zombie Nautilus, los que salen con su drowned encima.");
        counter("nautilus", "Zombie Nautilus", 15);
        extraOf(22);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.LOYALTY, 3), item(Material.DIAMOND_NAUTILUS_ARMOR, 1));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ZOMBIE_NAUTILUS) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "nautilus", 1);
    }
}
