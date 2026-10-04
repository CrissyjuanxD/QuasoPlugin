package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission123 extends BaseMission {

    public Mission123(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 123, "Domador", MissionDifficulty.FACIL, 11,
                "Doma un lobo y un gato.");
        flag("lobo", "Lobo");
        flag("gato", "Gato");
        extraOf(7);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NAME_TAG, 2), item(Material.BONE, 32));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        switch (event.getEntityType()) {
            case WOLF -> mark(player, "lobo");
            case CAT -> mark(player, "gato");
            default -> { }
        }
    }
}
