package Events.MissionSystem;

import items.WardenCaveItems;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission128 extends BaseMission {

    public Mission128(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 128, "Sabores de la cueva", MissionDifficulty.FACIL, 12,
                "Come las 3 frutas de la Warden Cave: Baya Sculk, Fruta Abisal y Baya Luminosa.");
        flag("baya_sculk", "Baya Sculk");
        flag("fruta_abisal", "Fruta Abisal");
        flag("baya_luminosa", "Baya Luminosa");
        extraOf(21);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("baya_luminosa", 8), item(Material.GOLDEN_CARROT, 16));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (WardenCaveItems.isFruit(item)) mark(event.getPlayer(), WardenCaveItems.idOf(item));
    }
}
