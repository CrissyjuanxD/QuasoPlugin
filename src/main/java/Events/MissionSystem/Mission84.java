package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Raid;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission84 extends BaseMission {

    public Mission84(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 84, "Raid de élite", MissionDifficulty.DIFICIL, 19,
                "Gana una Raid con Mal Presagio V.");
        flag("raid", "Raid con Mal Presagio V");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("icetotem", 1), item(Material.TOTEM_OF_UNDYING, 3));
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        Raid raid = event.getRaid();
        if (raid.getStatus() != Raid.RaidStatus.VICTORY || raid.getBadOmenLevel() < 5) return;
        for (Player player : event.getWinners()) mark(player, "raid");
    }
}
