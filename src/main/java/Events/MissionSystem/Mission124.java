package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission124 extends BaseMission {

    public Mission124(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 124, "Ganadero", MissionDifficulty.FACIL, 10,
                "Cría 15 animales.");
        counter("crias", "Animales criados", 15);
        extraOf(10);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.WHEAT, 64), item(Material.LEAD, 4));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) add(player, "crias", 1);
    }
}
