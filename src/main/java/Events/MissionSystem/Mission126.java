package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission126 extends BaseMission {

    public Mission126(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 126, "Encantador", MissionDifficulty.FACIL, 11,
                "Encanta 5 items en la mesa de encantamientos.");
        counter("items", "Items encantados", 5);
        extraOf(15);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BOOKSHELF, 8), item(Material.LAPIS_LAZULI, 32));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        add(event.getEnchanter(), "items", 1);
    }
}
