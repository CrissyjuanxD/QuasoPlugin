package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission120 extends BaseMission {

    public Mission120(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 120, "Leñador", MissionDifficulty.FACIL, 10,
                "Tala 64 troncos de cualquier árbol.");
        counter("troncos", "Troncos talados", 64);
        extraOf(1);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(enchanted(Material.IRON_AXE, Enchantment.EFFICIENCY, 2, Enchantment.UNBREAKING, 1), item(Material.BONE_MEAL, 32));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (Tag.LOGS.isTagged(event.getBlock().getType())) add(event.getPlayer(), "troncos", 1);
    }
}
