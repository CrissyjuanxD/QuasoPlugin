package Events.MissionSystem;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission83 extends BaseMission {

    public Mission83(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 83, "Noche sin miedo", MissionDifficulty.MUY_DIFICIL, 21,
                "Mata 50 mobs en una misma BloodMoon sin armadura.");
        counter("mobs", "Mobs sin armadura", 50);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_absorption_10", 3), custom("corrupted_golden_apple", 1));
    }

    // Tienen que ser en la misma BloodMoon: si empieza otra, el contador arranca de nuevo
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer == null || !MissionUtils.noArmor(killer) || !MissionUtils.isBloodMoon(killer.getWorld()) || !tracking(killer)) return;

        MissionData data = data(killer);
        int night = (int) MissionUtils.dayId(killer.getWorld());
        if (data.getProgressValue("noche") == null || data.getProgressInt("noche") != night) {
            data.setProgressValue("noche", night);
            data.setProgressValue("mobs", 0);
            save(killer, data);
        }
        add(killer, "mobs", 1);
    }
}
