package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission139 extends BaseMission {

    public Mission139(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 139, "Decorador", MissionDifficulty.FACIL, 10,
                "Cuelga 10 cuadros.");
        counter("cuadros", "Cuadros colgados", 10);
        extraOf(56);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ITEM_FRAME, 8), item(Material.GLOW_ITEM_FRAME, 4));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHang(HangingPlaceEvent event) {
        if (event.getEntity() instanceof Painting && event.getPlayer() != null) add(event.getPlayer(), "cuadros", 1);
    }
}
