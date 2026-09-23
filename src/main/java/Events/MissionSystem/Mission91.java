package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission91 extends BaseMission {

    public Mission91(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 91, "Héroe del pueblo", MissionDifficulty.MEDIA, 14,
                "Cura 10 aldeanos zombie.");
        counter("aldeanos", "Aldeanos curados", 10);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.MENDING, 1), item(Material.GOLDEN_APPLE, 10));
    }

    // Cuenta para el jugador que le dio la manzana dorada
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCure(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.CURED) return;
        if (!(event.getEntity() instanceof ZombieVillager zombie) || zombie.getConversionPlayer() == null) return;
        Player player = zombie.getConversionPlayer().getPlayer();
        if (player != null) add(player, "aldeanos", 1);
    }
}
