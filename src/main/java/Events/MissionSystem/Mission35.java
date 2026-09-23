package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission35 extends BaseMission {

    public Mission35(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 35, "El fin del principio", MissionDifficulty.MUY_DIFICIL, 25,
                "Derrota al Ender Dragon modificado.");
        flag("dragon", "Ender Dragon derrotado");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.DRAGON_HEAD, 1), custom("splash_resistance_3", 3));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("ender_dragon")) return;
        for (Player player : event.getPlayers()) mark(player, "dragon");
    }
}
