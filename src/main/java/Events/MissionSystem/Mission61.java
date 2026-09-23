package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission61 extends BaseMission {

    public Mission61(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 61, "Qué buen pescador", MissionDifficulty.MEDIA, 15,
                "Pesca 800 veces (vale también en el lago de pesca).");
        counter("pescas", "Pescas", 800);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(enchanted(Material.FISHING_ROD, Enchantment.LUCK_OF_THE_SEA, 3, Enchantment.LURE, 3), custom("lingote_platino", 2));
    }

    // El lago cancela la pesca normal para el minijuego, por eso no se ignoran los cancelados
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) add(event.getPlayer(), "pescas", 1);
    }
}
