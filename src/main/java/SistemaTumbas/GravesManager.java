package SistemaTumbas;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class GravesManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Grave> activeGraves = new HashMap<>();
    private final Set<String> pendingGraves = new HashSet<>();

    private File configFile;
    private FileConfiguration config;
    private File dataFile;
    private GraveStorage storage;

    private ModoTumba modo;
    private int expiryMinutes;
    private int privateMinutes;
    private int openMinutes;
    private boolean teleport;
    // Tumbas mixtas que ya se abrieron para todos (para avisarle al dueño una sola vez)
    private final Set<UUID> announcedOpen = new HashSet<>();

    public GravesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startExpiryTask();
    }

    // Crea tumbas_config.yml si no existe (o le agrega lo nuevo a uno viejo) y lee el modo, los tiempos y si /muertes tepea
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        configFile = new File(plugin.getDataFolder(), "tumbas_config.yml");
        boolean nuevo = !configFile.exists();
        config = YamlConfiguration.loadConfiguration(configFile);
        if (nuevo || !config.contains("modo")) {
            String modoInicial = config.getBoolean("anyone-can-open", false) ? "abierta" : "mixta";
            try (PrintWriter writer = new PrintWriter(new FileWriter(configFile, !nuevo))) {
                if (nuevo) {
                    writer.println("# Minutos antes de que la tumba desaparezca y suelte las cosas al suelo (modos privada y abierta)");
                    writer.println("expiry-minutes: 30");
                }
                writer.println("");
                writer.println("# Quién puede abrir una tumba ajena: privada (solo el dueño), abierta (cualquiera) o mixta");
                writer.println("# (solo el dueño los primeros minutos y después cualquiera). El dueño y los admins siempre pueden");
                writer.println("modo: " + modoInicial);
                writer.println("# En la mixta: minutos que es solo del dueño y minutos que queda abierta antes de soltar las cosas");
                writer.println("minutos-privada: 20");
                writer.println("minutos-abierta: 10");
                writer.println("");
                writer.println("# /muertes te tepea a tu tumba (true) o solo te dice dónde está y hay que ir caminando (false)");
                writer.println("muertes-teleport: false");
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo escribir tumbas_config.yml: " + e.getMessage());
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        modo = ModoTumba.parse(config.getString("modo"), ModoTumba.MIXTA);
        expiryMinutes = Math.max(1, config.getInt("expiry-minutes", 30));
        privateMinutes = Math.max(0, config.getInt("minutos-privada", 20));
        openMinutes = Math.max(1, config.getInt("minutos-abierta", 10));
        teleport = config.getBoolean("muertes-teleport", false);
    }

    private long lifetimeMillis() {
        return modo.minutosDeVida(expiryMinutes, privateMinutes, openMinutes) * 60_000L;
    }

    // El dueño y los admins siempre; los demás según el modo
    public boolean canOpen(Grave grave, Player player) {
        if (grave.getOwner().equals(player.getUniqueId()) || player.hasPermission("tumbas.admin")) return true;
        return isOpenForAll(grave);
    }

    public boolean isOpenForAll(Grave grave) {
        return modo.abiertaParaTodos(grave.getCreationTime(), System.currentTimeMillis(), privateMinutes);
    }

    // Cuánto falta para que otros la puedan abrir (0 si ya pueden; -1 si nunca, en el modo privada)
    public long millisUntilOpen(Grave grave) {
        if (modo == ModoTumba.PRIVADA) return -1;
        if (modo == ModoTumba.ABIERTA) return 0;
        return Math.max(0, grave.getCreationTime() + privateMinutes * 60_000L - System.currentTimeMillis());
    }

    public ModoTumba getModo() { return modo; }
    public boolean teleportsToGrave() { return teleport; }

    // Los mundos de otros plugins y wardencave pueden cargarse después de este sistema.
    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "tumbas_data.yml");
        try {
            storage = new GraveStorage(dataFile);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("No se pudo leer tumbas_data.yml; el archivo se conserva sin sobrescribir.", error);
        }
        activeGraves.clear();
        for (String key : storage.records().keySet()) restoreGrave(key, true);
    }

    private void restoreGrave(String key, boolean warnMissingWorld) {
        try {
            UUID id = UUID.fromString(key);
            Object raw = storage.records().get(key);
            if (!(raw instanceof Map<?, ?> rawRecord) || !(rawRecord.get("location") instanceof Map<?, ?> coordinates)) {
                throw new IllegalArgumentException("Falta una ubicación válida.");
            }
            Object worldName = coordinates.get("world");
            if (!(worldName instanceof String name) || name.isBlank()) throw new IllegalArgumentException("Falta el nombre del mundo.");
            World world = coordinates.get("world-uuid") instanceof String uuid
                    ? Bukkit.getWorld(UUID.fromString(uuid)) : Bukkit.getWorld(name);
            if (world == null) {
                pendingGraves.add(key);
                if (warnMissingWorld) plugin.getLogger().warning("La tumba " + key + " espera al mundo " + name + "; sus objetos se conservan.");
                return;
            }
            Location loc = new Location(world, coordinate(coordinates, "x"), coordinate(coordinates, "y"), coordinate(coordinates, "z"),
                    (float) optionalCoordinate(coordinates, "yaw"), (float) optionalCoordinate(coordinates, "pitch"));
            YamlConfiguration record = storage.read(key);
            String ownerId = record.getString("owner");
            if (ownerId == null) throw new IllegalArgumentException("Falta el dueño de la tumba.");
            UUID owner = UUID.fromString(ownerId);
            String ownerName = record.getString("ownerName", owner.toString());
            if (rawRecord.get("items") != null && !(rawRecord.get("items") instanceof List<?>)) {
                throw new IllegalArgumentException("La lista de objetos no es válida.");
            }
            List<?> savedItems = rawRecord.get("items") instanceof List<?> list ? list : List.of();
            List<?> decodedItems = record.getList("items", List.of());
            if (savedItems.size() != decodedItems.size()) throw new IllegalArgumentException("No se pudo leer la lista completa de objetos.");
            List<ItemStack> items = new ArrayList<>();
            for (int index = 0; index < decodedItems.size(); index++) {
                Object item = decodedItems.get(index);
                if (item == null && savedItems.get(index) != null) throw new IllegalArgumentException("Hay un objeto que no se pudo leer.");
                if (item != null && !(item instanceof ItemStack)) throw new IllegalArgumentException("Hay un objeto que no se pudo leer.");
                items.add((ItemStack) item);
            }
            Grave grave = new Grave(id, owner, ownerName, loc, record.getLong("creationTime"), record.getLong("expiryTime"), items);
            activeGraves.put(id, grave);
            pendingGraves.remove(key);
            // WorldLoadEvent ocurre antes de que terminen de cargar las entidades guardadas.
            // Esperar un tick y cargarlas antes de limpiar evita duplicar los displays al reiniciar.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (activeGraves.get(id) != grave || Bukkit.getWorld(world.getUID()) != world) return;
                // El modelo y el cuadro van en el bloque de la tumba: alcanza con su chunk
                world.getChunkAt(loc).getEntities();
                cleanupVisuals(id, loc);
                spawnGraveVisuals(id, ownerName, loc, grave.getCreationTime());
                plugin.getLogger().info("Tumba " + key + " restaurada en " + world.getName() + ".");
            });
        } catch (IllegalArgumentException | InvalidConfigurationException error) {
            pendingGraves.remove(key);
            plugin.getLogger().warning("No se pudo restaurar la tumba " + key + ": " + error.getMessage() + " Se conserva su registro y sus objetos.");
        }
    }

    private static double coordinate(Map<?, ?> coordinates, String key) {
        Object value = coordinates.get(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) throw new IllegalArgumentException("Coordenada " + key + " inválida.");
        return number.doubleValue();
    }

    private static double optionalCoordinate(Map<?, ?> coordinates, String key) {
        return coordinates.containsKey(key) ? coordinate(coordinates, key) : 0;
    }

    @EventHandler public void onWorldLoad(WorldLoadEvent event) {
        for (String key : Set.copyOf(pendingGraves)) restoreGrave(key, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (Grave grave : List.copyOf(activeGraves.values())) {
            if (!event.getWorld().equals(grave.getLocation().getWorld())) continue;
            storage.put(grave);
            activeGraves.remove(grave.getId());
            pendingGraves.add(grave.getId().toString());
        }
        saveData();
    }

    public void saveData() {
        for (Grave grave : activeGraves.values()) storage.put(grave);
        try { storage.save(); }
        catch (IOException error) { plugin.getLogger().severe("No se pudieron guardar las tumbas: " + error.getMessage()); }
    }

    // La tumba va en el bloque de los pies del jugador. Si murió en el vacío del End va al último suelo que pisó, y en
    // otro vacío 5 bloques arriba del fondo del mundo
    public void createGrave(Player player, List<ItemStack> items) {
        UUID id = UUID.randomUUID();
        Location blockLoc = feet(player.getLocation());
        if (blockLoc.getY() < blockLoc.getWorld().getMinHeight()) {
            Location ground = Encantamientos.RetornoDelVacio.ultimoSuelo(player);
            if (ground != null && ground.getWorld() == blockLoc.getWorld()) blockLoc = feet(ground);
            else blockLoc.setY(blockLoc.getWorld().getMinHeight() + 5);
        }

        long creationTime = System.currentTimeMillis();
        long expiryTime = creationTime + lifetimeMillis();

        Grave grave = new Grave(id, player.getUniqueId(), player.getName(), blockLoc, creationTime, expiryTime, items);
        activeGraves.put(id, grave);
        saveData();

        spawnGraveVisuals(id, player.getName(), blockLoc, creationTime);
    }

    private static Location feet(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY(), loc.getBlockZ() + 0.5);
    }

    // Tumba vacía con cualquier nombre, solo de decoración
    public void createFakeGrave(String fakeName, Location loc) {
        UUID id = UUID.randomUUID();
        Location blockLoc = feet(loc);
        long creationTime = System.currentTimeMillis();
        long expiryTime = creationTime + lifetimeMillis();

        Grave grave = new Grave(id, UUID.randomUUID(), fakeName, blockLoc, creationTime, expiryTime, new ArrayList<>());
        activeGraves.put(id, grave);
        saveData();

        spawnGraveVisuals(id, fakeName, blockLoc, creationTime);
    }

    // La tumba son displays (cabezas, textos y la losa) montados en un block_display, más un interaction para poder clickearla
    private void spawnGraveVisuals(UUID graveId, String playerName, Location loc, long creationTime) {
        Date date = new Date(creationTime);
        String dayMonthYear = new SimpleDateFormat("dd/MM/yyyy").format(date);
        String hour = new SimpleDateFormat("HH:mm").format(date);

        String tag = "grave_" + graveId.toString();
        String timerTag = "timer_" + graveId.toString();

        // El cuadro para clickear ocupa el bloque de la tumba, justo donde está el modelo
        String interactionCmd = String.format(Locale.ROOT, "execute in %s positioned %d.5 %d %d.5 run summon interaction ~ ~ ~ {width:1.05f,height:1.15f,Tags:[\"%s\"]}",
                loc.getWorld().getKey().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), tag);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), interactionCmd);

        String rawNbt = "{Tags:[\"" + tag + "\"],Passengers:[" +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-706285507,552059348,1919193404,638722781],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTcxOTI2OCwKICAicHJvZmlsZUlkIiA6ICJjM2ZmNTY5OWZlNWI0OTY2YTYzYzdhMTEzNTBjZGIyNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWNobzkwMDAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWRkNTZkYjVhYWE3MDM3MDA1MzI2OTgzZjMzZGY3OTRlYzJkMzEzZGE1ZGNmNjQ3ZWRkNWVhYWRiMzI0NWZiOCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.3671875f,0f,1f,0f,0.703125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-44807437,1502350709,-1118570811,-56742203],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTcyNTU4NCwKICAicHJvZmlsZUlkIiA6ICJhYzY1NDYwOWVkZjM0ODhmOTM0ZWNhMDRmNjlkNGIwMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFjZUd1cmxTa3kiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVjN2FkZmNkZDkzMTEzMjk3ZGViZjE3YjVlNGYxMzZiYWE5MTU2MzQxNjcxODU2ODEwMDExYzllYmZlMjBmYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.5f,0f,0f,0.7421875f,0f,1f,0f,0.703125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1871777221,-2134856434,551084704,1439255443],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIyNDcyNiwKICAicHJvZmlsZUlkIiA6ICJmZDlhMmNhNGNhMWM0YTkwYWEyODkyNThkYmM3MmFiNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJJRnJhbmtvWCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xMDg2YTBkNDYzZGM3OTEzZDBjOTRiODM3MzkzZWJhZjcyOGM0YzM1NjNlYzQwYjMwYWJhYjU0MmMwZTFmMTA3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.4921875f,0f,0.25f,0f,0.953125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1495279794,-16549190,-867311589,-900151558],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIyOTc4NywKICAicHJvZmlsZUlkIiA6ICI3ZGEyYWIzYTkzY2E0OGVlODMwNDhhZmMzYjgwZTY4ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHb2xkYXBmZWwiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTQwZjcyNmZiN2MxNGMyNDQ3YWIzODQ5ZDU5MzVmYTRmNmU1NmYzZWIwNDE5MTBiNzc3NGU3NDdlNGM1YTk2OSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.5f,0f,0f,0.7421875f,0f,0.25f,0f,0.828125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-542678401,1263327314,1384696590,-1535808441],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIzNzAzNCwKICAicHJvZmlsZUlkIiA6ICJiZTQxM2Y4M2Y4ZWE0MjE0OGMwMjk0YTJiYzIyN2U2NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGaWdodGJveTEwMyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83YmJmZDQyOTBhZjFlYTViZGZmNzYxY2VkMDk5ZjdiYjRkYzZjMGJkMGRjYjk1ZmVlMjdkYTExM2RmMTEyZjljIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.3671875f,0f,0.25f,0f,0.828125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1680774573,1407103419,1238979372,-1645546705],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTI0MjY1NiwKICAicHJvZmlsZUlkIiA6ICI3YTVkYmRlNDk0NWU0YTE4Yjg2OWY1MGY1NTJjNjlkYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdWtraXRBUEkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JjYjk1NGJjYTRjNTAyMjE4NzRhZDk1ODZkNGE3NDg0ZjQ3YTUxNzk4YWQzYzhmZmZjOTg0NjQ3ZmZjNTBkNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.125f,0f,0f,0.2109375f,0f,0.125f,0f,0.890625f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1857821422,-1014503370,-956361272,1290224529],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTI0ODM2NiwKICAicHJvZmlsZUlkIiA6ICI1N2I3OWE2NTllZTM0ODhmODBhM2ExOGQwNDYxMTg3YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaW1hdGlrX1giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU4YjMwNTQzNzZhZDI5OWUwYTc5MmVlNmU2NjQxZTA1NjUwNzkyYWE2OTUwNWQyNTIwNDMwZWEwYjA3MDllMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.125f,0f,0f,0.7734375f,0f,0.125f,0f,0.890625f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + playerName + "\",color:\"#95abbb\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.45f,0f,0f,0.495f,0f,0.55f,0f,0.655f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + dayMonthYear + "\",color:\"#818e98\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.536875f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + hour + "\",color:\"#818e98\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.474375f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:smooth_stone_slab\",Count:1},item_display:\"none\",transformation:[1f,0f,0f,0.5f,0f,0.5f,0f,0.2175f,0f,0f,1f,0.5f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + timerTag + "\",\"" + tag + "\"],text:[{text:\"00:00:00\",color:\"#858585\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.3125f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}" +
                "]}";

        // El modelo está armado desde la esquina del bloque: con la raíz ahí queda centrado en el bloque y apoyado en el suelo
        String command = String.format(Locale.ROOT, "execute in %s positioned %d %d %d run summon block_display ~ ~ ~ %s",
                loc.getWorld().getKey().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), rawNbt);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public void removeGrave(UUID id) {
        announcedOpen.remove(id);
        Grave grave = activeGraves.remove(id);
        if (grave != null) {
            storage.records().remove(id.toString());
            cleanupVisuals(id, grave.getLocation());
            saveData();
        }
    }

    private void cleanupVisuals(UUID id, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        String tag = "grave_" + id.toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + loc.getWorld().getKey().toString() + " run kill @e[tag=" + tag + "]");
    }

    // Cada segundo actualiza el contador; cuando se acaba el tiempo suelta los items y borra la tumba
    private void startExpiryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();

                for (Grave grave : activeGraves.values()) {
                    if (grave.getLocation().getWorld() == null) continue;
                    long remaining = grave.getExpiryTime() - now;

                    if (remaining <= 0) {
                        World world = grave.getLocation().getWorld();
                        if (world != null) {
                            for (ItemStack item : grave.getItems()) {
                                world.dropItemNaturally(grave.getLocation(), item);
                            }
                        }
                        toRemove.add(grave.getId());
                    } else {
                        announceOpening(grave);
                        if (grave.getLocation().getWorld() != null && grave.getLocation().getWorld().isChunkLoaded(grave.getLocation().getBlockX() >> 4, grave.getLocation().getBlockZ() >> 4)) {
                            // Mientras es privada cuenta hasta que se abre; abierta (en ámbar) cuenta hasta que suelta las cosas
                            long untilOpen = millisUntilOpen(grave);
                            boolean privatePhase = modo == ModoTumba.MIXTA && untilOpen > 0;
                            String timeStr = ModoTumba.reloj(privatePhase ? untilOpen : remaining);
                            String color = privatePhase || modo == ModoTumba.PRIVADA ? "#858585" : "#E3B778";
                            String timerTag = "timer_" + grave.getId().toString();

                            for (Entity entity : grave.getLocation().getWorld().getNearbyEntities(grave.getLocation(), 2, 2, 2)) {
                                if (entity instanceof TextDisplay textDisplay && entity.getScoreboardTags().contains(timerTag)) {
                                    textDisplay.setText(ChatColor.of(color) + "" + ChatColor.BOLD + timeStr);
                                }
                            }
                        }
                    }
                }

                for (UUID id : toRemove) {
                    removeGrave(id);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // En la mixta le avisa al dueño (si está conectado) cuando su tumba ya la puede abrir cualquiera
    private void announceOpening(Grave grave) {
        if (modo != ModoTumba.MIXTA || announcedOpen.contains(grave.getId()) || !isOpenForAll(grave)) return;
        announcedOpen.add(grave.getId());
        Player owner = Bukkit.getPlayer(grave.getOwner());
        if (owner != null) {
            Location loc = grave.getLocation();
            owner.sendMessage(TumbaMessages.warn("Tu tumba en X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ()
                    + " ya la puede abrir cualquiera. Desaparece en " + ModoTumba.reloj(grave.getExpiryTime() - System.currentTimeMillis()) + "."));
        }
    }

    public Collection<Grave> getGraves() { return activeGraves.values(); }
    public Grave getGraveById(UUID id) { return activeGraves.get(id); }
}
