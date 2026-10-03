package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission112 extends BaseMission {

    public Mission112(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 112, "Domador total", MissionDifficulty.MEDIA, 15,
                "Doma un Nautilus, un Zombie Horse, un camello y un Happy Ghast.");
        flag("nautilus", "Nautilus");
        flag("zombie_horse", "Zombie Horse");
        flag("camello", "Camello");
        flag("happy_ghast", "Happy Ghast");
        extraOf(38);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SADDLE, 2), custom("happy_ghast_enchant", 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        switch (event.getEntityType()) {
            case NAUTILUS -> mark(player, "nautilus");
            case ZOMBIE_HORSE -> mark(player, "zombie_horse");
            default -> { }
        }
    }

    // Al camello y al Happy Ghast no se los doma: cuenta montarlos (con montura o arnés)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        switch (event.getMount().getType()) {
            case CAMEL -> mark(player, "camello");
            case HAPPY_GHAST -> mark(player, "happy_ghast");
            default -> { }
        }
    }
}
