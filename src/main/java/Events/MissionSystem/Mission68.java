package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission68 extends BaseMission {

    public Mission68(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 68, "Fuego… ¿infectado?", MissionDifficulty.MUY_DIFICIL, 21,
                "Haz que un Infested Ghast le dispare a 50 esqueletos distintos sin recibir daño. Si una bola de fuego te pega, vuelves a 0.");
        counter("esqueletos", "Esqueletos distintos", 50);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("arco_nivel2", 1), item(Material.SPECTRAL_ARROW, 64));
    }

    // La bola de fuego cuenta para el jugador al que le estaba apuntando el ghast
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireball(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Fireball fireball) || !MissionUtils.isMob(fireball, MissionUtils.INFESTED_GHAST)) return;

        if (event.getEntity() instanceof Player hit) {
            if (tracking(hit) && data(hit).getProgressInt("esqueletos") > 0) {
                MissionData data = data(hit);
                data.setProgressValue("esqueletos_lista", new java.util.ArrayList<String>());
                set(hit, "esqueletos", 0);
                sendBar(hit, ChatColor.RED + "✖ Te pegó la bola de fuego, vuelves a 0");
            }
            return;
        }

        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) return;
        if (!(fireball.getShooter() instanceof Ghast ghast) || !(ghast.getTarget() instanceof Player player) || !tracking(player)) return;

        MissionData data = data(player);
        List<String> hits = data.getProgressList("esqueletos_lista");
        String id = skeleton.getUniqueId().toString();
        if (hits.contains(id)) return;

        hits.add(id);
        data.setProgressValue("esqueletos_lista", hits);
        set(player, "esqueletos", hits.size());
    }
}
