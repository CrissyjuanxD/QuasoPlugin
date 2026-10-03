package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission18 extends BaseMission {

    public Mission18(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 18, "Vida Opaca", MissionDifficulty.MEDIA, 14,
                "Rompe 25 Creaking Hearts en un Pale Garden de noche.");
        counter("corazones", "Creaking Hearts", 25);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_absorption_10", 3), custom("panic_apple", 2));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CREAKING_HEART || !block.getBiome().equals(Biome.PALE_GARDEN)) return;
        if (!MissionUtils.isNight(block.getWorld()) || !tracking(event.getPlayer())) return;
        event.setDropItems(false);
        add(event.getPlayer(), "corazones", 1);
    }
}
