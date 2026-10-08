package items;

import Bosses.ReyEnderBoss;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;

// La Espada de Celestita le pega el doble a los mobs del End: a todos los que están en el End y a los endermans,
// endermites y shulkers de cualquier lado. Al Rey Ender el doble se lo aplica ReyEnderBoss
public class EspadaCelestita implements Listener {

    public static final double MULTIPLICADOR = 2;
    private static final Set<EntityType> DEL_END = Set.of(EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.SHULKER,
            EntityType.ENDER_DRAGON);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player || target instanceof ArmorStand) return;
        if (!EndItems.isCelestiteSword(player.getInventory().getItemInMainHand())) return;
        if (ReyEnderBoss.ACTIVE_BOSSES.containsKey(target.getUniqueId())) return;
        if (delEnd(target.getWorld().getEnvironment(), target.getType())) event.setDamage(event.getDamage() * MULTIPLICADOR);
    }

    static boolean delEnd(World.Environment mundo, EntityType tipo) {
        return mundo == World.Environment.THE_END || DEL_END.contains(tipo);
    }
}
