package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission102 extends BaseMission {

    public Mission102(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 102, "Señor del océano", MissionDifficulty.FACIL, 12,
                "Doma un Nautilus con pez globo y recorre 500 bloques montado.");
        flag("domar", "Domar un Nautilus");
        counter("bloques", "Bloques montado", 500);
        extraOf(4);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.IRON_NAUTILUS_ARMOR, 1), potion(PotionType.LONG_WATER_BREATHING, 2));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getEntityType() == EntityType.NAUTILUS && event.getOwner() instanceof Player player) mark(player, "domar");
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        statSince(player, "bloques", Statistic.NAUTILUS_ONE_CM, 100);
    }
}
