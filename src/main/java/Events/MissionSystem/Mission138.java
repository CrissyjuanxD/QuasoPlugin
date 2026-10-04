package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission138 extends BaseMission {

    public Mission138(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 138, "Constructor", MissionDifficulty.FACIL, 10,
                "Coloca 500 bloques.");
        counter("bloques", "Bloques colocados", 500);
        extraOf(53);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SCAFFOLDING, 32), item(Material.STONE_BRICKS, 64));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        add(event.getPlayer(), "bloques", 1);
    }
}
