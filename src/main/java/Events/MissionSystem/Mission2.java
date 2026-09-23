package Events.MissionSystem;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission2 extends BaseMission {

    public Mission2(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 2, "Cazador nocturno", MissionDifficulty.FACIL, 10,
                "Mata 50 mobs hostiles de noche.");
        counter("mobs", "Mobs de noche", 50);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_resistance_2", 1), custom("tarta_calabaza_mejorada", 8));
    }

    // Solo los monstruos del Overworld mientras es de noche
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (!MissionUtils.isNight(event.getEntity().getWorld())) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "mobs", 1);
    }
}
