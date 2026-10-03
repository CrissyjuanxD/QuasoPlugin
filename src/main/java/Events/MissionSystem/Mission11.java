package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Raid;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission11 extends BaseMission {

    public Mission11(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 11, "El Héroe Dorado", MissionDifficulty.MEDIA, 16,
                "Completa 2 Raids y fabrica 32 Manzanas de Oro.");
        counter("raids", "Raids", 2);
        counter("manzanas", "Manzanas de Oro", 32);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.TOTEM_OF_UNDYING, 3), custom("potion_resistance_2", 2));
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (event.getRaid().getStatus() != Raid.RaidStatus.VICTORY) return;
        for (Player player : event.getWinners()) add(player, "raids", 1);
    }

    // Con shift-click calcula cuántas manzanas salen según el ingrediente que menos hay
    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() != Material.GOLDEN_APPLE) return;

        int amount = result.getAmount();
        if (event.isShiftClick()) {
            int max = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && !ingredient.getType().isAir()) max = Math.min(max, ingredient.getAmount());
            }
            amount = max == Integer.MAX_VALUE ? 0 : max * result.getAmount();
        }
        if (amount > 0) add(player, "manzanas", amount);
    }
}
