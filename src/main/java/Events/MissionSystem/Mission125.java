package Events.MissionSystem;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission125 extends BaseMission {

    public Mission125(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 125, "Apicultor", MissionDifficulty.FACIL, 11,
                "Esquila 3 colmenas o nidos de abejas con tijeras.");
        counter("colmenas", "Colmenas esquiladas", 3);
        extraOf(12);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.HONEY_BOTTLE, 8), item(Material.CAMPFIRE, 2));
    }

    // Paper avisa con este evento cuando se le saca el panal a una colmena llena
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearBlockEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.BEEHIVE || type == Material.BEE_NEST) add(event.getPlayer(), "colmenas", 1);
    }
}
