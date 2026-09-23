package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission67 extends BaseMission {

    public Mission67(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 67, "Reina otra vez", MissionDifficulty.DIFICIL, 18,
                "Mata a la Abeja Reina 5 veces.");
        counter("reinas", "Abejas Reina", 5);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.UNBREAKING, 4), custom("frasco_de_velocidad", 8));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("abeja_reina")) return;
        for (Player player : event.getPlayers()) add(player, "reinas", 1);
    }
}
