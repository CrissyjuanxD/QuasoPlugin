package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission13 extends BaseMission {
    private final NamespacedKey spottedKey;

    public Mission13(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 13, "¡Qué frialdad!", MissionDifficulty.MEDIA, 15,
                "Mira a 6 Iceologers con el catalejo y mátalos.");
        counter("iceologers", "Iceologers avistados", 6);
        this.spottedKey = new NamespacedKey(plugin, "mission9_spyglass_marker");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("icetotem", 1), item(Material.ANCIENT_DEBRIS, 8));
    }

    // Al mirar un Iceologer con el catalejo queda marcado para ese jugador
    @EventHandler
    public void onSpyglass(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.SPYGLASS) return;
        Player player = event.getPlayer();
        if (!tracking(player)) return;

        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50,
                entity -> entity instanceof Evoker && MissionUtils.isMob(entity, MissionUtils.ICEOLOGER));
        if (result == null || result.getHitEntity() == null) return;

        Entity iceologer = result.getHitEntity();
        String id = player.getUniqueId().toString();
        if (id.equals(iceologer.getPersistentDataContainer().get(spottedKey, PersistentDataType.STRING))) return;

        iceologer.getPersistentDataContainer().set(spottedKey, PersistentDataType.STRING, id);
        sendBar(player, ChatColor.AQUA + "¡Iceologer avistado! Ahora elimínalo.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!MissionUtils.isMob(entity, MissionUtils.ICEOLOGER)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String marked = entity.getPersistentDataContainer().get(spottedKey, PersistentDataType.STRING);
        if (killer.getUniqueId().toString().equals(marked)) add(killer, "iceologers", 1);
    }
}
