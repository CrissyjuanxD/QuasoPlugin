package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public abstract class InfestedMob {

    protected final JavaPlugin plugin;
    protected final NamespacedKey mobKey;

    // Probabilidad de que cada mob infestado suelte su alma (la que va en la mejora de la armadura)
    protected static final double SOUL_CHANCE = 0.07;

    public InfestedMob(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, keyName);
    }

    public void apply() {
    }

    // Carga el sonic boom 15 ticks antes de dispararlo
    protected void launchSonicBoom(LivingEntity mob, Player target) {
        if (mob.isDead() || !mob.isValid() || target.isDead()) return;

        World world = mob.getWorld();
        Location startLoc = mob.getEyeLocation();

        world.playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.8f);
        world.spawnParticle(Particle.SONIC_BOOM, startLoc, 2, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.ELECTRIC_SPARK, startLoc, 8, 0.4, 0.4, 0.4, 0.05);

        new BukkitRunnable() {
            int chargeTicks = 0;

            @Override
            public void run() {
                if (mob.isDead() || !mob.isValid()) {
                    this.cancel();
                    return;
                }

                if (chargeTicks % 4 == 0) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, mob.getEyeLocation(),
                            3, 0.25, 0.25, 0.25, 0.08);
                }

                chargeTicks++;

                if (chargeTicks >= 15) {
                    fireTravelingBeam(mob, target);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // El rayo avanza hacia donde estaba el jugador y hace 18 de daño al primero que toque
    private void fireTravelingBeam(LivingEntity mob, Player target) {
        if (mob.isDead() || !mob.isValid() || target.isDead()) return;

        World world = mob.getWorld();

        final Location origin = mob.getEyeLocation().clone();
        final Location destination = target.getEyeLocation().clone();

        final Vector direction = destination.toVector()
                .subtract(origin.toVector())
                .normalize();
        final double totalDistance = origin.distance(destination);
        final double SPEED = 1.5;

        world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                double prevTraveled = Math.max(0, traveled - SPEED);
                for (double d = prevTraveled; d <= traveled; d += 0.3) {
                    Location point = origin.clone().add(direction.clone().multiply(d));
                    world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
                    if (d % 0.6 < 0.3) {
                        world.spawnParticle(Particle.END_ROD, point, 1, 0.04, 0.04, 0.04, 0.01);
                    }
                }

                Location front = origin.clone().add(direction.clone().multiply(traveled));

                for (Entity nearby : front.getWorld().getNearbyEntities(front, 1.2, 1.2, 1.2)) {
                    if (!(nearby instanceof LivingEntity livingHit)) continue;
                    if (livingHit == mob) continue;
                    if (nearby instanceof Player p && p.getGameMode() == GameMode.CREATIVE) continue;

                    livingHit.damage(18, mob);
                    livingHit.setVelocity(direction.clone().multiply(0.6).setY(0.35));

                    world.playSound(front, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.1f);
                    world.spawnParticle(Particle.SONIC_BOOM, front, 4, 0.3, 0.3, 0.3, 0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, front, 12, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.END_ROD, front, 6, 0.3, 0.3, 0.3, 0.05);

                    this.cancel();
                    return;
                }

                traveled += SPEED;

                if (traveled > totalDistance + SPEED) {
                    world.spawnParticle(Particle.SONIC_BOOM, destination, 3, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.END_ROD, destination, 4, 0.2, 0.2, 0.2, 0.03);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    protected Player findNearestPlayer(Location location, double radius) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double dist = player.getLocation().distance(location);
            if (dist <= radius && dist < nearestDist) {
                nearest = player;
                nearestDist = dist;
            }
        }
        return nearest;
    }

    public abstract boolean isCustomMob(Entity entity);
}