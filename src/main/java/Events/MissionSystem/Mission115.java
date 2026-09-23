package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission115 extends BaseMission {

    public Mission115(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 115, "Carga de caballería", MissionDifficulty.DIFICIL, 18,
                "Mata 50 mobs con la carga de la lanza yendo montado.");
        counter("mobs", "Mobs con carga montado", 50);
        extraOf(52);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NETHERITE_HORSE_ARMOR, 1), custom("splash_regeneration_3", 2));
    }

    // La carga es el golpe con lanza mientras la montura va en movimiento
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null || !MissionUtils.isSpear(killer.getInventory().getItemInMainHand())) return;

        Entity mount = killer.getVehicle();
        if (mount != null && mount.getVelocity().setY(0).lengthSquared() > 0.01) add(killer, "mobs", 1);
    }
}
