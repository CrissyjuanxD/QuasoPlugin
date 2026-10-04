package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static Events.MissionSystem.MissionRewards.*;

public class Mission122 extends BaseMission {
    private static final Set<Material> FISH = EnumSet.of(
            Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH
    );

    public Mission122(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 122, "Pescador novato", MissionDifficulty.FACIL, 11,
                "Pesca 20 peces.");
        counter("peces", "Peces", 20);
        extraOf(5);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(enchanted(Material.FISHING_ROD, Enchantment.LUCK_OF_THE_SEA, 1, Enchantment.LURE, 1), item(Material.COOKED_SALMON, 16));
    }

    // El lago cancela la pesca normal para el minijuego, por eso no se ignoran los cancelados
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() instanceof Item caught && FISH.contains(caught.getItemStack().getType())) {
            add(event.getPlayer(), "peces", 1);
        }
    }
}
