package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission134 extends BaseMission {

    public Mission134(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 134, "Fruta del vacío", MissionDifficulty.FACIL, 11,
                "Come 10 frutas de chorus.");
        counter("frutas", "Frutas de chorus", 10);
        extraOf(39);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.PURPUR_BLOCK, 32), item(Material.CHORUS_FLOWER, 4));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT) add(event.getPlayer(), "frutas", 1);
    }
}
