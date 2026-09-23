package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission19 extends BaseMission {
    private final NamespacedKey markKey;

    public Mission19(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 19, "Nervios de Acero", MissionDifficulty.DIFICIL, 19,
                "Pégale con un proyectil a 4 Wardens y mátalos.");
        counter("wardens", "Wardens", 4);
        this.markKey = new NamespacedKey(plugin, "mission8_snowball_marker");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENCHANTED_GOLDEN_APPLE, 5), item(Material.EMERALD_BLOCK, 20));
    }

    // Marca al Warden con el jugador que le pegó con el proyectil
    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getHitEntity() == null || event.getHitEntity().getType() != EntityType.WARDEN) return;
        if (!(event.getEntity().getShooter() instanceof Player player) || !tracking(player)) return;

        event.getHitEntity().getPersistentDataContainer().set(markKey, PersistentDataType.STRING, player.getUniqueId().toString());
        sendBar(player, ChatColor.AQUA + "¡Warden marcado! Ahora elimínalo.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
    }

    // Solo cuenta si lo mata el mismo jugador que lo marcó
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String marked = event.getEntity().getPersistentDataContainer().get(markKey, PersistentDataType.STRING);
        if (killer.getUniqueId().toString().equals(marked)) add(killer, "wardens", 1);
    }
}
