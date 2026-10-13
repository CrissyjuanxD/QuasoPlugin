package Encantamientos;

import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

// Vigor de Batalla (arma, encantamiento de Guerrero): al matar un monstruo con el arma te curas 1 corazón por nivel
public class VigorDeBatalla implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMatar(EntityDeathEvent e) {
        LivingEntity muerto = e.getEntity();
        Player player = muerto.getKiller();
        if (player == null || !(muerto instanceof Enemy) || player.isDead()) return;
        int nivel = QuasoEnchant.VIGOR_DE_BATALLA.level(player.getInventory().getItemInMainHand());
        if (nivel <= 0) return;

        AttributeInstance vida = player.getAttribute(Attribute.MAX_HEALTH);
        double maximo = vida != null ? vida.getValue() : 20;
        if (player.getHealth() >= maximo) return;
        player.setHealth(Math.min(maximo, player.getHealth() + curacion(nivel)));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2.1, 0), nivel, 0.3, 0.1, 0.3, 0);
    }

    // Vida que cura (2 = un corazón por nivel)
    static double curacion(int nivel) {
        return 2.0 * nivel;
    }
}
