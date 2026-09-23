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

public class Mission16 extends BaseMission {

    public Mission16(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 16, "Jugando a ser músico", MissionDifficulty.MEDIA, 16,
                "Rompe 25 Sculk Shriekers en el Deep Dark.");
        counter("chilladores", "Chilladores", 25);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_2", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 3));
    }

    // Mientras cuenta no sueltan nada, así no se pueden volver a poner
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SCULK_SHRIEKER || !block.getBiome().equals(Biome.DEEP_DARK)) return;
        if (!tracking(event.getPlayer())) return;
        event.setDropItems(false);
        add(event.getPlayer(), "chilladores", 1);
    }
}
