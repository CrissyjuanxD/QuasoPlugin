package BloodMoon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Busca suelo y espacio para el cuerpo completo; nunca genera chunks para una horda. */
public final class SafeSpawnFinder {
    private static final Set<Material> HAZARDS = EnumSet.of(Material.LAVA, Material.WATER,
            Material.FIRE, Material.SOUL_FIRE, Material.CACTUS, Material.MAGMA_BLOCK,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.POWDER_SNOW,
            Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.POINTED_DRIPSTONE);
    private final Random random;

    public record Size(double width, double height) {
        public Size {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Tamaño inválido");
        }
    }

    public SafeSpawnFinder(Random random) { this.random = random; }

    public Size sizeOf(Location origin, EntityType type) {
        // createEntity construye un ejemplar sin añadirlo al mundo ni disparar un spawn.
        Entity prototype = origin.getWorld().createEntity(origin, type.getEntityClass());
        return new Size(prototype.getWidth(), prototype.getHeight());
    }

    public Optional<Location> find(Location origin, int radius, Size size) {
        World world = origin.getWorld();
        if (world == null) return Optional.empty();
        radius = Math.max(1, Math.min(radius, 128));
        for (int attempt = 0; attempt < 32; attempt++) {
            int x = origin.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int ground = world.getHighestBlockYAt(x, z);
            Location candidate = new Location(world, x + 0.5, ground + 1.0, z + 0.5);
            if (isSafe(candidate, size)) return Optional.of(candidate);
        }
        return Optional.empty();
    }

    public static boolean isSafe(Location location, Size size) {
        World world = location.getWorld();
        if (world == null || location.getY() < world.getMinHeight() + 1
                || location.getY() + size.height() >= world.getMaxHeight()) return false;
        double half = size.width() / 2.0;
        BoundingBox body = new BoundingBox(location.getX() - half, location.getY() + 0.001,
                location.getZ() - half, location.getX() + half, location.getY() + size.height(),
                location.getZ() + half);
        int minX = (int) Math.floor(body.getMinX()), maxX = (int) Math.floor(body.getMaxX());
        int minZ = (int) Math.floor(body.getMinZ()), maxZ = (int) Math.floor(body.getMaxZ());
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
        }
        for (double x : new double[]{body.getMinX(), body.getMaxX()}) {
            for (double z : new double[]{body.getMinZ(), body.getMaxZ()}) {
                if (!world.getWorldBorder().isInside(new Location(world, x, location.getY(), z))) return false;
            }
        }
        Block ground = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        if (ground.isPassable() || ground.isLiquid() || HAZARDS.contains(ground.getType())) return false;
        // Excluye vallas y muros que sobresalen del bloque usado como suelo.
        if (world.hasCollisionsIn(body)) return false;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            for (int y = location.getBlockY() - 1; y <= (int) Math.floor(body.getMaxY()); y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.isLiquid() || HAZARDS.contains(block.getType())) return false;
            }
        }
        return true;
    }
}
