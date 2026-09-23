package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission32 extends BaseMission {

    public Mission32(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 32, "Con su propia medicina", MissionDifficulty.MEDIA, 15,
                "Mata 10 Piglin Brutes con un hacha de oro y al menos una pieza de oro puesta.");
        counter("brutes", "Piglin Brutes", 10);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.NETHERITE_INGOT, 2), item(Material.GOLDEN_APPLE, 20));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.PIGLIN_BRUTE) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null || killer.getInventory().getItemInMainHand().getType() != Material.GOLDEN_AXE) return;
        if (hasGoldenArmor(killer)) add(killer, "brutes", 1);
    }

    private boolean hasGoldenArmor(Player player) {
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && piece.getType().name().startsWith("GOLDEN_")) return true;
        }
        return false;
    }
}
