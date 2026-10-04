package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static Events.MissionSystem.MissionRewards.*;

public class Mission129 extends BaseMission {
    private static final Set<Material> TORCHES = EnumSet.of(
            Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
            Material.COPPER_TORCH, Material.COPPER_WALL_TORCH
    );

    public Mission129(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 129, "Luz en la oscuridad", MissionDifficulty.FACIL, 11,
                "Coloca 30 antorchas en la Warden Cave.");
        counter("antorchas", "Antorchas colocadas", 30);
        extraOf(24);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.TORCH, 64), item(Material.LANTERN, 8));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!MissionUtils.inWardenCave(event.getPlayer())) return;
        if (TORCHES.contains(event.getBlockPlaced().getType())) add(event.getPlayer(), "antorchas", 1);
    }
}
