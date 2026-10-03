package Events.MissionSystem;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission66 extends BaseMission {

    public Mission66(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 66, "Trader profesional", MissionDifficulty.DIFICIL, 17,
                "Tradea 1.000 veces con aldeanos (sin contar las tiendas del spawn).");
        counter("tradeos", "Tradeos", 1000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.MENDING, 1), item(Material.EMERALD_BLOCK, 16));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (MissionUtils.isShopVillager(event.getVillager())) return;
        add(event.getPlayer(), "tradeos", 1);
    }
}
