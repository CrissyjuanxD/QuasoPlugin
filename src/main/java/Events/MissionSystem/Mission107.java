package Events.MissionSystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.SulfurCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission107 extends BaseMission {

    public Mission107(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 107, "Uh Oh", MissionDifficulty.FACIL, 11,
                "Haz que un Sulfur Cube absorba un bloque de TNT.");
        flag("tnt", "Sulfur Cube con TNT");
        extraOf(19);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.TNT, 8), item(Material.GUNPOWDER, 32));
    }

    // Dándole la TNT en la mano: al tick siguiente el cubo ya la tiene adentro
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeed(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof SulfurCube cube)) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItem(event.getHand());
        if (hand == null || hand.getType() != Material.TNT || !tracking(player)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (cube.isValid() && cube.getEquipped().getType() == Material.TNT) mark(player, "tnt");
        });
    }

    // Tirándole la TNT al piso para que la absorba
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbsorb(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof SulfurCube) || event.getItem().getItemStack().getType() != Material.TNT) return;
        UUID thrower = event.getItem().getThrower();
        Player player = thrower != null ? Bukkit.getPlayer(thrower) : null;
        if (player != null) mark(player, "tnt");
    }
}
