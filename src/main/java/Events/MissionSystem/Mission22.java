package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission22 extends BaseMission {

    public Mission22(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 22, "Minero de las profundidades", MissionDifficulty.MEDIA, 15,
                "Mina 64 Minerales Profundos de cualquier color.");
        counter("minerales", "Minerales Profundos", 64);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_3", 2), custom("artefacto_nivel_1", 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!MissionUtils.inWardenCave(event.getPlayer())) return;
        if (WardenBiome.fromOre(event.getBlock().getType()) != null) add(event.getPlayer(), "minerales", 1);
    }
}
