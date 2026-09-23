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

public class EnderCreeper extends EnderMobs implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    public EnderCreeper(JavaPlugin plugin) {
        super(plugin, "ender_creeper");
    }

    @Override
    public void apply() {
        super.apply();
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
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Creeper spawnEnderCreeper(Location location) {
        Creeper creeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyAttributes(creeper);
        activeMobs.add(creeper.getUniqueId());
        startGlobalParticleTask();
        return creeper;
    }

    // Creeper cargado, invisible y rápido con explosión de radio 5
    private void applyAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Creeper");
        creeper.setCustomNameVisible(false);

        creeper.getAttribute(Attribute.MAX_HEALTH).setBaseValue(60);
        creeper.setHealth(60);

        creeper.setPowered(true);
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));

        creeper.setExplosionRadius(5);
        creeper.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    // Evita que un rayo lo transforme
    @EventHandler
    public void onTransform(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING &&
                isCustomMob(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Creeper creeper = (Creeper) event.getEntity();
        Location loc = creeper.getLocation();
        World world = creeper.getWorld();

        world.playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.8f);
        world.playSound(loc, Sound.ENTITY_CREEPER_DEATH, 2.0f, 0.8f);
        world.spawnParticle(Particle.DRAGON_BREATH, loc, 100, 3, 3, 3, 0.5);
        world.spawnParticle(Particle.PORTAL, loc, 80, 2, 2, 2, 0.4);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Creeper creeper = (Creeper) event.getEntity();

        creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 2.0f, 0.7f);
        creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_HURT, 2.0f, 0.7f);
        creeper.getWorld().spawnParticle(Particle.PORTAL, creeper.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        creeper.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Creeper creeper = (Creeper) event.getEntity();
        event.getDrops().clear();

        creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.7f);
        creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_DEATH, 2.0f, 0.7f);
        creeper.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                creeper.getLocation(), 100, 1, 1, 1, 0.5);
        creeper.getWorld().spawnParticle(Particle.PORTAL, creeper.getLocation(), 60, 1, 1, 1, 0.3);

        activeMobs.remove(creeper.getUniqueId());
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
                    entity.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                            entity.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}