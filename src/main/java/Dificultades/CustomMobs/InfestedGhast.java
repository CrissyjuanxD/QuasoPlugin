package Dificultades.CustomMobs;

import Dificultades.Features.InfestedMob;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InfestedGhast extends InfestedMob implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static final Set<UUID> trackedFireballs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    private static final double DARKNESS_RADIUS = 25.0;

    public InfestedGhast(JavaPlugin plugin) {
        super(plugin, "infested_ghast");
    }

    @Override
    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isCustomMob(entity)) entity.remove();
                }
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            trackedFireballs.clear();
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Ghast spawnInfestedGhast(Location location) {
        Ghast ghast = (Ghast) location.getWorld().spawnEntity(location, EntityType.GHAST);
        applyAttributes(ghast);
        activeMobs.add(ghast.getUniqueId());
        startGlobalParticleTask();
        return ghast;
    }

    private void applyAttributes(Ghast ghast) {
        ghast.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Infested Ghast");
        ghast.setCustomNameVisible(false);

        ghast.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
        ghast.setHealth(100);

        ghast.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onGhastFireball(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!(fireball.getShooter() instanceof Ghast ghast)) return;
        if (!isCustomMob(ghast)) return;

        fireball.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        trackedFireballs.add(fireball.getUniqueId());
        fireball.setYield(6.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead() || !fireball.isValid()) {
                    trackedFireballs.remove(fireball.getUniqueId());
                    this.cancel();
                    return;
                }
                fireball.getWorld().spawnParticle(Particle.SONIC_BOOM, fireball.getLocation(), 1, 0, 0, 0, 0);
                fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!trackedFireballs.contains(fireball.getUniqueId())) return;

        Location explosionLoc = fireball.getLocation();
        World world = fireball.getWorld();

        world.playSound(explosionLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.SONIC_BOOM, explosionLoc, 8, 1, 1, 1, 0);

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(explosionLoc) <= DARKNESS_RADIUS) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 1, false, true));
                player.spawnParticle(Particle.SQUID_INK,
                        player.getLocation().add(0, 1, 0), 15, 0.5, 0.8, 0.5, 0.05);
            }
        }

        trackedFireballs.remove(fireball.getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GHAST_HURT, 1.5f, 0.6f);
        event.getEntity().getWorld().spawnParticle(Particle.SONIC_BOOM,
                event.getEntity().getLocation().add(0, 2, 0), 2, 0.3, 0.3, 0.3, 0);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Ghast ghast = (Ghast) event.getEntity();
        event.getDrops().clear();

        ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_DEATH, 2.0f, 0.5f);
        ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 0.7f);
        ghast.getWorld().spawnParticle(Particle.SOUL, ghast.getLocation(), 60, 2, 2, 2, 0.3);
        ghast.getWorld().spawnParticle(Particle.SONIC_BOOM, ghast.getLocation(), 8, 1, 1, 1, 0);

        activeMobs.remove(ghast.getUniqueId());
    }

    private void startGlobalParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) return;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMobs.isEmpty()) return;
                Iterator<UUID> it = activeMobs.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }
                    entity.getWorld().spawnParticle(Particle.SONIC_BOOM,
                            entity.getLocation().add(0, 2, 0), 2, 1.0, 1.0, 1.0, 0);
                    entity.getWorld().spawnParticle(Particle.FLAME,
                            entity.getLocation(), 4, 1.0, 1.0, 1.0, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 8L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Ghast &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}