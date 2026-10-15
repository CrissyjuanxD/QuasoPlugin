package Events.MissionSystem;

import BloodMoon.BloodMoonMobs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission216 extends BaseMission {

    public Mission216(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 216, "Herida abierta", MissionDifficulty.DIFICIL, 18,
                "Mata 10 mobs de BloodMoon mientras estás sangrando (el Sangrado que hacen sus golpes, flechas y "
                        + "charcos).");
        counter("mobs", "Mobs de BloodMoon sangrando", 10);
        extraOf(96);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_resistance_2", 2), item(Material.ENCHANTED_GOLDEN_APPLE, 2));
    }

    // Se mira el Sangrado del que lo mató en ese momento
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        BloodMoonMobs mobs = BloodMoonMobs.instancia();
        if (mobs == null || !BloodMoonMobs.es(event.getEntity())) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null && mobs.sangrado(killer) > 0) add(killer, "mobs", 1);
    }
}
