package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission26 extends BaseMission {

    public Mission26(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 26, "Jugando a ser músico profesional", MissionDifficulty.MEDIA, 16,
                "Rompe 150 chilladores en la Warden Cave.");
        counter("chilladores", "Chilladores", 150);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_2", 1), custom("potion_haste_3", 2));
    }

    // Mientras cuenta no sueltan nada, así no se pueden volver a poner
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SCULK_SHRIEKER || !MissionUtils.inWardenCave(event.getPlayer())) return;
        if (!tracking(event.getPlayer())) return;
        event.setDropItems(false);
        add(event.getPlayer(), "chilladores", 1);
    }
}
