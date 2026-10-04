package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission136 extends BaseMission {

    public Mission136(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 136, "Cazador de insectos", MissionDifficulty.FACIL, 12,
                "Mata 20 Ender Insects.");
        counter("insectos", "Ender Insects", 20);
        extraOf(45);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.MILK_BUCKET, 3), item(Material.GOLDEN_APPLE, 3));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), "ender_insect")) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "insectos", 1);
    }
}
