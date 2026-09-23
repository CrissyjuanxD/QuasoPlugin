package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission111 extends BaseMission {

    public Mission111(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 111, "Cazador de cubos", MissionDifficulty.FACIL, 12,
                "Haz crecer un Sulfur Cube con bolas de slime y atrápalo en una cubeta.");
        flag("crecer", "Darle bolas de slime");
        flag("cubeta", "Atraparlo en una cubeta");
        extraOf(32);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SLIME_BLOCK, 8), custom("potion_slow_falling", 2));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeed(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof SulfurCube)) return;
        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());
        if (hand != null && hand.getType() == Material.SLIME_BALL) mark(event.getPlayer(), "crecer");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucket(PlayerBucketEntityEvent event) {
        if (event.getEntity() instanceof SulfurCube) mark(event.getPlayer(), "cubeta");
    }
}
