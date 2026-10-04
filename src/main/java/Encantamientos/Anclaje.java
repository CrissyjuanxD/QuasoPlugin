package Encantamientos;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

// Anclaje (botas): la resistencia al empuje la pone el datapack. Acá: con nivel I la levitación de los shulkers dura
// la mitad y con nivel II no la recibe. La Ender Spider pregunta resistsTeleport antes de teletransportar
public class Anclaje implements Listener {

    private final JavaPlugin plugin;

    public Anclaje(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Solo la levitación de un ataque (balas de shulker); la del tótem en el vacío o la de otros items no se toca
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLevitation(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (e.getCause() != EntityPotionEffectEvent.Cause.ATTACK) return;
        PotionEffect effect = e.getNewEffect();
        if (effect == null || !effect.getType().equals(PotionEffectType.LEVITATION)) return;
        int level = QuasoEnchant.ANCLAJE.level(player.getInventory().getBoots());
        if (level <= 0) return;

        e.setCancelled(true);
        if (level == 1) {
            PotionEffect half = new PotionEffect(PotionEffectType.LEVITATION, Math.max(1, effect.getDuration() / 2),
                    effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) player.addPotionEffect(half);
            });
        }
    }

    // Nivel I: la mitad de las veces no lo mueven; nivel II: nunca
    public static boolean resistsTeleport(Player player) {
        int level = QuasoEnchant.ANCLAJE.level(player.getInventory().getBoots());
        return level >= 2 || (level == 1 && ThreadLocalRandom.current().nextBoolean());
    }
}
