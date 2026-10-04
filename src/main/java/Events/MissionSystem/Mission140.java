package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission140 extends BaseMission {

    public Mission140(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 140, "Pesca con suerte", MissionDifficulty.FACIL, 11,
                "Pesca un libro encantado.");
        flag("libro", "Pescar un libro encantado");
        extraOf(59);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(enchanted(Material.FISHING_ROD, Enchantment.LUCK_OF_THE_SEA, 3, Enchantment.LURE, 3), item(Material.COOKED_COD, 32));
    }

    // El lago cancela la pesca normal para el minijuego, por eso no se ignoran los cancelados
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() instanceof Item caught && caught.getItemStack().getType() == Material.ENCHANTED_BOOK) {
            mark(event.getPlayer(), "libro");
        }
    }
}
