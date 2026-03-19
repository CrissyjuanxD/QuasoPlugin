package Handlers;

import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomSpawnerHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final NamespacedKey spawnerKey;
    private final NamespacedKey spawnModeKey;

    private final Map<UUID, ItemStack> editingSpawners = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingProperties = new ConcurrentHashMap<>();
    private final Map<UUID, String> playersWaitingForInput = new ConcurrentHashMap<>();

    private final Map<Location, CustomSpawnerData> activeCustomSpawners = new ConcurrentHashMap<>();
    private BukkitRunnable customSpawnTask;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60000;

    // Instancias de los mobs
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

        CustomSpawnerData(String mobType, Map<String, Integer> config, SpawnMode spawnMode) {
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
        }
    }

    public CustomSpawnerHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.spawnerKey = new NamespacedKey(plugin, "custom_spawner");
        this.spawnModeKey = new NamespacedKey(plugin, "spawn_mode");

        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin, dayHandler);
        this.infestedBeeHandler = new InfestedBeeHandler(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedBee = new CorruptedBee(plugin);

        startCustomSpawnTask();
        Bukkit.getScheduler().runTaskLater(plugin, this::loadAllCustomSpawners, 100L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();

        new BukkitRunnable() {
            @Override
            public void run() {
                int spawnersFound = 0;
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof CreatureSpawner) {
                        CreatureSpawner spawner = (CreatureSpawner) blockState;
                        if (isCustomSpawner(spawner)) {
                            Location loc = spawner.getLocation();
                            if (!activeCustomSpawners.containsKey(loc)) {
                                registerCustomSpawner(spawner);
                                spawnersFound++;
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        World world = event.getChunk().getWorld();

        activeCustomSpawners.keySet().removeIf(loc ->
                loc.getWorld().equals(world) &&
                        (loc.getBlockX() >> 4) == chunkX &&
                        (loc.getBlockZ() >> 4) == chunkZ
        );
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

            int chunkX = spawnerLoc.getBlockX() >> 4;
            int chunkZ = spawnerLoc.getBlockZ() >> 4;
            if (!spawnerLoc.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            if (spawnerLoc.getBlock().getType() != Material.SPAWNER) {
                iterator.remove();
                continue;
            }

            if (data.spawnMode == SpawnMode.SPAWN_CUSTOM_INFINITE && hasSoulTorchAbove(spawnerLoc)) {
                showDeactivatedParticles(spawnerLoc);
                continue;
            }

            if (data.spawnMode == SpawnMode.ONE_SPAWN && data.hasSpawnedOnce) {
                spawnerLoc.getBlock().breakNaturally();
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

    private void cleanupInvalidSpawners() {
        int removedCount = 0;
        Iterator<Map.Entry<Location, CustomSpawnerData>> iterator = activeCustomSpawners.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, CustomSpawnerData> entry = iterator.next();
            Location loc = entry.getKey();

            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;

            if (!loc.getWorld().isChunkLoaded(cx, cz)) {
                continue;
            }

            if (loc.getBlock().getType() != Material.SPAWNER) {
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

        Random random = new Random();
        if (random.nextInt(20) == 0) {
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
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity living = (LivingEntity) entity;

                if (entity.getType() == targetType && !living.isDead()) {
                    Location entityLoc = entity.getLocation();
                    double deltaX = Math.abs(entityLoc.getX() - spawnerLoc.getX());
                    double deltaY = Math.abs(entityLoc.getY() - spawnerLoc.getY());
                    double deltaZ = Math.abs(entityLoc.getZ() - spawnerLoc.getZ());

                    if (deltaX <= checkRadius && deltaY <= checkRadius && deltaZ <= checkRadius) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void performCustomSpawn(Location spawnerLoc, CustomSpawnerData data) {
        int spawnCount;
        if (data.spawnMode == SpawnMode.ONE_SPAWN) {
            spawnCount = data.spawnCount;
        } else {
            spawnCount = 1 + (int)(Math.random() * data.spawnCount);
        }

        for (int i = 0; i < spawnCount; i++) {
            Location spawnLoc = findValidSpawnLocation(spawnerLoc, data.spawnRange);
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
        }
    }

    private Location findValidSpawnLocation(Location spawnerLoc, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return null;

        Random random = new Random();

        for (int attempts = 0; attempts < 10; attempts++) {
            double x = spawnerLoc.getX() + (random.nextDouble() * range * 2 - range);
            double z = spawnerLoc.getZ() + (random.nextDouble() * range * 2 - range);
            double y = spawnerLoc.getY() + (random.nextDouble() * 3 - 1);

            Location testLoc = new Location(world, x, y, z);

            if (isValidSpawnLocation(testLoc)) {
                return testLoc;
            }
        }

        return spawnerLoc.clone().add(0.5, 1, 0.5);
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

    @EventHandler
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

        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == Material.SPAWNER) {
                    CreatureSpawner spawner = (CreatureSpawner) block.getState();
                    Map<String, Integer> config = readSpawnerConfig(item);
                    SpawnMode spawnMode = readSpawnModeConfig(item);

                    EntityType baseType;
                    if (mobType.startsWith("vanilla_")) {
                        String vanillaType = mobType.substring(8);
                        try {
                            baseType = EntityType.valueOf(vanillaType.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            baseType = EntityType.PIG;
                        }
                    } else {
                        baseType = getBaseEntityType(mobType);
                    }
                    spawner.setSpawnedType(baseType != null ? baseType : EntityType.PIG);

                    saveSpawnerData(spawner, mobType, config, spawnMode);

                    String customName = getCustomMobName(mobType);
                    if (customName != null) {
                        applyCustomNameToSpawner(spawner, customName);
                    }

                    if (spawnMode != SpawnMode.VANILLA) {
                        activeCustomSpawners.put(block.getLocation(), new CustomSpawnerData(mobType, config, spawnMode));
                    }

                    player.sendMessage(ChatColor.GREEN + "¡Spawner de " + mobType + " (" + spawnMode.getDisplayName() + ") colocado correctamente!");

                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    public void loadAllCustomSpawners() {
        int totalSpawners = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof CreatureSpawner) {
                        CreatureSpawner spawner = (CreatureSpawner) blockState;
                        if (isCustomSpawner(spawner)) {
                            registerCustomSpawner(spawner);
                            totalSpawners++;
                        }
                    }
                }
            }
        }
    }

    private void registerCustomSpawner(CreatureSpawner spawner) {
        Location loc = spawner.getLocation();
        if (activeCustomSpawners.containsKey(loc)) {
            return;
        }

        PersistentDataContainer container = spawner.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        String spawnModeStr = container.get(spawnModeKey, PersistentDataType.STRING);

        if (mobType != null && spawnModeStr != null) {
            try {
                SpawnMode spawnMode = SpawnMode.valueOf(spawnModeStr);
                if (spawnMode != SpawnMode.VANILLA) {
                    Map<String, Integer> config = loadSpawnerData(spawner);
                    activeCustomSpawners.put(loc, new CustomSpawnerData(mobType, config, spawnMode));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Modo de spawn inválido: " + spawnModeStr);
            }
        }
    }

    private Map<String, Integer> readSpawnerConfig(ItemStack spawnerItem) {
        Map<String, Integer> config = new HashMap<>();
        config.put("spawn_count", 4);
        config.put("max_nearby", 6);
        config.put("player_range", 20);
        config.put("delay", 40);
        config.put("min_delay", 200);
        config.put("max_delay", 600);
        config.put("spawn_range", 4);

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

    private void updateSpawnModeConfig(ItemStack spawner, SpawnMode spawnMode) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        String propertyDisplay = "Spawn Mode";
        String newLine = ChatColor.GRAY + propertyDisplay + ": " + ChatColor.WHITE + spawnMode.getDisplayName();

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
                if (ChatColor.stripColor(lore.get(i)).contains("Spawn Custom:")) {
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

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = (CreatureSpawner) event.getSpawner().getBlock().getState();
        if (!isCustomSpawner(spawner)) return;

        PersistentDataContainer container = spawner.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        String spawnModeStr = container.get(spawnModeKey, PersistentDataType.STRING);

        if (spawnModeStr != null) {
            try {
                SpawnMode spawnMode = SpawnMode.valueOf(spawnModeStr);

                if (spawnMode != SpawnMode.VANILLA) {
                    event.setCancelled(true);

                    if (spawnMode == SpawnMode.ONE_SPAWN) {
                        Location loc = spawner.getLocation();
                        CustomSpawnerData data = activeCustomSpawners.get(loc);
                        if (data != null && data.hasSpawnedOnce) {
                            return;
                        }
                    }
                    return;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Modo de spawn inválido: " + spawnModeStr);
            }
        }

        event.setCancelled(true);
        Location location = event.getLocation();

        if (mobType.startsWith("vanilla_")) {
            spawnVanillaMob(mobType, location);
        } else {
            spawnCustomMob(mobType, location);
        }

        location.getWorld().spawnParticle(Particle.POOF, location, 10, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_SCULK_BREAK, 3.0f, 0.1f);
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
        switch (mobType.toLowerCase()) {
            case "bombita": bombitaSpawner.spawnBombita(location); break;
            case "iceologer": iceologerSpawner.spawnIceologer(location); break;
            case "corruptedzombie": corruptedZombieSpawner.spawnCorruptedZombie(location); break;
            case "corruptedspider": corruptedSpider.spawnCorruptedSpider(location); break;
            case "queenbee": QueenBeeHandler.spawn(plugin, location); break;
            case "infestedbee": infestedBeeHandler.spawnInfestedBee(location); break;
            case "guardianblaze": guardianBlaze.spawnGuardianBlaze(location); break;
            case "guardiancorruptedskeleton": guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location); break;
            case "corruptedinfernalspider": corruptedInfernalSpider.spawnCorruptedInfernalSpider(location); break;
            case "corruptedbee": corruptedBee.spawnCorruptedBee(location); break;
            default: plugin.getLogger().warning("Tipo de mob desconocido en spawner: " + mobType); break;
        }
    }

    private EntityType getBaseEntityType(String mobType) {
        switch (mobType.toLowerCase()) {
            case "bombita": case "corruptedcreeper": case "infernalcreeper": case "endercreeper": case "darkcreeper": return EntityType.CREEPER;
            case "iceologer": return EntityType.ILLUSIONER;
            case "corruptedzombie": return EntityType.ZOMBIE;
            case "corruptedspider": case "corruptedinfernalspider": return EntityType.SPIDER;
            case "toxicspider": return EntityType.CAVE_SPIDER;
            case "queenbee": case "hellishbee": case "infestedbee": case "corruptedbee": return EntityType.BEE;
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
        switch (mobType.toLowerCase()) {
            case "bombita": return "Bombita";
            case "iceologer": return "Iceologer";
            case "corruptedzombie": return "Corrupted Zombie";
            case "corruptedspider": return "Corrupted Spider";
            case "queenbee": return "Abeja Reina";
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
            case "corruptedbee": return "Corrupted Bee";
            default: return null;
        }
    }

    private void applyCustomNameToSpawner(CreatureSpawner spawner, String customName) {
        Location loc = spawner.getLocation();
        String command = String.format(
                "data merge block %d %d %d {SpawnData:{entity:{CustomName:'[{\"text\":\"%s\"}]'}}}",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), customName
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }.runTaskLater(plugin, 1L);
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

        if (event.getClickedInventory() == null) return;

        int slot = event.getSlot();
        if (slot < 0 || slot > 21) return;

        if (slot == 7) {
            handleSpawnModeChange(player);
            return;
        }

        if (slot >= 18 && slot <= 21) return;

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

    private void saveSpawnerData(CreatureSpawner spawner, String mobType, Map<String, Integer> config, SpawnMode spawnMode) {
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        container.set(spawnerKey, PersistentDataType.STRING, mobType);
        container.set(spawnModeKey, PersistentDataType.STRING, spawnMode.name());

        for (Map.Entry<String, Integer> entry : config.entrySet()) {
            container.set(new NamespacedKey(plugin, entry.getKey()), PersistentDataType.INTEGER, entry.getValue());
        }

        spawner.update();
    }

    private Map<String, Integer> loadSpawnerData(CreatureSpawner spawner) {
        Map<String, Integer> config = new HashMap<>();
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        String[] keys = {"spawn_count", "max_nearby", "player_range", "delay",
                "min_delay", "max_delay", "spawn_range"};

        for (String key : keys) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
                config.put(key, container.get(namespacedKey, PersistentDataType.INTEGER));
            }
        }

        return config;
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
        activeCustomSpawners.clear();
    }
}