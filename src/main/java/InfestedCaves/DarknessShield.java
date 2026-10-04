package InfestedCaves;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Jugadores que por un rato no reciben Oscuridad (la Fruta Abisal y el encantamiento Visión Abisal). Corta la de los
// Warden, los chilladores, los mobs infestados y la del ambiente de la dimensión
public class DarknessShield implements Listener {

    private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();

    public static void protect(Player player, long millis) {
        UNTIL.merge(player.getUniqueId(), System.currentTimeMillis() + millis, Math::max);
        if (player.hasPotionEffect(PotionEffectType.DARKNESS)) player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    public static boolean isProtected(Player player) {
        return UNTIL.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDarkness(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        PotionEffect effect = e.getNewEffect();
        if (effect == null || !effect.getType().equals(PotionEffectType.DARKNESS)) return;
        if (isProtected(player)) e.setCancelled(true);
    }
}
