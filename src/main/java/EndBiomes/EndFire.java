package EndBiomes;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;

// En el End el fuego no se esparce ni quema bloques: las bolas de fuego de los blazes prendían los árboles del Bosque
// Prismático y el incendio daba lag. Se puede prender con mechero (y los cristales del End siguen con su fuego), pero
// se queda en ese bloque
public class EndFire implements Listener {

    public static boolean permitido(BlockIgniteEvent.IgniteCause cause) {
        return cause == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL || cause == BlockIgniteEvent.IgniteCause.ENDER_CRYSTAL;
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getBlock().getWorld().getEnvironment() == World.Environment.THE_END && !permitido(e.getCause())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (e.getBlock().getWorld().getEnvironment() == World.Environment.THE_END) e.setCancelled(true);
    }
}
