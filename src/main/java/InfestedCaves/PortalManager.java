package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class PortalManager implements Listener {
    // La llegada normal cae al azar entre -3000 y 3000 en X y en Z
    static final int RANDOM_RADIUS = 3000;
    private static final int RANDOM_ATTEMPTS = 12;

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final Set<Material> BAD_GROUND = EnumSet.of(
            Material.BEDROCK, Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SCULK_SHRIEKER, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.POINTED_DRIPSTONE);
    private static final Set<Material> BAD_SPACE = EnumSet.of(
            Material.FIRE, Material.SOUL_FIRE, Material.COBWEB, Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW);

    private final JavaPlugin plugin;
    private final NamespacedKey PORTAL_KEY;

    public PortalManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.PORTAL_KEY = new NamespacedKey(plugin, "infested_portal_active");

        startParticleTask();
    }

    // Partículas en todos los portales cada segundo (desde la 26.2 el dragon breath pide su potencia como dato)
    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(BlockDisplay.class)) {
                        if (entity.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                            world.spawnParticle(Particle.DRAGON_BREATH, entity.getLocation().add(0, 0.2, 0), 3, 1.5, 0, 1.5, 0.02, 1.0f);
                            world.spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 0.5, 0), 2, 1.0, 0.5, 1.0, 0.1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // El portal es un BlockDisplay acostado de terracota morada marcado con el PDC
    public void spawnPortal(Location loc) {
        World world = loc.getWorld();

        Location spawnLoc = loc.clone();
        spawnLoc.setPitch(0);
        spawnLoc.setYaw(0);
        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);
        spawnLoc.setY(Math.floor(spawnLoc.getY()) + 0.02);

        BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(Material.PURPLE_GLAZED_TERRACOTTA));

        Transformation transformation = new Transformation(
                new Vector3f(-2.0f, 0.0f, -2.0f),
                new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
                new Vector3f(4f, 4f, 0.1f),
                new AxisAngle4f(0, 0, 0, 0)
        );

        display.setTransformation(transformation);
        display.setGlowing(true);
        display.setGlowColorOverride(Color.PURPLE);

        display.getPersistentDataContainer().set(PORTAL_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    public void removePortalNearby(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof BlockDisplay) {
                if (entity.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    // Si el jugador pisa un portal empieza el teleport
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        if (p.hasMetadata("Teleporting")) return;

        if (p.getGameMode() == GameMode.SPECTATOR) return;

        for (Entity ent : p.getNearbyEntities(2.5, 2.0, 2.5)) {
            if (ent instanceof BlockDisplay) {
                if (ent.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                    Location portalLoc = ent.getLocation();
                    Location playerLoc = p.getLocation();

                    if (Math.abs(portalLoc.getX() - playerLoc.getX()) <= 2.0 &&
                            Math.abs(portalLoc.getZ() - playerLoc.getZ()) <= 2.0 &&
                            Math.abs(portalLoc.getY() - playerLoc.getY()) <= 1.5) {

                        triggerTeleport(p);
                        break;
                    }
                }
            }
        }
    }

    // Lo levita 4 segundos con efectos y después lo manda a la otra dimensión.
    // Si va a la Warden Cave, el lugar al azar se busca mientras levita
    private void triggerTeleport(Player p) {
        p.setMetadata("Teleporting", new FixedMetadataValue(plugin, true));

        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
        p.playSound(p.getLocation(), "minecraft:custom.transition_1", 10.0f, 1.3f);
        p.sendTitle("", "", 50, 80, 20);

        boolean leaving = p.getWorld().getName().equals(QuasoPlugin.WORLD_NAME);
        World infested = Bukkit.getWorld(QuasoPlugin.WORLD_NAME);
        CompletableFuture<Location> arrival = !leaving && infested != null ? findRandomSpawn(infested) : null;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || ticks >= 80) {
                    this.cancel();
                    return;
                }
                p.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.1);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.removeMetadata("Teleporting", plugin);
                p.removePotionEffect(PotionEffectType.LEVITATION);
                if (!p.isOnline()) return;

                if (leaving) {
                    teleportToOverworld(p);
                } else if (arrival == null) {
                    p.sendMessage(ChatColor.RED + "La dimensión WardenCave no está cargada.");
                } else {
                    arrival.thenAccept(location -> teleportToInfested(p, location));
                }
            }
        }.runTaskLater(plugin, 80L);
    }

    public void teleportToInfested(Player p, Location location) {
        if (!p.isOnline()) return;
        p.setFallDistance(0);
        p.teleportAsync(location).thenAccept(done -> p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f));
    }

    private void teleportToOverworld(Player p) {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = p.getRespawnLocation();
        if (spawn == null) spawn = overworld.getSpawnLocation();
        p.teleport(spawn);
        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1f);
    }

    // Llegada normal: un punto al azar a hasta 3.000 bloques del centro, siempre en un suelo seguro.
    // Los chunks se cargan en async; si en 12 intentos no hay lugar, cae en el centro
    public CompletableFuture<Location> findRandomSpawn(World world) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        tryRandomSpawn(world, result, 0);
        return result;
    }

    private void tryRandomSpawn(World world, CompletableFuture<Location> result, int attempt) {
        if (attempt >= RANDOM_ATTEMPTS) {
            result.complete(findCenterSpawn(world));
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int chunkX = Math.floorDiv(random.nextInt(-RANDOM_RADIUS, RANDOM_RADIUS + 1), 16);
        int chunkZ = Math.floorDiv(random.nextInt(-RANDOM_RADIUS, RANDOM_RADIUS + 1), 16);

        world.getChunkAtAsync(chunkX, chunkZ).whenComplete((chunk, error) -> {
            Location found = error == null ? safeInChunk(world, chunkX, chunkZ) : null;
            if (found != null) result.complete(found);
            else tryRandomSpawn(world, result, attempt + 1);
        });
    }

    // Recorre las columnas del chunk en un orden mezclado (sin los bordes, así no carga los chunks de al lado)
    // y se queda con la primera superficie segura bajo el cielo que no esté en una Ancient City
    private Location safeInChunk(World world, int chunkX, int chunkZ) {
        int start = ThreadLocalRandom.current().nextInt(196);
        for (int i = 0; i < 196; i++) {
            int index = (start + i * 37) % 196;
            int x = (chunkX << 4) + 1 + index % 14;
            int z = (chunkZ << 4) + 1 + index / 14;

            AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(world.getSeed(), x, z);
            if (city != null && AncientCityLocator.computeInfluence(city, x, z) > 0) continue;

            Location found = highestSafeBelow(world, x, z, WardenGenerator.TOP_Y - 4);
            if (found != null) return found;
        }
        return null;
    }

    // Spawn administrativo (/wardencave join <jugador> spawn): la meseta del centro en Y 100, donde está la build
    // con el portal de salida. Busca de 0 101 0 para abajo y alrededor, así no cae arriba del techo de la build
    public Location findCenterSpawn(World world) {
        for (int r = 0; r <= 60; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    Location found = highestSafeBelow(world, dx, dz, WardenGenerator.SPAWN_Y + 1);
                    if (found != null) return found;
                }
            }
        }
        return buildLanding(world, 0, 0);
    }

    private Location highestSafeBelow(World world, int x, int z, int fromY) {
        for (int y = Math.min(fromY, WardenGenerator.TOP_Y - 4); y > WardenGenerator.MIN_Y; y--) {
            if (isSafe(world, x, y, z)) return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    // Suelo firme y sin peligro, apoyado por al menos 3 lados (no la punta de un pico ni el borde de un precipicio),
    // 3 bloques libres arriba, abierto al menos por 2 lados (no un pozo de 1x1) y sin lava al lado
    private boolean isSafe(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y, z);
        if (!ground.getType().isSolid() || BAD_GROUND.contains(ground.getType())) return false;

        int support = 0;
        for (BlockFace face : HORIZONTAL) {
            if (ground.getRelative(face).getType().isSolid()) support++;
        }
        if (support < 3) return false;

        for (int dy = 1; dy <= 3; dy++) {
            Block space = world.getBlockAt(x, y + dy, z);
            if (!space.isPassable() || space.isLiquid() || BAD_SPACE.contains(space.getType())) return false;
        }

        int open = 0;
        Block feet = world.getBlockAt(x, y + 1, z);
        for (BlockFace face : HORIZONTAL) {
            Block side = feet.getRelative(face);
            if (side.getType() == Material.LAVA || side.getRelative(BlockFace.DOWN).getType() == Material.LAVA) return false;
            if (side.isPassable() && !side.isLiquid()) open++;
        }
        return open >= 2;
    }

    // Si no encontró nada arma un hueco mínimo con piso en la meseta, así nunca aparece adentro de la roca
    private Location buildLanding(World world, int x, int z) {
        int floorY = WardenGenerator.SPAWN_Y;
        world.getBlockAt(x, floorY, z).setType(Material.DEEPSLATE);
        for (int dy = 1; dy <= 3; dy++) world.getBlockAt(x, floorY + dy, z).setType(Material.AIR);
        return new Location(world, x + 0.5, floorY + 1, z + 0.5);
    }
}
