package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission87 extends BaseMission {

    public Mission87(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 87, "Arquero explosivo", MissionDifficulty.DIFICIL, 19,
                "Mata 100 mobs con el Arco Explosivo.");
        counter("mobs", "Mobs con el Arco Explosivo", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("arco_nivel3", 1), item(Material.ARROW, 64));
    }

    // Cuenta el flechazo directo y la explosión, que el arco crea a nombre del jugador
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.getLastDamageCause() instanceof EntityDamageByEntityEvent damage)) return;

        Player player = null;
        if (damage.getDamager() instanceof SpectralArrow arrow && arrow.getShooter() instanceof Player shooter
                && arrow.getPersistentDataContainer().has(MissionUtils.key("explosive_arrow_level"), PersistentDataType.INTEGER)) {
            player = shooter;
        } else if (damage.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && damage.getDamager() instanceof Player source) {
            player = source;
        }
        if (player != null) add(player, "mobs", 1);
    }
}
