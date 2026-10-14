package EndBiomes;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Set;

// En el End los items tirados no se queman ni explotan (el fuego del Páramo y las TNT de los shulkers se comían los
// drops) y los mobs no reciben Wither: en el Páramo se lo pasaban entre ellos y se mataban solos
public class EndProteccion implements Listener {

    static final Set<EntityDamageEvent.DamageCause> DESTRUYEN_ITEMS = EnumSet.of(
            EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK, EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.HOT_FLOOR, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (entity.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (entity instanceof Item && DESTRUYEN_ITEMS.contains(e.getCause())) {
            e.setCancelled(true);
            entity.setFireTicks(0);
        } else if (esMob(entity) && e.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            e.setCancelled(true);
        }
    }

    // Tampoco se les pone el efecto, así no quedan las partículas negras
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWither(EntityPotionEffectEvent e) {
        if (e.getNewEffect() == null || !e.getNewEffect().getType().equals(PotionEffectType.WITHER)) return;
        if (e.getEntity().getWorld().getEnvironment() == World.Environment.THE_END && esMob(e.getEntity())) e.setCancelled(true);
    }

    static boolean esMob(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player);
    }
}
