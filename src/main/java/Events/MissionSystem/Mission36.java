package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission36 extends BaseMission {

    public Mission36(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 36, "¿Confías en mí?", MissionDifficulty.MEDIA, 14,
                "Sobrevive al vacío activando un tótem.");
        flag("vacio", "Tótem en el vacío");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.TOTEM_OF_UNDYING, 2), custom("potion_slow_falling", 3));
    }

    // El tótem tiene que salvarlo del vacío (o de caer por debajo de Y -50 en el End)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent last = player.getLastDamageCause();
        boolean voidDeath = (last != null && last.getCause() == EntityDamageEvent.DamageCause.VOID)
                || (player.getWorld().getEnvironment() == World.Environment.THE_END && player.getLocation().getY() < -50);
        if (voidDeath) mark(player, "vacio");
    }
}
