package Events.MissionSystem;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission132 extends BaseMission {

    public Mission132(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 132, "Bautizo", MissionDifficulty.FACIL, 10,
                "Ponle nombre a 3 items en un yunque.");
        counter("nombres", "Items con nombre", 3);
        extraOf(33);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ANVIL, 1), item(Material.NAME_TAG, 3));
    }

    // Cuenta al sacar el resultado del yunque con un nombre nuevo y los niveles para pagarlo
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRename(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL || event.getRawSlot() != 2) return;
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getView() instanceof AnvilView view)) return;

        ItemStack result = event.getCurrentItem();
        String name = view.getRenameText();
        if (result == null || result.getType().isAir() || name == null || name.isBlank()) return;
        if (player.getGameMode() != GameMode.CREATIVE && player.getLevel() < view.getRepairCost()) return;

        ItemStack first = view.getTopInventory().getFirstItem();
        if (first != null && first.hasItemMeta() && first.getItemMeta().hasDisplayName()
                && name.equals(ChatColor.stripColor(first.getItemMeta().getDisplayName()))) return;
        add(player, "nombres", 1);
    }
}
