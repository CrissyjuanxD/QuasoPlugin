package Bosses;

import InfestedCaves.AncientCityLocator;
import imp.crissyjuanxd.QuasoPlugin;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Una guarida del Infested Warden por cada Ancient City de la Warden Cave. El boss aparece cuando hay alguien
// cerca, no despawnea, y cuando muere vuelve a salir a la hora (solo con la zona cargada). Nunca hay dos por ciudad, y
// las ciudades pegadas (las que se generaron antes de separarlas) comparten una sola guarida
public class InfestedWardenLairs implements Listener {

    private static final long RESPAWN_MS = 60 * 60 * 1000L;
    private static final double NEAR_PLAYER = 96;
    private static final int SEARCH_RADIUS = 28;
    // Dos ciudades a menos de esto son la misma guarida; ahora se generan a 512 bloques o más
    static final double MISMA_GUARIDA = 300;

    private static final class Lair {
        final int x;
        final int z;
        UUID boss;
        long respawnAt;
        int misses;

        Lair(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Lair> lairs = new LinkedHashMap<>();
    // Ciudad pegada a otra -> la guarida que comparten
    private final Map<String, String> alias = new HashMap<>();
    private final NamespacedKey bossIdKey;
    private final NamespacedKey lairKey;
    private List<Structure> ciudades;

    public InfestedWardenLairs(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "infested_warden.yml");
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
        this.lairKey = new NamespacedKey(plugin, "warden_lair");
        load();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
    }

    // Cada Ancient City que se carga queda anotada, la clave es el chunk del centro de la ciudad
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        for (Structure structure : ciudades()) {
            for (GeneratedStructure city : event.getChunk().getStructures(structure)) {
                BoundingBox box = city.getBoundingBox();
                int x = (int) Math.floor(box.getCenterX());
                int z = (int) Math.floor(box.getCenterZ());
                if (register(Math.floorDiv(x, 16) + "," + Math.floorDiv(z, 16), x, z)) save();
            }
        }
    }

    // La Ancient City de la Warden Cave (la del datapack) y la vanilla, que es la de las ciudades generadas antes
    private List<Structure> ciudades() {
        if (ciudades == null) {
            List<Structure> list = new ArrayList<>(List.of(Structure.ANCIENT_CITY));
            Structure propia = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE)
                    .get(new NamespacedKey("quaso", AncientCityLocator.STRUCTURE_KEY));
            if (propia != null) list.add(propia);
            ciudades = list;
        }
        return ciudades;
    }

    // Anota una ciudad nueva; si queda pegada a una que ya tiene guarida, comparte esa. Devuelve si cambió algo
    private boolean register(String key, int x, int z) {
        if (lairs.containsKey(key) || alias.containsKey(key)) return false;
        String cercana = cercana(lairs, x, z);
        if (cercana != null) alias.put(key, cercana);
        else lairs.put(key, new Lair(x, z));
        return true;
    }

    private static String cercana(Map<String, Lair> lairs, int x, int z) {
        for (Map.Entry<String, Lair> entry : lairs.entrySet()) {
            if (Math.hypot(entry.getValue().x - x, entry.getValue().z - z) < MISMA_GUARIDA) return entry.getKey();
        }
        return null;
    }

    private Lair lairOf(Warden warden) {
        String key = warden.getPersistentDataContainer().get(lairKey, PersistentDataType.STRING);
        return key == null ? null : lairs.get(alias.getOrDefault(key, key));
    }

    // Al cargarse un boss guardado le vuelve a crear su pelea; si su ciudad ya tiene otro, este sobra
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof Warden warden) || !isBoss(warden)) continue;
            if (warden.isDead() || InfestedWardenBoss.ACTIVE_BOSSES.containsKey(warden.getUniqueId())) continue;

            Lair lair = lairOf(warden);
            if (lair != null) {
                if (lair.boss != null && !lair.boss.equals(warden.getUniqueId())) {
                    warden.remove();
                    continue;
                }
                lair.boss = warden.getUniqueId();
                lair.respawnAt = 0;
                lair.misses = 0;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (warden.isValid() && !InfestedWardenBoss.ACTIVE_BOSSES.containsKey(warden.getUniqueId())) {
                    new InfestedWardenBoss(plugin, warden);
                }
            });
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Warden warden) || !isBoss(warden)) return;
        Lair lair = lairOf(warden);
        if (lair == null || !warden.getUniqueId().equals(lair.boss)) return;
        lair.boss = null;
        lair.respawnAt = System.currentTimeMillis() + RESPAWN_MS;
        save();
    }

    private boolean isBoss(Warden warden) {
        return InfestedWardenBoss.BOSS_ID.equals(warden.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING));
    }

    // Cada 10 segundos: si el boss de una ciudad desapareció sin morir (por ejemplo se metió bajo tierra) vuelve a
    // salir enseguida, y si ya pasó la hora desde que murió sale cuando haya alguien cerca
    private void tick() {
        World world = Bukkit.getWorld(QuasoPlugin.WORLD_NAME);
        if (world == null) return;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Lair> entry : lairs.entrySet()) {
            Lair lair = entry.getValue();
            if (!world.isChunkLoaded(lair.x >> 4, lair.z >> 4)) continue;

            if (lair.boss != null) {
                Entity boss = Bukkit.getEntity(lair.boss);
                if (boss != null && boss.isValid()) {
                    lair.misses = 0;
                } else if (areaLoaded(world, lair) && ++lair.misses >= 3) {
                    plugin.getLogger().info("El Infested Warden de la ciudad " + entry.getKey() + " desapareció, sale otro.");
                    lair.boss = null;
                    lair.respawnAt = 0;
                    save();
                }
                continue;
            }

            if (now >= lair.respawnAt && playerNear(world, lair) && !bossNear(lair)) spawn(world, entry.getKey(), lair);
        }
    }

    private boolean spawn(World world, String key, Lair lair) {
        Location spot = findSpot(world, lair);
        if (spot == null) return false;
        InfestedWardenBoss boss = InfestedWardenBoss.spawn(plugin, spot, key);
        lair.boss = boss.entity.getUniqueId();
        lair.respawnAt = 0;
        lair.misses = 0;
        save();
        return true;
    }

    private boolean areaLoaded(World world, Lair lair) {
        int cx = lair.x >> 4;
        int cz = lair.z >> 4;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (!world.isChunkLoaded(cx + dx, cz + dz)) return false;
            }
        }
        return true;
    }

    // Otro Infested Warden vivo en la zona (uno de /spawnqp o el de otra guarida que se alejó): no sale un segundo
    private boolean bossNear(Lair lair) {
        for (UUID id : InfestedWardenBoss.ACTIVE_BOSSES.keySet()) {
            Entity boss = Bukkit.getEntity(id);
            if (boss == null || !boss.isValid() || !boss.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) continue;
            if (Math.hypot(boss.getX() - lair.x, boss.getZ() - lair.z) < MISMA_GUARIDA) return true;
        }
        return false;
    }

    private boolean playerNear(World world, Lair lair) {
        Location center = new Location(world, lair.x, -40, lair.z);
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.getLocation().distanceSquared(center) <= NEAR_PLAYER * NEAR_PLAYER) return true;
        }
        return false;
    }

    // El boss sale arriba del portal: 9 bloques arriba de la base del marco y 10 hacia atrás, encima del cofre que
    // hay ahí. Como el centro de la ciudad tiene variantes, si no hay cofre usa el lugar libre más cercano a ese punto
    // (probando los dos lados del marco); si la ciudad no tiene portal, el piso libre más cercano al centro
    private Location findSpot(World world, Lair lair) {
        int[] portal = findPortal(world, lair);
        if (portal != null) {
            boolean planeX = portal[3] == 1;
            Location best = null;
            for (int side : new int[]{-1, 1}) {
                int tx = portal[0] + (planeX ? side * 10 : 0);
                int tz = portal[2] + (planeX ? 0 : side * 10);
                int ty = portal[1] + 9;
                Location chest = chestSpot(world, tx, ty, tz);
                if (chest != null) return chest;
                if (best == null) best = nearestClear(world, tx, ty, tz, 8, 6);
            }
            if (best != null) return best;
        }
        Location spot = nearestClear(world, lair.x, AncientCityLocator.MIN_Y + 2, lair.z, 48, 3);
        return spot != null ? spot : nearestClear(world, lair.x, AncientCityLocator.MIN_Y + 12, lair.z, SEARCH_RADIUS, 10);
    }

    // Centro de la base del marco de deepslate reforzado y en qué plano está (1 = plano X, el marco mira hacia X)
    private int[] findPortal(World world, Lair lair) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        for (int x = lair.x - 48; x <= lair.x + 48; x++) {
            for (int z = lair.z - 48; z <= lair.z + 48; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int y = AncientCityLocator.MIN_Y + 1; y <= AncientCityLocator.MAX_Y; y++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.REINFORCED_DEEPSLATE) continue;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                    minY = Math.min(minY, y);
                }
            }
        }
        if (minY == Integer.MAX_VALUE) return null;
        return new int[]{(minX + maxX) / 2, minY, (minZ + maxZ) / 2, maxX - minX <= maxZ - minZ ? 1 : 0};
    }

    private Location chestSpot(World world, int tx, int ty, int tz) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    Block block = world.getBlockAt(tx + dx, ty + dy, tz + dz);
                    if (block.getType() != Material.CHEST) continue;
                    Block above = block.getRelative(0, 1, 0);
                    if (fits(above, false)) return above.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }

    // El lugar libre más cercano al punto, buscando en anillos y a varias alturas
    private Location nearestClear(World world, int tx, int ty, int tz, int radius, int height) {
        for (int r = 0; r <= radius; r++) {
            int points = r == 0 ? 1 : r * 6;
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                int x = tx + (int) Math.round(Math.cos(a) * r);
                int z = tz + (int) Math.round(Math.sin(a) * r);
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int d = 0; d <= height; d++) {
                    for (int y : new int[]{ty - d, ty + d}) {
                        Block feet = world.getBlockAt(x, y, z);
                        if (fits(feet, true)) return feet.getLocation().add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return null;
    }

    // El Warden (escala 1.2) mide 3.5 de alto y algo más de un bloque de ancho: necesita 5 bloques libres arriba y las
    // 8 columnas de alrededor libres, así no aparece asfixiándose
    private boolean fits(Block feet, boolean needSolidFloor) {
        if (needSolidFloor && !feet.getRelative(0, -1, 0).getType().isSolid()) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int height = dx == 0 && dz == 0 ? 5 : 4;
                for (int h = 0; h < height; h++) {
                    Block b = feet.getRelative(dx, h, dz);
                    if (!b.isPassable() || b.isLiquid()) return false;
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- Comando

    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Lair> entry : lairs.entrySet()) {
            Lair lair = entry.getValue();
            String state;
            if (lair.boss != null) state = "vivo";
            else if (lair.respawnAt > now) state = "muerto, vuelve en " + ((lair.respawnAt - now) / 60000 + 1) + " min";
            else state = "esperando a que llegue alguien";
            lines.add("Ciudad " + lair.x + ", " + lair.z + ": " + state);
        }
        return lines;
    }

    // Saca el boss de la ciudad más cercana al jugador sin esperar el respawn
    public String forceSpawn(Player player) {
        Lair best = null;
        String bestKey = null;
        double bestDist = Double.MAX_VALUE;
        for (Map.Entry<String, Lair> entry : lairs.entrySet()) {
            Lair lair = entry.getValue();
            double d = Math.hypot(player.getLocation().getX() - lair.x, player.getLocation().getZ() - lair.z);
            if (d < bestDist) {
                bestDist = d;
                best = lair;
                bestKey = entry.getKey();
            }
        }
        if (best == null || bestDist > 200) return "No hay ninguna Ancient City cargada a menos de 200 bloques.";
        if (best.boss != null && Bukkit.getEntity(best.boss) != null) return "El boss de esa ciudad ya está vivo.";
        best.boss = null;
        if (!spawn(player.getWorld(), bestKey, best)) return "No encontré lugar para el boss en esa ciudad.";
        return "Infested Warden spawneado en la ciudad " + best.x + ", " + best.z + ".";
    }

    // ---------------------------------------------------------------- Archivo

    // Las guaridas que quedaron pegadas a otra (de antes de separar las ciudades) pasan a ser parte de esa; su boss,
    // si sigue vivo, lo adopta la guarida que queda o se borra si ya tiene uno (onEntitiesLoad)
    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("ciudades");
        if (section != null) {
            for (String path : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(path);
                if (s == null) continue;
                String key = path.replace('_', ',');
                Lair lair = new Lair(s.getInt("x"), s.getInt("z"));
                String cercana = cercana(lairs, lair.x, lair.z);
                if (cercana != null) {
                    alias.put(key, cercana);
                    continue;
                }
                String boss = s.getString("boss");
                lair.boss = boss != null ? UUID.fromString(boss) : null;
                lair.respawnAt = s.getLong("respawn");
                lairs.put(key, lair);
            }
        }
        ConfigurationSection aliases = yml.getConfigurationSection("pegadas");
        if (aliases != null) {
            for (String path : aliases.getKeys(false)) {
                String main = aliases.getString(path);
                if (main != null) alias.putIfAbsent(path.replace('_', ','), main.replace('_', ','));
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Lair> entry : lairs.entrySet()) {
            String path = "ciudades." + entry.getKey().replace(',', '_');
            Lair lair = entry.getValue();
            yml.set(path + ".x", lair.x);
            yml.set(path + ".z", lair.z);
            yml.set(path + ".boss", lair.boss != null ? lair.boss.toString() : null);
            yml.set(path + ".respawn", lair.respawnAt);
        }
        for (Map.Entry<String, String> entry : alias.entrySet()) {
            yml.set("pegadas." + entry.getKey().replace(',', '_'), entry.getValue().replace(',', '_'));
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar infested_warden.yml: " + e.getMessage());
        }
    }
}
