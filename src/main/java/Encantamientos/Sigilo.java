package Encantamientos;

import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;

import java.util.Set;

// Sigilo (pantalones): nivel I, lo que hace el jugador al moverse no hace vibraciones (como si fuera agachado);
// nivel II, nada de lo que hace las hace. Se corta el evento antes de que lo escuchen sensores, chilladores y Wardens
public class Sigilo implements Listener {

    private static final Set<GameEvent> MOVEMENT = Set.of(
            GameEvent.STEP, GameEvent.HIT_GROUND, GameEvent.SWIM, GameEvent.SPLASH, GameEvent.FLAP,
            GameEvent.ELYTRA_GLIDE, GameEvent.PROJECTILE_SHOOT, GameEvent.ITEM_INTERACT_START,
            GameEvent.ITEM_INTERACT_FINISH);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        int level = QuasoEnchant.SIGILO.level(player.getInventory().getLeggings());
        if (level >= 2 || (level == 1 && MOVEMENT.contains(e.getEvent()))) e.setCancelled(true);
    }

    // Pisar un chillador o un sensor no los activa (agachado tampoco lo hace)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStep(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.SCULK_SHRIEKER && type != Material.SCULK_SENSOR && type != Material.CALIBRATED_SCULK_SENSOR) return;
        if (QuasoEnchant.SIGILO.level(e.getPlayer().getInventory().getLeggings()) > 0) e.setCancelled(true);
    }
}
