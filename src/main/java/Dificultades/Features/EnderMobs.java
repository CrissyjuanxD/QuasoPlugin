package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public abstract class EnderMobs {

    protected final JavaPlugin plugin;
    protected final NamespacedKey mobKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();

    public EnderMobs(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, keyName);
    }

    public void apply() {
        if (!eventsRegistered) {
            // Listener: 25% de teleport al recibir daño
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onMobHurt(EntityDamageEvent event) {
                    if (isCustomMob(event.getEntity())) {
                        handleTeleportOnDamage((LivingEntity) event.getEntity());
                    }
                }
            }, plugin);

            // Tarea periódica: 5% de teleport cada minuto (1200 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (!isCustomMob(entity)) continue;
                            if (!(entity instanceof LivingEntity mob)) continue;
                            if (mob.isDead() || !mob.isValid()) continue;

                            if (Math.random() < 0.05) {
                                teleportRandomly(mob, 15); // Radio reducido a 15
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1200L);

            eventsRegistered = true;
        }
    }

    protected void handleTeleportOnDamage(LivingEntity mob) {
        if (Math.random() < 0.25) { // Reducido al 25%
            teleportRandomly(mob, 15); // Radio reducido a 15
        }
    }

    protected void teleportRandomly(LivingEntity mob, int radius) {
        Location originalLoc = mob.getLocation();
        Location newLoc = findSafeLocation(originalLoc, radius);

        if (newLoc != null) {
            World world = mob.getWorld();
            world.playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
            world.spawnParticle(Particle.PORTAL, originalLoc, 50, 0.5, 0.5, 0.5, 0.5);

            mob.teleport(newLoc);

            world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
            world.spawnParticle(Particle.PORTAL, newLoc, 50, 0.5, 0.5, 0.5, 0.5);
        }
    }

    // Busca un lugar seguro cercano sin ahogar al mob ni enviarlo encima de la bedrock
    private Location findSafeLocation(Location currentLoc, int radius) {
        World world = currentLoc.getWorld();

        for (int i = 0; i < 15; i++) {
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetY = random.nextInt(radius) - (radius / 2);
            int offsetZ = random.nextInt(radius * 2) - radius;

            Location target = currentLoc.clone().add(offsetX, offsetY, offsetZ);

            // Previene que se salga de los límites del mundo
            if (target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight()) {
                continue;
            }

            Block ground = target.clone().subtract(0, 1, 0).getBlock();
            Block feet = target.getBlock();
            Block head = target.clone().add(0, 1, 0).getBlock();

            // Verifica que el suelo sea sólido y que el cuerpo/cabeza estén en bloques libres (sin lava/agua)
            if (ground.getType().isSolid() &&
                    feet.isPassable() && feet.getType() != Material.WATER && feet.getType() != Material.LAVA &&
                    head.isPassable() && head.getType() != Material.WATER && head.getType() != Material.LAVA) {

                // Centramos al mob en el bloque respetando su rotación
                return new Location(world, target.getBlockX() + 0.5, target.getBlockY(), target.getBlockZ() + 0.5, currentLoc.getYaw(), currentLoc.getPitch());
            }
        }
        return null; // Si no encuentra un lugar seguro después de 15 intentos, se cancela el TP
    }

    public abstract boolean isCustomMob(Entity entity);
}