package Dificultades.CustomMobs;

import Dificultades.Features.InfestedMob;
import items.InfestedSoulsItems;
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

public class InfestedCreeper extends InfestedMob implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    private static final double EXPLOSION_EFFECT_RADIUS = 15.0;

    public InfestedCreeper(JavaPlugin plugin) {
        super(plugin, "infested_creeper");
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
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Creeper spawnInfestedCreeper(Location location) {
        Creeper creeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        infest(creeper);
        return creeper;
    }

    // Convierte un creeper que ya existe (por ejemplo el que spawnea el juego) en Infested Creeper
    public void infest(Creeper creeper) {
        applyAttributes(creeper);
        activeMobs.add(creeper.getUniqueId());
        startGlobalParticleTask();
    }

    private void applyAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Infested Creeper");
        creeper.setCustomNameVisible(false);

        creeper.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
        creeper.setHealth(100);

        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));

        creeper.setExplosionRadius(8);
        creeper.setPowered(true);

        creeper.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    // La explosión no rompe bloques pero da oscuridad y veneno a los que estén a 15 bloques
    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Creeper creeper = (Creeper) event.getEntity();
        Location explosionLoc = creeper.getLocation();
        World world = creeper.getWorld();

        event.blockList().clear();

        world.playSound(explosionLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
        world.playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 2, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.SQUID_INK, explosionLoc, 30, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, explosionLoc, 50, 3, 2, 3, 0.05);

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(explosionLoc) <= EXPLOSION_EFFECT_RADIUS) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 1, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 1, false, true));
                player.spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.05);
                player.spawnParticle(Particle.SPORE_BLOSSOM_AIR, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.03);
            }
        }
    }

    @EventHandler
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (!(event.getDamager() instanceof Creeper creeper)) return;
        if (!isCustomMob(creeper)) return;
        if (event.getEntity() instanceof Player) return;

        event.setCancelled(true);
    }

    // Con proyectiles no se le puede bajar de 18 de vida, hay que rematarlo cuerpo a cuerpo
    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!isCustomMob(event.getEntity())) return;

        if (event.getDamager() instanceof Projectile) {
            Creeper creeper = (Creeper) event.getEntity();

            if (creeper.getHealth() <= 18.0) {
                event.setCancelled(true);
                creeper.getWorld().playSound(creeper.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            }
            else if (creeper.getHealth() - event.getFinalDamage() <= 18.0) {
                event.setCancelled(true);
                creeper.setHealth(18.0);
                creeper.getWorld().playSound(creeper.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onTransform(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING &&
                isCustomMob(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_WARDEN_HURT, 1.2f, 0.7f);
        event.getEntity().getWorld().spawnParticle(Particle.SMOKE,
                event.getEntity().getLocation(), 15, 0.4, 0.4, 0.4, 0.05);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Creeper creeper = (Creeper) event.getEntity();
        event.getDrops().clear();
        if (Math.random() < SOUL_CHANCE) {
            event.getDrops().add(new InfestedSoulsItems(plugin).createInfestedCreeperSoul());
        }

        creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 0.8f);
        creeper.getWorld().spawnParticle(Particle.SOUL, creeper.getLocation(), 50, 1, 1, 1, 0.3);
        creeper.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, creeper.getLocation(), 30, 1, 1, 1, 0.05);

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
                    entity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                            entity.getLocation().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 0.02);
                    entity.getWorld().spawnParticle(Particle.SQUID_INK,
                            entity.getLocation(), 2, 0.2, 0.2, 0.2, 0.02);
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