package mobcap.spawn;

import mobcap.config.MobCapConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class CustomSpawnManager implements Listener {
    private final JavaPlugin plugin;
    private final MobCapConfig config;
    private final Random random = new Random();

    public CustomSpawnManager(JavaPlugin plugin, MobCapConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!config.isCustomSpawnEnabled()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        EntityType entityType = entity.getType();

        SpawnMode spawnMode = config.getMobSpawnMode(entityType);
        if (spawnMode != SpawnMode.CUSTOM) {
            return;
        }

        World world = entity.getWorld();
        long time = world.getTime();
        boolean isDaytime = time >= 0 && time < 12300;

        if (isDaytime && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if (config.isOvalPatternEnabled() && config.isCloserToPlayerEnabled()) {
                Location spawnLocation = getOptimalSpawnLocation(entity.getLocation());
                if (spawnLocation != null && !spawnLocation.equals(entity.getLocation())) {
                    entity.teleport(spawnLocation);
                }
            }
        }
    }

    private Location getOptimalSpawnLocation(Location originalLocation) {
        Player nearestPlayer = getNearestPlayer(originalLocation);
        if (nearestPlayer == null) {
            return null;
        }

        Location playerLoc = nearestPlayer.getLocation();
        double radiusMultiplier = config.getRadiusMultiplier();

        double maxDistance = 24 * radiusMultiplier;
        double minDistance = 8 * radiusMultiplier;

        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance * 0.7;

            Location newLocation = new Location(
                    originalLocation.getWorld(),
                    x,
                    originalLocation.getWorld().getHighestBlockYAt((int) x, (int) z) + 1,
                    z
            );

            if (isValidSpawnLocation(newLocation)) {
                return newLocation;
            }
        }

        return null;
    }

    private Player getNearestPlayer(Location location) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isValidSpawnLocation(Location location) {
        return location.getBlock().isEmpty() &&
                location.clone().add(0, 1, 0).getBlock().isEmpty() &&
                !location.clone().subtract(0, 1, 0).getBlock().isEmpty();
    }
}