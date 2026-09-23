package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission93 extends BaseMission {

    public Mission93(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 93, "Trabajo duro", MissionDifficulty.MUY_DIFICIL, 24,
                "Rompe 300.000 bloques y coloca 300.000.");
        counter("rotos", "Bloques rotos", 300000);
        counter("colocados", "Bloques colocados", 300000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BEACON, 2), custom("potion_haste_3", 3));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        add(event.getPlayer(), "rotos", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        add(event.getPlayer(), "colocados", 1);
    }
}
