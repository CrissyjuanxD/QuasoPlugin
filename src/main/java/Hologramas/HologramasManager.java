package Hologramas;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// Guarda los hologramas en hologramas.yml y mantiene su Text Display en el mundo. Las entidades no se guardan con el
// mundo (no persistentes): se crean al cargar el chunk, así nunca quedan duplicadas. Un holograma sin publicar es un
// borrador: solo lo ven los que tienen el permiso de hologramas
public final class HologramasManager implements Listener {

    public static final String PERMISO = "quaso.hologramas";

    private final JavaPlugin plugin;
    private final File archivo;
    private final NamespacedKey llave;
    private final Map<String, Holograma> hologramas = new TreeMap<>();
    private final Map<String, UUID> entidades = new HashMap<>();

    public HologramasManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.archivo = new File(plugin.getDataFolder(), "hologramas.yml");
        this.llave = new NamespacedKey(plugin, "holograma");
        cargar();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void cargar() {
        quitarTodas();
        hologramas.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection seccion = config.getConfigurationSection("hologramas");
        if (seccion != null) {
            for (String id : seccion.getKeys(false)) {
                Holograma holograma = new Holograma(id);
                holograma.cargar(seccion.getConfigurationSection(id));
                hologramas.put(id, holograma);
            }
        }
        for (Holograma holograma : hologramas.values()) mostrar(holograma);
    }

    void guardar() {
        YamlConfiguration config = new YamlConfiguration();
        for (Holograma holograma : hologramas.values()) holograma.guardar(config.createSection("hologramas." + holograma.id));
        try {
            config.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar hologramas.yml: " + e.getMessage());
        }
    }

    public List<Holograma> todos() {
        return new ArrayList<>(hologramas.values());
    }

    public Holograma get(String id) {
        return id == null ? null : hologramas.get(id.toLowerCase());
    }

    static boolean idValido(String id) {
        return id != null && id.matches("[a-z0-9_-]{1,32}");
    }

    // Crea un borrador en ese lugar
    Holograma crear(String id, Location location, String texto) {
        Holograma holograma = new Holograma(id);
        holograma.mover(location);
        holograma.yaw = location.getYaw();
        holograma.pitch = location.getPitch();
        if (texto != null && !texto.isBlank()) holograma.texto = texto;
        hologramas.put(id, holograma);
        guardar();
        mostrar(holograma);
        return holograma;
    }

    Holograma duplicar(Holograma original, String id, Location location) {
        Holograma copia = original.copia(id);
        copia.mover(location);
        copia.yaw = location.getYaw();
        copia.pitch = location.getPitch();
        hologramas.put(id, copia);
        guardar();
        mostrar(copia);
        return copia;
    }

    void eliminar(Holograma holograma) {
        quitar(holograma.id);
        hologramas.remove(holograma.id);
        guardar();
    }

    // Guarda el cambio y lo aplica al Text Display (lo mueve si cambió de lugar)
    void actualizar(Holograma holograma) {
        guardar();
        TextDisplay display = entidad(holograma);
        Location location = holograma.ubicacion();
        if (display == null || location == null || !display.getWorld().equals(location.getWorld())) {
            mostrar(holograma);
            return;
        }
        if (!location.isChunkLoaded()) {
            quitar(holograma.id);
            return;
        }
        display.teleport(location);
        holograma.aplicar(display);
        visibilidad(holograma, display);
    }

    TextDisplay entidad(Holograma holograma) {
        UUID uuid = entidades.get(holograma.id);
        Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
        return entity instanceof TextDisplay display && display.isValid() ? display : null;
    }

    // Crea el Text Display si su chunk está cargado
    private void mostrar(Holograma holograma) {
        quitar(holograma.id);
        Location location = holograma.ubicacion();
        if (location == null || !location.isChunkLoaded()) return;
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.getPersistentDataContainer().set(llave, PersistentDataType.STRING, holograma.id);
            d.setVisibleByDefault(holograma.publicado);
            holograma.aplicar(d);
        });
        entidades.put(holograma.id, display.getUniqueId());
        visibilidad(holograma, display);
    }

    private void visibilidad(Holograma holograma, TextDisplay display) {
        display.setVisibleByDefault(holograma.publicado);
        if (holograma.publicado) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PERMISO)) player.showEntity(plugin, display);
        }
    }

    private void quitar(String id) {
        UUID uuid = entidades.remove(id);
        Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
        if (entity != null) entity.remove();
    }

    private void quitarTodas() {
        for (String id : new ArrayList<>(entidades.keySet())) quitar(id);
    }

    // Los borradores se le muestran a los admins que entran
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(PERMISO)) return;
        for (Holograma holograma : hologramas.values()) {
            TextDisplay display = holograma.publicado ? null : entidad(holograma);
            if (display != null) player.showEntity(plugin, display);
        }
    }

    // Al cargar un chunk vuelven sus hologramas (un tick después, cuando el chunk ya está listo)
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        List<Holograma> enChunk = new ArrayList<>();
        for (Holograma holograma : hologramas.values()) {
            if (world.getName().equals(holograma.mundo) && ((int) Math.floor(holograma.x)) >> 4 == chunk.getX()
                    && ((int) Math.floor(holograma.z)) >> 4 == chunk.getZ()) enChunk.add(holograma);
        }
        if (enChunk.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Holograma holograma : enChunk) {
                if (hologramas.get(holograma.id) == holograma && entidad(holograma) == null) mostrar(holograma);
            }
        });
    }

    public void shutdown() {
        quitarTodas();
    }
}
