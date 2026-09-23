package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission25 extends BaseMission {

    public Mission25(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 25, "¡Parece imposible!", MissionDifficulty.MUY_DIFICIL, 25,
                "Derrota al Ultra Warden.");
        flag("jefe", "Ultra Warden derrotado");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    // El jefe tiene que llevar la PDC boss_id = ultra_warden
    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("ultra_warden")) return;
        for (Player player : event.getPlayers()) mark(player, "jefe");
    }
}
