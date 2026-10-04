package Events.MissionSystem;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission127 extends BaseMission {

    public Mission127(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 127, "Comerciante", MissionDifficulty.FACIL, 11,
                "Tradea 20 veces con aldeanos (sin contar las tiendas del spawn).");
        counter("tradeos", "Tradeos", 20);
        extraOf(18);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.EMERALD, 16), item(Material.BELL, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (MissionUtils.isShopVillager(event.getVillager())) return;
        add(event.getPlayer(), "tradeos", 1);
    }
}
