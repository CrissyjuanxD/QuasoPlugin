package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission121 extends BaseMission {

    public Mission121(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 121, "Chef", MissionDifficulty.FACIL, 10,
                "Cocina 32 alimentos en un horno o ahumador.");
        counter("comida", "Alimentos cocinados", 32);
        extraOf(3);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SMOKER, 1), item(Material.COOKED_BEEF, 32));
    }

    // Cuenta la comida al sacarla del horno, aunque la saque de a varios
    @EventHandler(priority = EventPriority.MONITOR)
    public void onExtract(FurnaceExtractEvent event) {
        if (event.getItemType().isEdible()) add(event.getPlayer(), "comida", event.getItemAmount());
    }
}
