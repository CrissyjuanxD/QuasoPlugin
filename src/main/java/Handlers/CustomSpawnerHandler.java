package Handlers;

import Managers.MobManager;

import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// Los spawners custom: en el mundo son una barrera (la colisión) con un block display de spawner que gira encima, que
// guarda toda la configuración. Spawnean con una tarea propia en los 4 modos. Si en su configuración dice
// "Rompible: Sí" se rompen con 5 golpes de pico (como las estatuas); si no, solo un admin en creativo. Los spawners de
// antes (el bloque de spawner con la configuración adentro) se pasan solos al cargar su chunk
public class CustomSpawnerHandler implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey spawnerKey;
    private final NamespacedKey spawnModeKey;
    private final NamespacedKey breakableKey;
    private final NamespacedKey hpKey;

    // Golpes de pico para romperlo y lo que se espera entre golpe y golpe
    public static final int VIDA_MAXIMA = 5;
    private static final long ESPERA_GOLPE_MS = 1000L;
    // El spawner mide 0,7 bloques para que al girar no se meta en los bloques de al lado
    private static final float ESCALA = 0.7f;
    // Gira 15 grados cada 3 ticks (una vuelta cada 3,6 segundos) si hay alguien a 48 bloques
    private static final int TICKS_GIRO = 3;
    private static final float PASO_GIRO = (float) Math.toRadians(15);
    private static final double DISTANCIA_GIRO = 48;
    private static final String[] CONFIG_KEYS = {"spawn_count", "max_nearby", "player_range", "delay",
            "min_delay", "max_delay", "spawn_range"};

    private final Map<UUID, ItemStack> editingSpawners = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingProperties = new ConcurrentHashMap<>();
    private final Map<UUID, String> playersWaitingForInput = new ConcurrentHashMap<>();
    private final Map<UUID, Long> hitCooldowns = new HashMap<>();

    private final Map<Location, CustomSpawnerData> activeCustomSpawners = new ConcurrentHashMap<>();
    private BukkitRunnable customSpawnTask;
    private BukkitRunnable rotationTask;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60000;

    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final InfestedBeeHandler infestedBeeHandler;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CorruptedBee corruptedBee;

    public enum SpawnMode {
        VANILLA("Vanilla", Material.SPAWNER),
        SPAWN_CUSTOM("Spawn Custom", Material.REPEATER),
        SPAWN_CUSTOM_INFINITE("Spawn Custom Infinite", Material.COMMAND_BLOCK),
        ONE_SPAWN("One Spawn", Material.TNT);

        private final String displayName;
        private final Material icon;

        SpawnMode(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public SpawnMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private static class CustomSpawnerData {
        String mobType;
        int spawnCount;
        int maxNearby;
        int playerRange;
        int delay;
        int minDelay;
        int maxDelay;
        int spawnRange;
        SpawnMode spawnMode;
        long lastSpawnTime;
        long nextSpawnTime;
        boolean hasSpawnedOnce;
        UUID display;
        float angle;

        CustomSpawnerData(String mobType, Map<String, Integer> config, SpawnMode spawnMode, UUID display) {
            this.mobType = mobType;
            this.spawnCount = config.get("spawn_count");
            this.maxNearby = config.get("max_nearby");
            this.playerRange = config.get("player_range");
            this.delay = config.get("delay");
            this.minDelay = config.get("min_delay");
            this.maxDelay = config.get("max_delay");
            this.spawnRange = config.get("spawn_range");
            this.spawnMode = spawnMode;
            this.lastSpawnTime = System.currentTimeMillis();
            this.nextSpawnTime = this.lastSpawnTime + (this.delay * 50L);
            this.hasSpawnedOnce = false;
            this.display = display;
        }
    }

    public CustomSpawnerHandler(JavaPlugin plugin, InfestedBeeHandler infestedBeeHandler) {
        this.plugin = plugin;
        this.spawnerKey = new NamespacedKey(plugin, "custom_spawner");
        this.spawnModeKey = new NamespacedKey(plugin, "spawn_mode");
        this.breakableKey = new NamespacedKey(plugin, "spawner_rompible");
        this.hpKey = new NamespacedKey(plugin, "spawner_vida");

        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.infestedBeeHandler = infestedBeeHandler;
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedBee = new CorruptedBee(plugin);

        startCustomSpawnTask();
        startRotationTask();
        Bukkit.getScheduler().runTaskLater(plugin, this::loadAllCustomSpawners, 100L);
    }

    // Los spawners de antes (bloque de spawner con la configuración) se cambian por la barrera y el display
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!chunk.isLoaded()) return;
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof CreatureSpawner spawner && isCustomSpawner(spawner)) migrateLegacy(spawner);
            }
        }, 1L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof BlockDisplay display && isSpawnerDisplay(display)) registerDisplay(display);
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof BlockDisplay display && isSpawnerDisplay(display)) {
                activeCustomSpawners.remove(blockOf(display));
            }
        }
    }

    private void startCustomSpawnTask() {
        customSpawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                processCustomSpawners();
            }
        };
        customSpawnTask.runTaskTimer(plugin, 20L, 20L);
    }

    // Una sola tarea para todos: cada spawner con un jugador cerca avanza 15 grados (el cliente lo interpola)
    private void startRotationTask() {
        rotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, CustomSpawnerData> entry : activeCustomSpawners.entrySet()) {
                    Location loc = entry.getKey();
                    if (!loc.isChunkLoaded() || !playerNear(loc, DISTANCIA_GIRO)) continue;
                    Entity entity = Bukkit.getEntity(entry.getValue().display);
                    if (!(entity instanceof BlockDisplay display)) continue;
                    CustomSpawnerData data = entry.getValue();
                    data.angle = (data.angle + PASO_GIRO) % (float) (Math.PI * 2);
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(TICKS_GIRO);
                    display.setTransformation(transformation(data.angle));
                }
            }
        };
        rotationTask.runTaskTimer(plugin, 20L, TICKS_GIRO);
    }

    // Gira sobre su eje: el display está en el centro de abajo del bloque y el modelo se corre medio bloque (rotado)
    static Transformation transformation(float angle) {
        Quaternionf rotation = new Quaternionf().rotateY(angle);
        Vector3f offset = rotation.transform(new Vector3f(-ESCALA / 2, 0, -ESCALA / 2)).add(0, (1 - ESCALA) / 2, 0);
        return new Transformation(offset, rotation, new Vector3f(ESCALA, ESCALA, ESCALA), new Quaternionf());
    }

    private boolean playerNear(Location loc, double distance) {
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= distance * distance) return true;
        }
        return false;
    }

    // Cada segundo revisa los spawners custom: modo, delay, jugadores en rango y máximo de mobs cerca
    private void processCustomSpawners() {
        long timelimp = System.currentTimeMillis();

        if (timelimp - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupInvalidSpawners();
            lastCleanupTime = timelimp;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Location, CustomSpawnerData>> iterator = activeCustomSpawners.entrySet().iterator();
        long currentTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            Map.Entry<Location, CustomSpawnerData> entry = iterator.next();
            Location spawnerLoc = entry.getKey();
            CustomSpawnerData data = entry.getValue();

            if (!spawnerLoc.isChunkLoaded()) continue;

            if (!isValid(spawnerLoc, data)) {
                removeSpawner(spawnerLoc, data);
                iterator.remove();
                continue;
            }

            if (data.spawnMode == SpawnMode.SPAWN_CUSTOM_INFINITE && hasSoulTorchAbove(spawnerLoc)) {
                showDeactivatedParticles(spawnerLoc);
                continue;
            }

            if (data.spawnMode == SpawnMode.ONE_SPAWN && data.hasSpawnedOnce) {
                spawnerLoc.getWorld().spawnParticle(Particle.BLOCK, spawnerLoc.clone().add(0.5, 0.5, 0.5), 40, 0.3, 0.3, 0.3, 0.1,
                        Material.SPAWNER.createBlockData());
                removeSpawner(spawnerLoc, data);
                iterator.remove();
                continue;
            }

            if (currentTime < data.nextSpawnTime) continue;

            if (!isPlayerInRange(spawnerLoc, data.playerRange)) continue;

            boolean shouldCheckNearby = (data.spawnMode != SpawnMode.SPAWN_CUSTOM_INFINITE && data.spawnMode != SpawnMode.ONE_SPAWN);

            if (shouldCheckNearby && getNearbyEntitiesCount(spawnerLoc, data, data.spawnRange) >= data.maxNearby) continue;

            performCustomSpawn(spawnerLoc, data);

            if (data.spawnMode == SpawnMode.ONE_SPAWN) {
                data.hasSpawnedOnce = true;
            }

            int randomDelay = (data.spawnMode == SpawnMode.ONE_SPAWN) ? 0 : data.minDelay + (int)(Math.random() * (data.maxDelay - data.minDelay));
            data.nextSpawnTime = currentTime + (randomDelay * 50L);
        }
    }

    // Sigue en pie si está la barrera y el display; si alguien sacó uno de los dos (WorldEdit, /kill) se quita el resto
    private boolean isValid(Location loc, CustomSpawnerData data) {
        if (loc.getBlock().getType() != Material.BARRIER) return false;
        if (!loc.getChunk().isEntitiesLoaded()) return true;
        Entity display = Bukkit.getEntity(data.display);
        return display != null && display.isValid();
    }

    private void cleanupInvalidSpawners() {
        int removedCount = 0;
        Iterator<Map.Entry<Location, CustomSpawnerData>> iterator = activeCustomSpawners.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, CustomSpawnerData> entry = iterator.next();
            Location loc = entry.getKey();
            if (!loc.isChunkLoaded()) continue;

            if (!isValid(loc, entry.getValue())) {
                removeSpawner(loc, entry.getValue());
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Limpieza periódica: Eliminados " + removedCount + " spawners inválidos");
        }
    }

    private boolean hasSoulTorchAbove(Location spawnerLoc) {
        Block blockAbove = spawnerLoc.clone().add(0, 1, 0).getBlock();
        return blockAbove.getType() == Material.SOUL_TORCH || blockAbove.getType() == Material.SOUL_WALL_TORCH;
    }

    private void showDeactivatedParticles(Location spawnerLoc) {
        World world = spawnerLoc.getWorld();
        if (world == null) return;

        Location particleLoc = spawnerLoc.clone().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 3, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.SOUL, particleLoc, 5, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, particleLoc, 5, 0.2, 0.2, 0.2, 0.02);

        if (ThreadLocalRandom.current().nextInt(20) == 0) {
            world.playSound(spawnerLoc, Sound.BLOCK_SOUL_SAND_BREAK, 0.3f, 0.8f);
        }
    }

    private boolean isPlayerInRange(Location spawnerLoc, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return false;

        double rangeSquared = range * range;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(spawnerLoc) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    private int getNearbyEntitiesCount(Location spawnerLoc, CustomSpawnerData data, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return 0;

        int count = 0;
        EntityType targetType = getTargetEntityType(data.mobType);
        if (targetType == null) return 0;

        double checkRadius = 9.0;

        for (Entity entity : world.getNearbyEntities(spawnerLoc, checkRadius, checkRadius, checkRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                if (entity.getType() == targetType && !living.isDead()) count++;
            }
        }
        return count;
    }

    // Spawnea los mobs del spawner con partículas según el modo. En Vanilla, como un spawner normal, solo donde la luz
    // de bloques es 11 o menos
    private void performCustomSpawn(Location spawnerLoc, CustomSpawnerData data) {
        int spawnCount;
        if (data.spawnMode == SpawnMode.ONE_SPAWN) {
            spawnCount = data.spawnCount;
        } else {
            spawnCount = 1 + (int)(Math.random() * data.spawnCount);
        }

        for (int i = 0; i < spawnCount; i++) {
            Location spawnLoc = findValidSpawnLocation(spawnerLoc, data.spawnRange, data.spawnMode == SpawnMode.VANILLA);
            if (spawnLoc == null) continue;

            if (data.mobType.startsWith("vanilla_")) {
                spawnVanillaMob(data.mobType, spawnLoc);
            } else {
                spawnCustomMob(data.mobType, spawnLoc);
            }

            switch (data.spawnMode) {
                case ONE_SPAWN:
                    spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, spawnLoc, 5, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
                    break;
                case SPAWN_CUSTOM_INFINITE:
                    spawnLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 1.0f);
                    break;
                case VANILLA:
                    spawnLoc.getWorld().spawnParticle(Particle.POOF, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SCULK_BREAK, 3.0f, 0.1f);
                    break;
                default:
                    spawnLoc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SCULK_BREAK, 3.0f, 0.1f);
                    break;
            }
        }

        Location spawnerParticleLoc = spawnerLoc.clone().add(0.5, 0.5, 0.5);
        switch (data.spawnMode) {
            case ONE_SPAWN:
                spawnerLoc.getWorld().spawnParticle(Particle.CLOUD, spawnerParticleLoc, 20, 0.3, 0.3, 0.3, 0.1);
                break;
            case SPAWN_CUSTOM_INFINITE:
                spawnerLoc.getWorld().spawnParticle(Particle.SOUL, spawnerParticleLoc, 5, 0.2, 0.2, 0.2, 0.02);
                break;
            default:
                spawnerLoc.getWorld().spawnParticle(Particle.FLAME, spawnerParticleLoc, 8, 0.25, 0.25, 0.25, 0.01);
                break;
        }
    }

    private Location findValidSpawnLocation(Location spawnerLoc, int range, boolean darkOnly) {
        World world = spawnerLoc.getWorld();
        if (world == null) return null;

        Random random = ThreadLocalRandom.current();

        for (int attempts = 0; attempts < 10; attempts++) {
            double x = spawnerLoc.getX() + 0.5 + (random.nextDouble() * range * 2 - range);
            double z = spawnerLoc.getZ() + 0.5 + (random.nextDouble() * range * 2 - range);
            double y = spawnerLoc.getY() + (random.nextDouble() * 3 - 1);

            Location testLoc = new Location(world, x, Math.floor(y), z);

            if (isValidSpawnLocation(testLoc) && (!darkOnly || testLoc.getBlock().getLightFromBlocks() <= 11)) {
                return testLoc;
            }
        }

        return darkOnly ? null : spawnerLoc.clone().add(0.5, 1, 0.5);
    }

    private boolean isValidSpawnLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block block = world.getBlockAt(loc);
        Block above = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block below = world.getBlockAt(loc.clone().add(0, -1, 0));

        return !block.getType().isSolid() &&
                !above.getType().isSolid() &&
                below.getType().isSolid();
    }

    // Al poner el spawner custom se cambia por la barrera y el display con la configuración del item
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer itemContainer = meta.getPersistentDataContainer();
        if (!itemContainer.has(spawnerKey, PersistentDataType.STRING)) return;

        String mobType = itemContainer.get(spawnerKey, PersistentDataType.STRING);
        if (mobType == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        ItemStack copy = item.clone();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() != Material.SPAWNER) return;
            Map<String, Integer> config = readSpawnerConfig(copy);
            SpawnMode spawnMode = readSpawnModeConfig(copy);
            boolean breakable = readBreakable(copy);
            block.setType(Material.BARRIER, false);
            createSpawner(block, mobType, config, spawnMode, breakable, VIDA_MAXIMA);

            player.sendMessage(ChatColor.GREEN + "¡Spawner de " + mobType + " (" + spawnMode.getDisplayName() + ", "
                    + (breakable ? "rompible" : "irrompible") + ") colocado correctamente!");

            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        }, 2L);
    }

    // El display guarda todo en su PDC: así se vuelve a armar cuando se carga el chunk
    private void createSpawner(Block block, String mobType, Map<String, Integer> config, SpawnMode spawnMode,
                               boolean breakable, int hp) {
        Location loc = block.getLocation();
        BlockDisplay display = loc.getWorld().spawn(loc.clone().add(0.5, 0, 0.5), BlockDisplay.class, d -> {
            d.setBlock(Material.SPAWNER.createBlockData());
            d.setTransformation(transformation(0));
            d.setPersistent(true);
            PersistentDataContainer container = d.getPersistentDataContainer();
            container.set(spawnerKey, PersistentDataType.STRING, mobType);
            container.set(spawnModeKey, PersistentDataType.STRING, spawnMode.name());
            container.set(breakableKey, PersistentDataType.BYTE, (byte) (breakable ? 1 : 0));
            container.set(hpKey, PersistentDataType.INTEGER, hp);
            for (Map.Entry<String, Integer> entry : config.entrySet()) {
                container.set(new NamespacedKey(plugin, entry.getKey()), PersistentDataType.INTEGER, entry.getValue());
            }
        });
        activeCustomSpawners.put(loc, new CustomSpawnerData(mobType, config, spawnMode, display.getUniqueId()));
    }

    // Pasa un spawner de antes al sistema nuevo: rompible y con la vida entera
    private void migrateLegacy(CreatureSpawner spawner) {
        PersistentDataContainer container = spawner.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        if (mobType == null) return;
        SpawnMode spawnMode = parseMode(container.get(spawnModeKey, PersistentDataType.STRING));
        Map<String, Integer> config = readConfig(container);
        Block block = spawner.getBlock();
        block.setType(Material.BARRIER, false);
        createSpawner(block, mobType, config, spawnMode, true, VIDA_MAXIMA);
        plugin.getLogger().info("Spawner custom de " + mobType + " en " + block.getX() + " " + block.getY() + " "
                + block.getZ() + " pasado al block display.");
    }

    public void loadAllCustomSpawners() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof CreatureSpawner spawner && isCustomSpawner(spawner)) migrateLegacy(spawner);
                }
            }
            for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) {
                if (isSpawnerDisplay(display)) registerDisplay(display);
            }
        }
    }

    private void registerDisplay(BlockDisplay display) {
        Location loc = blockOf(display);
        CustomSpawnerData current = activeCustomSpawners.get(loc);
        if (current != null && current.display.equals(display.getUniqueId())) return;

        PersistentDataContainer container = display.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        if (mobType == null) return;
        SpawnMode spawnMode = parseMode(container.get(spawnModeKey, PersistentDataType.STRING));
        activeCustomSpawners.put(loc, new CustomSpawnerData(mobType, readConfig(container), spawnMode, display.getUniqueId()));
    }

    private SpawnMode parseMode(String name) {
        if (name == null) return SpawnMode.VANILLA;
        try {
            return SpawnMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Modo de spawn inválido: " + name);
            return SpawnMode.VANILLA;
        }
    }

    // Los valores guardados encima de los de siempre (por si falta alguno)
    private Map<String, Integer> readConfig(PersistentDataContainer container) {
        Map<String, Integer> config = defaultConfig();
        for (String key : CONFIG_KEYS) {
            Integer value = container.get(new NamespacedKey(plugin, key), PersistentDataType.INTEGER);
            if (value != null) config.put(key, value);
        }
        return config;
    }

    private static Map<String, Integer> defaultConfig() {
        Map<String, Integer> config = new HashMap<>();
        config.put("spawn_count", 4);
        config.put("max_nearby", 6);
        config.put("player_range", 20);
        config.put("delay", 40);
        config.put("min_delay", 200);
        config.put("max_delay", 600);
        config.put("spawn_range", 4);
        return config;
    }

    private static Location blockOf(Entity display) {
        Location loc = display.getLocation();
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean isSpawnerDisplay(Entity entity) {
        return entity.getPersistentDataContainer().has(spawnerKey, PersistentDataType.STRING);
    }

    // Saca la barrera y el display
    private void removeSpawner(Location loc, CustomSpawnerData data) {
        if (loc.getBlock().getType() == Material.BARRIER) loc.getBlock().setType(Material.AIR);
        Entity display = Bukkit.getEntity(data.display);
        if (display != null) display.remove();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  GOLPES Y ROTURA
    // ──────────────────────────────────────────────────────────────────────────

    // Con 5 golpes de pico (uno por segundo) se rompe si es rompible; en creativo se rompe de una
    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;
        CustomSpawnerData data = activeCustomSpawners.get(block.getLocation());
        if (data == null) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!(Bukkit.getEntity(data.display) instanceof BlockDisplay display)) return;

        PersistentDataContainer container = display.getPersistentDataContainer();
        if (container.getOrDefault(breakableKey, PersistentDataType.BYTE, (byte) 1) == 0) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.getType().name().endsWith("_PICKAXE")) return;

        long now = System.currentTimeMillis();
        if (now - hitCooldowns.getOrDefault(player.getUniqueId(), 0L) < ESPERA_GOLPE_MS) return;
        hitCooldowns.put(player.getUniqueId(), now);

        int hp = container.getOrDefault(hpKey, PersistentDataType.INTEGER, VIDA_MAXIMA) - 1;
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().playSound(center, Sound.ITEM_MACE_SMASH_GROUND, SoundCategory.BLOCKS, 1.0F, 2.0F);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 25, 0.3, 0.3, 0.3, 0.1, Material.SPAWNER.createBlockData());

        if (hp <= 0) {
            breakSpawner(block.getLocation(), data);
        } else {
            container.set(hpKey, PersistentDataType.INTEGER, hp);
            spawnDamageDisplay(block.getLocation(), hp);
        }
        player.setCooldown(hand.getType(), 20);
    }

    // En creativo se saca con un golpe aunque sea irrompible
    @EventHandler(ignoreCancelled = true)
    public void onCreativeBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARRIER) return;
        CustomSpawnerData data = activeCustomSpawners.remove(block.getLocation());
        if (data == null) return;
        Entity display = Bukkit.getEntity(data.display);
        if (display != null) display.remove();
        event.getPlayer().sendMessage(ChatColor.YELLOW + "Spawner custom de " + data.mobType + " quitado.");
    }

    // Al romperse suelta experiencia como un spawner normal
    private void breakSpawner(Location loc, CustomSpawnerData data) {
        activeCustomSpawners.remove(loc);
        removeSpawner(loc, data);
        World world = loc.getWorld();
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.BLOCK, center, 80, 0.3, 0.4, 0.3, 0.15, Material.SPAWNER.createBlockData());
        world.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.8f);
        world.playSound(center, Sound.ITEM_SHIELD_BREAK, 1.0F, 0.75F);
        int xp = 15 + ThreadLocalRandom.current().nextInt(29);
        world.spawn(center, ExperienceOrb.class, orb -> orb.setExperience(xp));
    }

    // La vida que le queda sube y se desvanece arriba del spawner (solo texto, sin íconos)
    private void spawnDamageDisplay(Location spawnerLoc, int currentHp) {
        Location spawnLoc = spawnerLoc.clone().add(0.5, 1.3, 0.5);
        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setPersistent(false);
            d.setViewRange(24.0f / 64.0f);
            d.setText(net.md_5.bungee.api.ChatColor.of("#67B3E0") + "" + ChatColor.BOLD + currentHp
                    + net.md_5.bungee.api.ChatColor.of("#DBECF2") + ChatColor.BOLD + "/"
                    + net.md_5.bungee.api.ChatColor.of("#43A0DE") + ChatColor.BOLD + VIDA_MAXIMA);
        });

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }
                ticks++;
                if (ticks > maxTicks) {
                    display.remove();
                    cancel();
                    return;
                }
                Transformation transform = display.getTransformation();
                transform.getTranslation().add(0f, 0.02f, 0f);
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
                display.setTransformation(transform);

                int fadeStart = maxTicks - 20;
                if (ticks > fadeStart) {
                    float progress = Math.max(0, (maxTicks - ticks) / 20.0f);
                    display.setTextOpacity((byte) (255 * progress));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CONFIGURACIÓN DEL ITEM
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Integer> readSpawnerConfig(ItemStack spawnerItem) {
        Map<String, Integer> config = defaultConfig();

        if (spawnerItem.hasItemMeta() && spawnerItem.getItemMeta().hasLore()) {
            List<String> lore = spawnerItem.getItemMeta().getLore();
            for (String line : lore) {
                String strippedLine = ChatColor.stripColor(line);
                if (strippedLine.contains(":")) {
                    String[] parts = strippedLine.split(":");
                    if (parts.length >= 2) {
                        String key = parts[0].trim().toLowerCase().replace(" ", "_");
                        String valueStr = parts[1].trim();

                        try {
                            int value = Integer.parseInt(valueStr);

                            switch (key) {
                                case "spawn_count":
                                    config.put("spawn_count", value);
                                    break;
                                case "max_nearby":
                                    config.put("max_nearby", value);
                                    break;
                                case "player_range":
                                    config.put("player_range", value);
                                    break;
                                case "initial_delay":
                                    config.put("delay", value);
                                    break;
                                case "min_delay":
                                    config.put("min_delay", value);
                                    break;
                                case "max_delay":
                                    config.put("max_delay", value);
                                    break;
                                case "spawn_range":
                                    config.put("spawn_range", value);
                                    break;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        return config;
    }

    private SpawnMode readSpawnModeConfig(ItemStack spawnerItem) {
        if (spawnerItem.hasItemMeta() && spawnerItem.getItemMeta().hasLore()) {
            for (String line : spawnerItem.getItemMeta().getLore()) {
                String strippedLine = ChatColor.stripColor(line).toLowerCase();
                if (strippedLine.contains("spawn mode:")) {
                    String[] parts = strippedLine.split(":");
                    if (parts.length > 1) {
                        String mode = parts[1].trim();
                        for (SpawnMode spawnMode : SpawnMode.values()) {
                            if (spawnMode.getDisplayName().toLowerCase().equals(mode)) {
                                return spawnMode;
                            }
                        }
                    }
                }
            }
        }
        return SpawnMode.VANILLA;
    }

    // "Rompible: Sí" o "Rompible: No" en el lore; los items de antes no lo tienen y son rompibles
    static boolean readBreakable(List<String> lore) {
        if (lore == null) return true;
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line).trim().toLowerCase(Locale.ROOT);
            if (stripped.startsWith("rompible:")) return !stripped.substring("rompible:".length()).trim().equals("no");
        }
        return true;
    }

    private boolean readBreakable(ItemStack spawnerItem) {
        return readBreakable(spawnerItem.hasItemMeta() ? spawnerItem.getItemMeta().getLore() : null);
    }

    private void setLoreLine(ItemStack spawner, String propertyDisplay, String value) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String newLine = ChatColor.GRAY + propertyDisplay + ": " + ChatColor.WHITE + value;

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(propertyDisplay + ":")) {
                lore.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            int insertIndex = -1;
            for (int i = 0; i < lore.size(); i++) {
                if (ChatColor.stripColor(lore.get(i)).contains("Spawn Range:")) {
                    insertIndex = i + 1;
                    break;
                }
            }
            if (insertIndex == -1) insertIndex = lore.size();
            lore.add(insertIndex, newLine);
        }

        meta.setLore(lore);
        spawner.setItemMeta(meta);
    }

    private void updateSpawnModeConfig(ItemStack spawner, SpawnMode spawnMode) {
        setLoreLine(spawner, "Spawn Mode", spawnMode.getDisplayName());
    }

    // Los spawners de antes que todavía no se pasaron no sacan mobs solos (los saca la tarea cuando ya son display)
    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (event.getSpawner() != null && isCustomSpawner(event.getSpawner())) event.setCancelled(true);
    }

    private void spawnVanillaMob(String mobType, Location location) {
        String vanillaType = mobType.substring(8);
        try {
            EntityType entityType = EntityType.valueOf(vanillaType.toUpperCase());
            LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tipo de mob vanilla no válido: " + vanillaType);
        }
    }

    private EntityType getTargetEntityType(String mobType) {
        if (mobType.startsWith("vanilla_")) {
            String vanillaType = mobType.substring(8);
            try {
                return EntityType.valueOf(vanillaType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return EntityType.PIG;
            }
        } else {
            return getBaseEntityType(mobType);
        }
    }

    private void spawnCustomMob(String mobType, Location location) {
        switch (MobManager.normalizeId(mobType)) {
            case "creeperfloral": bombitaSpawner.spawnBombita(location); break;
            case "iceologer": iceologerSpawner.spawnIceologer(location); break;
            case "zombiefloral": corruptedZombieSpawner.spawnCorruptedZombie(location); break;
            case "spiderfloral": corruptedSpider.spawnCorruptedSpider(location); break;
            case "abejafloral": QueenBeeHandler.spawn(plugin, location); break;
            case "infestedbee": infestedBeeHandler.spawnInfestedBee(location); break;
            case "guardianblaze": guardianBlaze.spawnGuardianBlaze(location); break;
            case "guardiancorruptedskeleton": guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location); break;
            case "corruptedinfernalspider": corruptedInfernalSpider.spawnCorruptedInfernalSpider(location); break;
            case "corruptedbee": corruptedBee.spawnCorruptedBee(location); break;
            default: plugin.getLogger().warning("Tipo de mob desconocido en spawner: " + mobType); break;
        }
    }

    // Mob vanilla que se muestra dentro del spawner para cada mob custom
    private EntityType getBaseEntityType(String mobType) {
        switch (MobManager.normalizeId(mobType)) {
            case "creeperfloral": case "corruptedcreeper": case "infernalcreeper": case "endercreeper": case "darkcreeper": return EntityType.CREEPER;
            case "iceologer": return EntityType.ILLUSIONER;
            case "zombiefloral": return EntityType.ZOMBIE;
            case "spiderfloral": case "corruptedinfernalspider": return EntityType.SPIDER;
            case "toxicspider": return EntityType.CAVE_SPIDER;
            case "abejafloral": case "hellishbee": case "infestedbee": case "corruptedbee": return EntityType.BEE;
            case "guardianblaze": return EntityType.BLAZE;
            case "guardiancorruptedskeleton": return EntityType.WITHER_SKELETON;
            case "corruptedskeleton": case "darkskeleton": return EntityType.SKELETON;
            case "buffbreeze": return EntityType.BREEZE;
            case "invertedghast": case "enderghast": case "piglinglobo": return EntityType.GHAST;
            case "netheritevexguardian": case "darkvex": return EntityType.VEX;
            case "ultrawitherboss": return EntityType.WITHER;
            case "whiteenderman": return EntityType.ENDERMAN;
            case "fastravager": return EntityType.RAVAGER;
            case "bruteimperial": return EntityType.PIGLIN_BRUTE;
            case "infernalbeast": return EntityType.HOGLIN;
            case "batboom": return EntityType.BAT;
            case "endersilverfish": return EntityType.SILVERFISH;
            case "guardianshulker": return EntityType.SHULKER;
            case "spectraleeye": return EntityType.PHANTOM;
            case "corrupteddrowned": return EntityType.DROWNED;
            default: return null;
        }
    }

    private String getCustomMobName(String mobType) {
        switch (MobManager.normalizeId(mobType)) {
            case "creeperfloral": return "Creeper Floral";
            case "iceologer": return "Iceologer";
            case "zombiefloral": return "Zombie Floral";
            case "spiderfloral": return "Spider Floral";
            case "abejafloral": return "Abeja Floral";
            case "hellishbee": return "Abeja Infernal";
            case "infestedbee": return "Infested Bee";
            case "guardianblaze": return "Guardian Blaze";
            case "guardiancorruptedskeleton": return "Guardian Corrupted Skeleton";
            case "corruptedskeleton": return "Corrupted Skeleton";
            case "corruptedinfernalspider": return "Corrupted Infernal Spider";
            case "corruptedcreeper": return "Corrupted Creeper";
            case "corruptedmagma": return "Corrupted Magma Cube";
            case "piglinglobo": return "Piglin Globo";
            case "buffbreeze": return "Buff Breeze";
            case "invertedghast": return "Inverted Ghast";
            case "netheritevexguardian": return "Netherite Vex Guardian";
            case "ultrawitherboss": return "Corrupted Wither Boss";
            case "whiteenderman": return "White Enderman";
            case "infernalcreeper": return "Infernal Creeper";
            case "toxicspider": return "Toxic Spider";
            case "fastravager": return "Fast Ravager";
            case "bruteimperial": return "Brute Imperial";
            case "batboom": return "Bat Boom";
            case "spectraleeye": return "Ojo Espectral";
            case "enderghast": return "Ender Ghast";
            case "endercreeper": return "Ender Creeper";
            case "endersilverfish": return "Ender Silverfish";
            case "guardianshulker": return "Guardian Shulker";
            case "darkphantom": return "Dark Phantom";
            case "darkcreeper": return "Dark Creeper";
            case "darkvex": return "Dark Vex";
            case "darkskeleton": return "Dark Skeleton";
            case "infernalbeast": return "Infernal Beast";
            case "corrupteddrowned": return "Corrupted Drowned";
            case "corruptedbee": return "Mini Abeja Floral";
            default: return null;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        if (!event.getPlayer().isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SPAWNER) return;

        event.setCancelled(true);
        openConfigGUI(event.getPlayer(), item);
    }

    // Menú para editar los valores del spawner (shift + click derecho con el spawner en la mano)
    public void openConfigGUI(Player player, ItemStack spawnerItem) {
        Inventory gui = Bukkit.createInventory(player, 27, "Configuración del Spawner");

        editingSpawners.put(player.getUniqueId(), spawnerItem);

        Map<String, Integer> currentConfig = readSpawnerConfig(spawnerItem);
        SpawnMode currentSpawnMode = readSpawnModeConfig(spawnerItem);

        gui.setItem(0, createConfigItem("Spawn Count", currentConfig.get("spawn_count")));
        gui.setItem(1, createConfigItem("Max Nearby", currentConfig.get("max_nearby")));
        gui.setItem(2, createConfigItem("Player Range", currentConfig.get("player_range")));
        gui.setItem(3, createConfigItem("Initial Delay", currentConfig.get("delay")));
        gui.setItem(4, createConfigItem("Min Delay", currentConfig.get("min_delay")));
        gui.setItem(5, createConfigItem("Max Delay", currentConfig.get("max_delay")));
        gui.setItem(6, createConfigItem("Spawn Range", currentConfig.get("spawn_range")));
        gui.setItem(7, createSpawnModeItem(currentSpawnMode));
        gui.setItem(8, createBreakableItem(readBreakable(spawnerItem)));

        gui.setItem(18, createInfoItem(SpawnMode.VANILLA));
        gui.setItem(19, createInfoItem(SpawnMode.SPAWN_CUSTOM));
        gui.setItem(20, createInfoItem(SpawnMode.SPAWN_CUSTOM_INFINITE));
        gui.setItem(21, createInfoItem(SpawnMode.ONE_SPAWN));

        player.openInventory(gui);
    }

    private void updateGUI(Player player, ItemStack spawnerItem) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui == null) return;

        Map<String, Integer> currentConfig = readSpawnerConfig(spawnerItem);
        SpawnMode currentSpawnMode = readSpawnModeConfig(spawnerItem);

        gui.setItem(0, createConfigItem("Spawn Count", currentConfig.get("spawn_count")));
        gui.setItem(1, createConfigItem("Max Nearby", currentConfig.get("max_nearby")));
        gui.setItem(2, createConfigItem("Player Range", currentConfig.get("player_range")));
        gui.setItem(3, createConfigItem("Initial Delay", currentConfig.get("delay")));
        gui.setItem(4, createConfigItem("Min Delay", currentConfig.get("min_delay")));
        gui.setItem(5, createConfigItem("Max Delay", currentConfig.get("max_delay")));
        gui.setItem(6, createConfigItem("Spawn Range", currentConfig.get("spawn_range")));
        gui.setItem(7, createSpawnModeItem(currentSpawnMode));
        gui.setItem(8, createBreakableItem(readBreakable(spawnerItem)));

        gui.setItem(18, createInfoItem(SpawnMode.VANILLA));
        gui.setItem(19, createInfoItem(SpawnMode.SPAWN_CUSTOM));
        gui.setItem(20, createInfoItem(SpawnMode.SPAWN_CUSTOM_INFINITE));
        gui.setItem(21, createInfoItem(SpawnMode.ONE_SPAWN));

        player.updateInventory();
    }

    private ItemStack createConfigItem(String name, int currentValue) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Valor actual: " + ChatColor.WHITE + currentValue,
                "",
                ChatColor.YELLOW + "Click para modificar"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSpawnModeItem(SpawnMode spawnMode) {
        ItemStack item = new ItemStack(spawnMode.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Modo de Spawn");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Modo actual: " + ChatColor.WHITE + spawnMode.getDisplayName());
        lore.add("");
        lore.add(ChatColor.GRAY + "Características:");

        switch (spawnMode) {
            case VANILLA:
                lore.add(ChatColor.GREEN + "✓ Comportamiento vanilla");
                lore.add(ChatColor.GREEN + "✓ No afectado por Soul Torch");
                break;
            case SPAWN_CUSTOM:
                lore.add(ChatColor.GREEN + "✓ No le afecta el nivel de luz");
                lore.add(ChatColor.GREEN + "✓ Spawn count aleatorio");
                break;
            case SPAWN_CUSTOM_INFINITE:
                lore.add(ChatColor.GREEN + "✓ Spawn count aleatorio");
                lore.add(ChatColor.GREEN + "✓ Spawn Infinitamente");
                lore.add(ChatColor.RED + "✗ Ignora Max Nearby");
                lore.add(ChatColor.YELLOW + "✓ Desactivado por Soul Torch");
                break;
            case ONE_SPAWN:
                lore.add(ChatColor.GREEN + "✓ Spawn count exacto");
                lore.add(ChatColor.RED + "✗ Ignora Max Nearby");
                lore.add(ChatColor.RED + "✗ Solo spawn una vez");
                lore.add(ChatColor.GREEN + "✓ No afectado por Soul Torch");
                break;
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Click para cambiar modo");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBreakableItem(boolean breakable) {
        ItemStack item = new ItemStack(breakable ? Material.IRON_PICKAXE : Material.BEDROCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Rompible");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Valor actual: " + (breakable ? ChatColor.GREEN + "Sí" : ChatColor.RED + "No"),
                "",
                ChatColor.GRAY + "Sí: se rompe con " + VIDA_MAXIMA + " golpes de pico.",
                ChatColor.GRAY + "No: solo lo saca un admin en creativo.",
                "",
                ChatColor.YELLOW + "Click para cambiar"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(SpawnMode spawnMode) {
        ItemStack item = new ItemStack(spawnMode.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + spawnMode.getDisplayName());

        List<String> lore = new ArrayList<>();
        switch (spawnMode) {
            case VANILLA:
                lore.add(ChatColor.GRAY + "Comportamiento vanilla normal");
                lore.add(ChatColor.GRAY + "No usa el sistema custom");
                break;
            case SPAWN_CUSTOM:
                lore.add(ChatColor.GRAY + "Sistema custom mejorado");
                lore.add(ChatColor.GRAY + "Respeta Max Nearby entities");
                lore.add(ChatColor.GRAY + "Spawn count aleatorio");
                break;
            case SPAWN_CUSTOM_INFINITE:
                lore.add(ChatColor.GRAY + "Spawn infinito e incontrolable");
                lore.add(ChatColor.GRAY + "Ignora Max Nearby entities");
                lore.add(ChatColor.RED + "Se desactiva con Soul Torch");
                break;
            case ONE_SPAWN:
                lore.add(ChatColor.GRAY + "Spawn único y preciso");
                lore.add(ChatColor.GRAY + "Ignora Max Nearby entities");
                lore.add(ChatColor.GRAY + "Se destruye después del spawn");
                lore.add(ChatColor.GRAY + "Spawn count exacto (no aleatorio)");
                break;
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("Configuración del Spawner")) {
            handleConfigGUIClick(event, player);
        }
    }

    private void handleConfigGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        // Solo cuentan los clics en el menú, no en el inventario del jugador
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getSlot();

        if (slot == 7) {
            handleSpawnModeChange(player);
            return;
        }

        if (slot == 8) {
            handleBreakableChange(player);
            return;
        }

        if (slot < 0 || slot > 6) return;

        String[] propertyNames = {
                "spawn_count", "max_nearby", "player_range",
                "delay", "min_delay", "max_delay", "spawn_range"
        };

        String property = propertyNames[slot];
        editingProperties.put(player.getUniqueId(), property);

        playersWaitingForInput.put(player.getUniqueId(), property);
        player.closeInventory();

        player.sendMessage(ChatColor.GOLD + "Ingresa el nuevo valor para " +
                getPropertyDisplayName(property) + ChatColor.GOLD + ":");
        player.sendMessage(ChatColor.GRAY + "(Escribe 'cancelar' para abortar)");
        player.sendMessage(ChatColor.GRAY + "Valor actual: " + ChatColor.WHITE +
                getCurrentValue(player, property));
    }

    private void handleSpawnModeChange(Player player) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        if (spawner == null) return;

        SpawnMode currentMode = readSpawnModeConfig(spawner);
        SpawnMode newMode = currentMode.next();

        updateSpawnModeConfig(spawner, newMode);

        player.sendMessage(ChatColor.GREEN + "Modo de spawn cambiado a: " +
                ChatColor.WHITE + newMode.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);

        updateGUI(player, spawner);
    }

    private void handleBreakableChange(Player player) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        if (spawner == null) return;

        boolean breakable = !readBreakable(spawner);
        setLoreLine(spawner, "Rompible", breakable ? "Sí" : "No");
        player.sendMessage(ChatColor.GREEN + "El spawner ahora es " + ChatColor.WHITE + (breakable ? "rompible" : "irrompible"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);

        updateGUI(player, spawner);
    }

    // Lee por chat el nuevo valor del spawner que se está editando
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playersWaitingForInput.containsKey(uuid)) return;

        event.setCancelled(true);

        String property = playersWaitingForInput.get(uuid);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancelar")) {
            player.sendMessage(ChatColor.RED + "Edición cancelada");
            playersWaitingForInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (editingSpawners.containsKey(uuid)) {
                    openConfigGUI(player, editingSpawners.get(uuid));
                }
            });
            return;
        }

        try {
            int newValue = Integer.parseInt(input);
            ItemStack spawner = editingSpawners.get(uuid);

            if (!isValidValue(property, newValue)) {
                player.sendMessage(ChatColor.RED + "Valor inválido para " +
                        getPropertyDisplayName(property));
                player.sendMessage(ChatColor.GOLD + "Ingresa un nuevo valor o escribe 'cancelar':");
                return;
            }

            updateSpawnerConfig(spawner, property, newValue);
            player.sendMessage(ChatColor.GREEN + getPropertyDisplayName(property) +
                    " actualizado a " + newValue);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);

            playersWaitingForInput.remove(uuid);
            editingProperties.remove(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                openConfigGUI(player, spawner);
            });

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Debes ingresar un número válido");
            player.sendMessage(ChatColor.GOLD + "Ingresa un nuevo valor o escribe 'cancelar':");
        }
    }

    private boolean isValidValue(String property, int value) {
        switch (property) {
            case "spawn_count": return value > 0 && value <= 64;
            case "max_nearby": return value > 0 && value <= 100;
            case "player_range": return value > 0 && value <= 128;
            case "delay":
            case "min_delay":
            case "max_delay": return value >= 5 && value <= 3600;
            case "spawn_range": return value > 0 && value <= 32;
            default: return value > 0;
        }
    }

    private int getCurrentValue(Player player, String property) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        Map<String, Integer> config = readSpawnerConfig(spawner);
        return config.get(property);
    }

    private String getPropertyDisplayName(String property) {
        switch (property) {
            case "spawn_count": return "Spawn Count";
            case "max_nearby": return "Max Nearby";
            case "player_range": return "Player Range";
            case "delay": return "Initial Delay";
            case "min_delay": return "Min Delay";
            case "max_delay": return "Max Delay";
            case "spawn_range": return "Spawn Range";
            default: return property;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        String title = event.getView().getTitle();

        if (title.equals("Configuración del Spawner")) {
            if (!playersWaitingForInput.containsKey(uuid)) {
                editingSpawners.remove(uuid);
                editingProperties.remove(uuid);
            }
        }
    }

    private void updateSpawnerConfig(ItemStack spawner, String property, int value) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;

        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();

        String propertyDisplay = getPropertyDisplayName(property);
        String newLine = ChatColor.GRAY + propertyDisplay + ": " + ChatColor.WHITE + value;

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(propertyDisplay + ":")) {
                lore.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(newLine);
        }

        meta.setLore(lore);
        spawner.setItemMeta(meta);
    }

    private boolean isCustomSpawner(CreatureSpawner spawner) {
        return spawner.getPersistentDataContainer().has(spawnerKey, PersistentDataType.STRING);
    }

    public void clearActiveSpawners() {
        activeCustomSpawners.clear();
    }

    public void shutdown() {
        if (customSpawnTask != null && !customSpawnTask.isCancelled()) {
            customSpawnTask.cancel();
        }
        if (rotationTask != null && !rotationTask.isCancelled()) {
            rotationTask.cancel();
        }
        activeCustomSpawners.clear();
    }
}