package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission46 extends BaseMission {

    public Mission46(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 46, "KBOOM", MissionDifficulty.DIFICIL, 18,
                "Mata 100 creepers con la explosión de otro creeper.");
        counter("creepers", "Creepers explotados", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("arco_nivel1", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 4));
    }

    // Cuenta para el jugador al que iba a buscar el creeper que explotó (o el más cercano)
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Creeper victim)) return;
        if (!(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent damage)) return;
        if (damage.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || !(damage.getDamager() instanceof Creeper exploded)) return;

        Player player = exploded.getTarget() instanceof Player target ? target : nearest(victim);
        if (player != null) add(player, "creepers", 1);
    }

    private Player nearest(Creeper creeper) {
        Player best = null;
        double bestDistance = 16 * 16;
        for (Player player : creeper.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(creeper.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }
}
