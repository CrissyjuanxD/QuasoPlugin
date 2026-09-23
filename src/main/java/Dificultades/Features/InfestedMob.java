package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Clase abstracta base para los Infested Mobs.
 * Los mobs usan su AI vanilla normal.
 * Ataque compartido: launchSonicBoom.
 */
public abstract class InfestedMob {

    protected final JavaPlugin plugin;
    protected final NamespacedKey mobKey;

    public InfestedMob(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, keyName);
    }

    public void apply() {
        // Subclases registran sus propios listeners
    }

    // -------------------------------------------------------------------------
    // ATAQUE COMPARTIDO: SONIC BOOM
    // -------------------------------------------------------------------------

    /**
     * Lanza un sonic boom desde el mob hacia el objetivo.
     *
     * Mecánica:
     * 1. Carga breve (~0.75s) con sonido y partículas.
     * 2. El beam viaja como proyectil de partículas tick a tick, rápido y visible.
     * 3. Atraviesa bloques completamente (sin rayTrace de bloques, solo entidades).
     * 4. Daña UNA SOLA VEZ al contacto con el jugador y desaparece inmediatamente.
     * 5. No interfiere con la AI del mob.
     */
    protected void launchSonicBoom(LivingEntity mob, Player target) {
        if (mob.isDead() || !mob.isValid() || target.isDead()) return;

        World world = mob.getWorld();
        Location startLoc = mob.getEyeLocation();

        // Sonido + partículas de carga
        world.playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.8f);
        world.spawnParticle(Particle.SONIC_BOOM, startLoc, 2, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.ELECTRIC_SPARK, startLoc, 8, 0.4, 0.4, 0.4, 0.05);

        // Carga breve (15 ticks = 0.75s) antes de disparar
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

    /**
     * El beam viaja en línea recta desde el mob hasta la posición del jugador
     * en el momento del disparo. Avanza 1.5 bloques por tick dejando estela.
     * Sin colisión con bloques. Daña una sola vez al contacto y termina.
     */
    private void fireTravelingBeam(LivingEntity mob, Player target) {
        if (mob.isDead() || !mob.isValid() || target.isDead()) return;

        World world = mob.getWorld();

        // Origen y destino fijos en el momento del disparo
        final Location origin = mob.getEyeLocation().clone();
        final Location destination = target.getEyeLocation().clone();

        final Vector direction = destination.toVector()
                .subtract(origin.toVector())
                .normalize();
        final double totalDistance = origin.distance(destination);
        final double SPEED = 1.5; // bloques por tick

        world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                // Estela desde posición anterior hasta actual (sin huecos)
                double prevTraveled = Math.max(0, traveled - SPEED);
                for (double d = prevTraveled; d <= traveled; d += 0.3) {
                    Location point = origin.clone().add(direction.clone().multiply(d));
                    world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
                    if (d % 0.6 < 0.3) {
                        world.spawnParticle(Particle.END_ROD, point, 1, 0.04, 0.04, 0.04, 0.01);
                    }
                }

                // Frente del beam
                Location front = origin.clone().add(direction.clone().multiply(traveled));

                // Detección de impacto con entidades (sin rayTrace de bloques)
                for (Entity nearby : front.getWorld().getNearbyEntities(front, 1.2, 1.2, 1.2)) {
                    if (!(nearby instanceof LivingEntity livingHit)) continue;
                    if (livingHit == mob) continue;
                    if (nearby instanceof Player p && p.getGameMode() == GameMode.CREATIVE) continue;

                    // Impacto — una sola vez
                    livingHit.damage(25, mob);
                    livingHit.setVelocity(direction.clone().multiply(0.6).setY(0.35));

                    world.playSound(front, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.1f);
                    world.spawnParticle(Particle.SONIC_BOOM, front, 4, 0.3, 0.3, 0.3, 0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, front, 12, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.END_ROD, front, 6, 0.3, 0.3, 0.3, 0.05);

                    this.cancel();
                    return;
                }

                traveled += SPEED;

                // Llegó al destino sin impactar: efecto residual y termina
                if (traveled > totalDistance + SPEED) {
                    world.spawnParticle(Particle.SONIC_BOOM, destination, 3, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.END_ROD, destination, 4, 0.2, 0.2, 0.2, 0.03);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // -------------------------------------------------------------------------
    // UTILIDADES
    // -------------------------------------------------------------------------

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