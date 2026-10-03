package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission43 extends BaseMission {

    public Mission43(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 43, "Geodas del vacío", MissionDifficulty.MEDIA, 15,
                "Consigue 16 Cristales de Celestita en las geodas del End.");
        counter("cristales", "Cristales de Celestita", 16);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_3", 2), custom("mochila_nivel_3", 1));
    }

    // Solo los que salen de los racimos: lo que tiró un jugador al piso no cuenta
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getItem().getThrower() != null) return;
        ItemStack item = event.getItem().getItemStack();
        if ("cristal_celestita".equals(MissionUtils.customId(item))) add(player, "cristales", item.getAmount());
    }
}
