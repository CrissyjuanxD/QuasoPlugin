package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission47 extends BaseMission {

    public Mission47(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 47, "Jugando a ser Dios", MissionDifficulty.DIFICIL, 18,
                "Activa 8 Tótems de la Inmortalidad.");
        counter("totems", "Tótems activados", 8);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 2), book(Enchantment.PROTECTION, 5));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) add(player, "totems", 1);
    }
}
