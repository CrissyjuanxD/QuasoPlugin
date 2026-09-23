package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Zoglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission6 extends BaseMission {
    private final NamespacedKey friendKey;

    public Mission6(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 6, "Los mejores amigos", MissionDifficulty.MEDIA, 13,
                "Defiende 10 Snow Golems sin calabaza en un Warped Forest hasta que se derritan solos.");
        counter("golems", "Golems derretidos", 10);
        this.friendKey = new NamespacedKey(plugin, "mission_friend_snowman");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("tarta_calabaza_mejorada", 8), item(Material.GOLDEN_APPLE, 5));
    }

    // En Warped Forest los golems no reciben daño de fuego ni calor, así solo se derriten con el daño de la misión
    @EventHandler
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowman snowman)) return;
        if (!snowman.getLocation().getBlock().getBiome().equals(Biome.WARPED_FOREST)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.MELTING || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA
                || isMagma(event)) {
            event.setCancelled(true);
        }
    }

    // Desde la 26.2 la magma ya no da HOT_FLOOR, llega como CONTACT con el bloque
    private boolean isMagma(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.CONTACT) return false;
        if (!(event instanceof EntityDamageByBlockEvent byBlock) || byBlock.getDamager() == null) return false;
        return byBlock.getDamager().getType() == Material.MAGMA_BLOCK;
    }

    // Al quitarle la calabaza el golem queda a cargo del jugador, atrae a los mobs del Nether y se va derritiendo
    @EventHandler
    public void onShearSnowman(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Snowman snowman)) return;
        Player player = event.getPlayer();
        if (!tracking(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.SHEARS) return;
        if (!snowman.getLocation().getBlock().getBiome().equals(Biome.WARPED_FOREST)) return;
        if (snowman.isDerp() || snowman.getPersistentDataContainer().has(friendKey, PersistentDataType.STRING)) return;

        snowman.getPersistentDataContainer().set(friendKey, PersistentDataType.STRING, player.getUniqueId().toString());
        sendBar(player, ChatColor.of("#FFCC99") + "¡Protege al Golem hasta que se derrita!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!snowman.isValid() || snowman.isDead()) {
                    cancel();
                    return;
                }
                for (Entity nearby : snowman.getNearbyEntities(15, 15, 15)) {
                    if ((nearby instanceof Enderman || nearby instanceof PiglinAbstract || nearby instanceof Zoglin)
                            && nearby instanceof Mob mob && !(mob.getTarget() instanceof Snowman)) {
                        mob.setTarget(snowman);
                    }
                }
                if (ticks % 6 == 0) {
                    snowman.setMetadata("custom_melt", new FixedMetadataValue(plugin, true));
                    snowman.damage(1.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // Solo cuenta si el golem murió derretido por la misión
    @EventHandler
    public void onSnowmanDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Snowman snowman)) return;
        String owner = snowman.getPersistentDataContainer().get(friendKey, PersistentDataType.STRING);
        if (owner == null) return;

        EntityDamageEvent damage = snowman.getLastDamageCause();
        if (damage == null || damage.getCause() != EntityDamageEvent.DamageCause.CUSTOM || !snowman.hasMetadata("custom_melt")) return;

        Player player = Bukkit.getPlayer(UUID.fromString(owner));
        if (player != null) add(player, "golems", 1);
    }
}
