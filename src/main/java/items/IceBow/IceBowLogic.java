package items.IceBow;

import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public class IceBowLogic implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> playerBowCooldowns;
    private final NamespacedKey playerIceArrowKey;
    private final IceBowItem iceBowItem;

    public IceBowLogic(JavaPlugin plugin, Map<UUID, Long> playerBowCooldowns) {
        this.plugin = plugin;
        this.playerBowCooldowns = playerBowCooldowns;
        this.playerIceArrowKey = new NamespacedKey(plugin, "player_ice_arrow");
        this.iceBowItem = new IceBowItem(plugin);
    }

    public void shootIceBow(Evoker shooter, Player target, NamespacedKey iceologerKey) {
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 0.8f);

        Arrow arrow = shooter.launchProjectile(Arrow.class);
        arrow.getPersistentDataContainer().set(iceologerKey, PersistentDataType.BYTE, (byte) 1);

        Location targetLoc = target.getLocation().add(0, 1, 0);
        Vector playerVelocity = target.getVelocity();
        double distance = shooter.getEyeLocation().distance(targetLoc);
        double timeToReach = distance / 2.0;
        targetLoc.add(playerVelocity.clone().multiply(timeToReach));

        Vector direction = targetLoc.toVector()
                .subtract(shooter.getEyeLocation().toVector()).normalize();
        arrow.setVelocity(direction.multiply(2.5));

        shooter.getWorld().spawnParticle(Particle.SNOWFLAKE,
                shooter.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1);

        shooter.getWorld().playSound(shooter.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.8f, 1.5f);
    }

    @EventHandler
    public void onPlayerShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!iceBowItem.isIceBow(event.getBow())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (playerBowCooldowns.containsKey(playerId)) {
            long lastUse = playerBowCooldowns.get(playerId);
            long remainingCooldown = (lastUse + 5000) - currentTime;

            if (remainingCooldown > 0) {
                event.setCancelled(true);

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.2f);
                return;
            }
        }

        playerBowCooldowns.put(playerId, currentTime);

        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(playerIceArrowKey, PersistentDataType.BYTE, (byte) 1);

            player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    player.getEyeLocation(), 15, 0.4, 0.4, 0.4, 0.1);
            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.8f);
        }

    }

    public void handleArrowDamage(EntityDamageByEntityEvent event, Arrow arrow, NamespacedKey iceologerKey) {
        if (arrow.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {
            if (event.getEntity() instanceof LivingEntity entity) {
                applyIceologerArrowEffect(entity);
            }
        }
        else if (arrow.getPersistentDataContainer().has(playerIceArrowKey, PersistentDataType.BYTE)) {
            if (event.getEntity() instanceof LivingEntity entity) {
                applyPlayerArrowEffect(entity);
            }
        }
    }

    private void applyIceologerArrowEffect(LivingEntity entity) {
        entity.setFreezeTicks(entity.getFreezeTicks() + 200);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0));

        World world = entity.getWorld();
        Location loc = entity.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.CLOUD, loc, 8, 0.3, 0.3, 0.3, 0.05);
        world.playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
        world.playSound(entity.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
    }

    private void applyPlayerArrowEffect(LivingEntity entity) {
        entity.setFreezeTicks(entity.getFreezeTicks() + 200);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));

        World world = entity.getWorld();
        Location loc = entity.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.SNOWFLAKE, loc, 10, 0.3, 0.3, 0.3, 0.1);
        world.playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.2f);

    }

    public void cleanOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        playerBowCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 300000);
    }

    public void removeCooldown(UUID playerId) {
        playerBowCooldowns.remove(playerId);
    }
}