package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission104 extends BaseMission {

    public Mission104(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 104, "Caballería zombie", MissionDifficulty.MEDIA, 15,
                "Mata 10 jinetes zombie (los que llevan lanza y montan un Zombie Horse) y doma uno de sus caballos.");
        counter("jinetes", "Jinetes zombie", 10);
        flag("domar", "Domar un Zombie Horse");
        extraOf(9);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SADDLE, 1), item(Material.DIAMOND_HORSE_ARMOR, 1));
    }

    // El jinete todavía va montado cuando se lanza el evento de muerte
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.getVehicle() == null || zombie.getVehicle().getType() != EntityType.ZOMBIE_HORSE) return;
        Player killer = MissionUtils.killer(zombie);
        if (killer != null) add(killer, "jinetes", 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE_HORSE && event.getOwner() instanceof Player player) mark(player, "domar");
    }
}
