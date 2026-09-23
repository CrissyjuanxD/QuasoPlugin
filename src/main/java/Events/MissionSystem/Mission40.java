package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission40 extends BaseMission {

    public Mission40(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 40, "Los endermans no pueden contra mí", MissionDifficulty.DIFICIL, 18,
                "Mata 100 Endermans con una espada de madera.");
        counter("endermans", "Endermans", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("perla_infinita", 1), custom("splash_resistance_3", 1));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDERMAN) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null && killer.getInventory().getItemInMainHand().getType() == Material.WOODEN_SWORD) {
            add(killer, "endermans", 1);
        }
    }
}
