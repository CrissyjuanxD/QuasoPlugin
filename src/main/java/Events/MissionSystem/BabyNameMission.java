package Events.MissionSystem;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

// Misiones 105 y 118: bebés que se quedan chiquitos con un Golden Dandelion y que tienen nombre
public abstract class BabyNameMission extends BaseMission {
    private final NamespacedKey dandelionKey;

    protected BabyNameMission(JavaPlugin plugin, MissionHandler handler, int number, String name,
                              MissionDifficulty difficulty, int coins, String description, int target) {
        super(plugin, handler, number, name, difficulty, coins, description);
        counter("tipos", "Mobs distintos", target);
        this.dandelionKey = new NamespacedKey(plugin, "mision_golden_dandelion");
    }

    // Marca al bebé con quien le dio el Golden Dandelion
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDandelion(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItem(event.getHand());
        if (hand == null || hand.getType() != Material.GOLDEN_DANDELION) return;
        if (!(event.getRightClicked() instanceof Ageable baby) || baby.isAdult() || !tracking(player)) return;

        baby.getPersistentDataContainer().set(dandelionKey, PersistentDataType.STRING, player.getUniqueId().toString());
        plugin.getServer().getScheduler().runTask(plugin, () -> register(player, baby));
    }

    // El nombre se pone después del evento, por eso se revisa al tick siguiente
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onName(PlayerNameEntityEvent event) {
        LivingEntity entity = event.getEntity();
        plugin.getServer().getScheduler().runTask(plugin, () -> register(event.getPlayer(), entity));
    }

    private void register(Player player, LivingEntity entity) {
        if (!(entity instanceof Ageable baby) || baby.isAdult() || baby.customName() == null) return;
        if (!player.getUniqueId().toString().equals(baby.getPersistentDataContainer().get(dandelionKey, PersistentDataType.STRING))) return;
        if (!tracking(player)) return;

        MissionData data = data(player);
        List<String> types = data.getProgressList("tipos_lista");
        String type = baby.getType().name();
        if (types.contains(type)) return;

        types.add(type);
        data.setProgressValue("tipos_lista", types);
        set(player, "tipos", types.size());
    }
}
