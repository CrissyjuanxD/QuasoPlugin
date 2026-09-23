package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

import static Events.MissionSystem.MissionRewards.*;

public class Mission80 extends BaseMission {
    private static final Set<String> BOSSES = Set.of("ender_dragon", "wither", "elder_guardian");

    public Mission80(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 80, "Duro de matar", MissionDifficulty.MUY_DIFICIL, 23,
                "Mata al Ender Dragon, un Wither o un Elder Guardian sin armadura.");
        flag("jefe", "Jefe sin armadura");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 1), custom("splash_resistance_3", 3));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!BOSSES.contains(event.getBossId())) return;
        for (Player player : event.getPlayers()) {
            if (MissionUtils.noArmor(player)) mark(player, "jefe");
        }
    }
}
