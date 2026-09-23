package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission44 extends BaseMission {

    public Mission44(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 44, "Estado en descomposición", MissionDifficulty.DIFICIL, 18,
                "Derrota 5 Withers.");
        counter("withers", "Withers", 5);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_3", 3), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("wither")) return;
        for (Player player : event.getPlayers()) add(player, "withers", 1);
    }
}
