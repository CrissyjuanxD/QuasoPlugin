package EndBiomes;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

// En el End los Enderman no agarran ni ponen bloques. En el Bosque Prismático se llevaban el pasto, la tierra y las flores,
// y un Enderman con un bloque en la mano no se va nunca ni cuenta para el límite de mobs: se juntaban cientos (en 5
// minutos casi 200 alrededor de un jugador, contra 70 en el End normal) y sus partículas bajaban mucho los FPS. Los que
// ya tenían un bloque lo pierden al cargarse, así vuelven a contar y se van como cualquier mob
public class EndEndermen implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlock(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof Enderman && e.getBlock().getWorld().getEnvironment() == World.Environment.THE_END) e.setCancelled(true);
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent e) {
        if (e.getWorld().getEnvironment() != World.Environment.THE_END) return;
        for (Entity entity : e.getEntities()) soltar(entity);
    }

    // Al prender el server, los que ya están cargados
    public static int limpiar() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            for (Enderman enderman : world.getEntitiesByClass(Enderman.class)) if (soltar(enderman)) count++;
        }
        return count;
    }

    static boolean soltar(Entity entity) {
        if (!(entity instanceof Enderman enderman) || enderman.getCarriedBlock() == null) return false;
        enderman.setCarriedBlock(null);
        return true;
    }
}
