package Dificultades.CustomMobs;

import Dificultades.Features.EnderMobs;
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

public class EnderBlaze extends EnderMobs implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static final Set<UUID> trackedFireballs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    public EnderBlaze(JavaPlugin plugin) {
        super(plugin, "ender_blaze");
    }

    @Override
    public void apply() {
        super.apply(); // Teleport al recibir daño
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

    public Blaze spawnEnderBlaze(Location location) {
        Blaze blaze = (Blaze) location.getWorld().spawnEntity(location, EntityType.BLAZE);
        applyAttributes(blaze);
        activeMobs.add(blaze.getUniqueId());
        startGlobalParticleTask();
        return blaze;
    }

    private void applyAttributes(Blaze blaze) {
        blaze.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Blaze");
        blaze.setCustomNameVisible(false);

        blaze.getAttribute(Attribute.MAX_HEALTH).setBaseValue(60);
        blaze.setHealth(60);
        blaze.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(64);

        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1, false, false));

        blaze.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    // Interceptar fireballs: marcarlas para que exploten al impactar
    @EventHandler
    public void onFireballLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!(fireball.getShooter() instanceof Blaze blaze)) return;
        if (!isCustomMob(blaze)) return;

        fireball.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        trackedFireballs.add(fireball.getUniqueId());
        fireball.setYield(3.0f);

        // Trail de partículas portal en la fireball
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead() || !fireball.isValid()) {
                    trackedFireballs.remove(fireball.getUniqueId());
                    this.cancel();
                    return;
                }
                fireball.getWorld().spawnParticle(Particle.PORTAL, fireball.getLocation(), 5, 0.1, 0.1, 0.1, 0.05);
                fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Al explotar la fireball
    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!trackedFireballs.contains(fireball.getUniqueId())) return;

        Location loc = fireball.getLocation();
        World world = fireball.getWorld();

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.PORTAL, loc, 60, 1.5, 1.5, 1.5, 0.3);
        world.spawnParticle(Particle.FLAME, loc, 30, 1, 1, 1, 0.1);

        trackedFireballs.remove(fireball.getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_BLAZE_HURT, 1.5f, 0.7f);
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.8f);
        event.getEntity().getWorld().spawnParticle(Particle.PORTAL,
                event.getEntity().getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Blaze blaze = (Blaze) event.getEntity();
        event.getDrops().clear();

        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_DEATH, 2.0f, 0.7f);
        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.7f);
        blaze.getWorld().spawnParticle(Particle.PORTAL, blaze.getLocation(), 100, 1, 1, 1, 0.5);
        blaze.getWorld().spawnParticle(Particle.FLAME, blaze.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        activeMobs.remove(blaze.getUniqueId());
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
                    entity.getWorld().spawnParticle(Particle.PORTAL,
                            entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                    entity.getWorld().spawnParticle(Particle.FLAME,
                            entity.getLocation(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Blaze &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}