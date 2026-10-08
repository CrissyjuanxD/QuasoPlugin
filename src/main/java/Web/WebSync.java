package Web;

import Commands.Homes;
import Dificultades.Change;
import Events.MissionSystem.Mission;
import Events.MissionSystem.MissionHandler;
import Events.MissionSystem.TipoMision;
import Habilidades.HabilidadesManager;
import Handlers.ChangesHandler;
import Handlers.DatabaseManager;
import Managers.ItemManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

// Conecta el servidor con la web. Sube a un repositorio de GitHub (web.repositorio en config.yml) tres archivos:
//   estado.json     misiones activas, cambios activos, día y quién está conectado (se revisa cada pocos segundos)
//   jugadores.json  las estadísticas de cada jugador (cada pocos minutos)
//   catalogo.json   items, misiones, crafteos, trabajos, habilidades y mobs (al prender el servidor y con /web subir)
// Solo sube lo que cambió y avisa por ntfy.sh para que las páginas abiertas se actualicen al instante
public final class WebSync implements CommandExecutor, TabCompleter {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final long ESPERA_MINIMA_MS = 8000;

    private final JavaPlugin plugin;
    private final Logger log;
    private final Supplier<ItemManager> items;
    private final Supplier<MissionHandler> misiones;
    private final Supplier<ChangesHandler> cambios;
    private final Supplier<HabilidadesManager> habilidades;
    private final Supplier<Homes> homes;
    private final Supplier<DatabaseManager> db;

    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "QuasoPlugin-Web");
        t.setDaemon(true);
        return t;
    });
    // Archivos que faltan subir (ruta -> contenido) y el hash de lo último que se armó de cada uno
    private final Map<String, String> pendientes = new ConcurrentHashMap<>();
    private final Map<String, String> hashes = new ConcurrentHashMap<>();
    private volatile Map<String, Object> estado = Map.of();
    private volatile boolean subidaEnCola;

    private boolean activo;
    private String repo = "";
    private String rama = "main";
    private String ntfy = "";
    private int segundosEstado = 15;
    private int minutosJugadores = 2;
    private GitHubLive github;
    private JugadoresWeb jugadoresWeb;
    private BukkitTask tareaEstado;
    private BukkitTask tareaJugadores;
    private volatile boolean leyendoJugadores;

    private volatile String ultimoCommit = "";
    private volatile long ultimaSubida;
    private volatile String ultimoError = "";
    private volatile long ultimoErrorEn;

    public WebSync(JavaPlugin plugin, Supplier<ItemManager> items, Supplier<MissionHandler> misiones, Supplier<ChangesHandler> cambios,
                   Supplier<HabilidadesManager> habilidades, Supplier<Homes> homes, Supplier<DatabaseManager> db) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.items = items;
        this.misiones = misiones;
        this.cambios = cambios;
        this.habilidades = habilidades;
        this.homes = homes;
        this.db = db;
        agregarConfigPorDefecto();
        cargar();
    }

    // Pone la sección web: en config.yml si todavía no está (el config.yml del servidor ya existe y no se pisa)
    private void agregarConfigPorDefecto() {
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("web")) return;
        config.set("web.activado", false);
        config.set("web.repositorio", "CrissyjuanxD/Croissants-Live");
        config.set("web.rama", "main");
        config.set("web.token", "");
        config.set("web.ntfy", "croissants-quaso-7f3k9w2m");
        config.set("web.segundos-estado", 15);
        config.set("web.minutos-jugadores", 2);
        config.setComments("web", List.of(
                "Conexión con la web de Croissants: sube el estado del server, los jugadores y el catálogo a un repositorio",
                "de GitHub. El token es un fine-grained token con permiso Contents: Read and write SOLO en ese repositorio."));
        plugin.saveConfig();
    }

    private void cargar() {
        detener();
        File archivo = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        repo = config.getString("web.repositorio", "").trim();
        rama = config.getString("web.rama", "main").trim();
        String token = config.getString("web.token", "").trim();
        ntfy = config.getString("web.ntfy", "").trim();
        segundosEstado = Math.max(5, config.getInt("web.segundos-estado", 15));
        minutosJugadores = Math.max(1, config.getInt("web.minutos-jugadores", 2));
        activo = config.getBoolean("web.activado", false) && !token.isEmpty() && repo.matches("[\\w.-]+/[\\w.-]+");
        if (!activo) {
            if (config.getBoolean("web.activado", false)) {
                log.warning("[Web] La conexión con la web está activada pero falta el token o el repositorio en config.yml.");
            }
            return;
        }
        github = new GitHubLive(repo, rama.isEmpty() ? "main" : rama, token, ntfy);
        if (jugadoresWeb == null) {
            World mundo = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            jugadoresWeb = new JugadoresWeb(mundo == null ? null : new File(mundo.getWorldFolder(), "stats"));
        }
        tareaEstado = Bukkit.getScheduler().runTaskTimer(plugin, this::revisarEstado, 20L * 10, 20L * segundosEstado);
        tareaJugadores = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::leerJugadores, 20L * 40, 20L * 60 * minutosJugadores);
        Bukkit.getScheduler().runTaskLater(plugin, this::armarCatalogo, 20L * 20);
        log.info("[Web] Conectado con la web: los datos se suben a " + repo + ".");
    }

    private void detener() {
        if (tareaEstado != null) tareaEstado.cancel();
        if (tareaJugadores != null) tareaJugadores.cancel();
        tareaEstado = null;
        tareaJugadores = null;
    }

    public void shutdown() {
        detener();
        hilo.shutdown();
        try {
            hilo.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        hilo.shutdownNow();
    }

    // ---------------------------------------------------------------- estado (hilo principal, cada pocos segundos)

    private void revisarEstado() {
        if (!activo) return;
        Map<String, Object> nuevo = armarEstado();
        if (!nuevo.equals(estado)) {
            estado = nuevo;
            pedirSubida();
        } else if (!pendientes.isEmpty()) {
            pedirSubida();
        }
    }

    private Map<String, Object> armarEstado() {
        Map<String, Object> e = new LinkedHashMap<>();
        Set<Integer> activas = new TreeSet<>();
        int dia = 0;
        MissionHandler handler = misiones.get();
        if (handler != null) {
            activas.addAll(handler.getActiveMissions());
            for (int n : activas) {
                Mission mission = handler.getMissions().get(n);
                if (mission != null && TipoMision.de(n, mission.getParentMission()) == TipoMision.NORMAL) dia = Math.max(dia, n);
            }
        }
        e.put("dia", dia);
        e.put("misiones", Map.of("activas", new ArrayList<>(activas)));
        Map<String, Object> etapas = new LinkedHashMap<>();
        ChangesHandler changes = cambios.get();
        if (changes != null) for (Change change : changes.getChanges()) etapas.put(change.id(), change.isApplied());
        e.put("cambios", etapas);
        List<String> conectados = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) conectados.add(player.getName());
        conectados.sort(String.CASE_INSENSITIVE_ORDER);
        Map<String, Object> servidor = new LinkedHashMap<>();
        servidor.put("online", conectados.size());
        servidor.put("max", Bukkit.getMaxPlayers());
        servidor.put("conectados", conectados);
        e.put("servidor", servidor);
        return e;
    }

    // ---------------------------------------------------------------- catálogo (hilo principal)

    private void armarCatalogo() {
        if (!activo) return;
        Map<String, Object> catalogo;
        try {
            catalogo = Catalogo.armar(items.get(), misiones.get(), cambios.get(), plugin.getPluginMeta().getVersion(), log);
        } catch (Throwable error) {
            log.warning("[Web] No se pudo armar el catálogo: " + error);
            return;
        }
        hilo.execute(() -> guardar("catalogo.json", catalogo));
    }

    // ---------------------------------------------------------------- jugadores (asíncrono -> principal -> asíncrono)

    private void leerJugadores() {
        if (!activo || leyendoJugadores) return;
        DatabaseManager base = db.get();
        if (base == null) return;
        leyendoJugadores = true;
        List<JugadoresWeb.Jugador> lista;
        try {
            lista = jugadoresWeb.leerBase(base);
        } catch (Exception error) {
            leyendoJugadores = false;
            registrarError("No se pudieron leer los jugadores de la base de datos: " + error.getMessage());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                jugadoresWeb.completarEnMemoria(lista, misiones.get(), habilidades.get(), homes.get());
            } catch (Throwable error) {
                leyendoJugadores = false;
                registrarError("No se pudieron leer los datos de los jugadores: " + error);
                return;
            }
            hilo.execute(() -> {
                try {
                    guardar("jugadores.json", jugadoresWeb.armar(lista));
                } finally {
                    leyendoJugadores = false;
                }
            });
        });
    }

    // ---------------------------------------------------------------- subida (hilo de la web)

    // Deja el archivo listo para subir si cambió desde la última vez
    private void guardar(String ruta, Map<String, Object> datos) {
        String sinFecha = GSON.toJson(datos);
        String hash = hash(sinFecha);
        if (hash.equals(hashes.get(ruta))) return;
        Map<String, Object> conFecha = new LinkedHashMap<>(datos);
        conFecha.put("generado", Instant.now().toString());
        hashes.put(ruta, hash);
        pendientes.put(ruta, GSON.toJson(conFecha));
        pedirSubida();
    }

    private void pedirSubida() {
        if (!activo || subidaEnCola) return;
        subidaEnCola = true;
        hilo.execute(this::subir);
    }

    private void subir() {
        subidaEnCola = false;
        // Si la última subida falló se espera un minuto antes de reintentar (token vencido, GitHub caído...)
        long minimo = ultimoError.isEmpty() ? ESPERA_MINIMA_MS : 60_000;
        if (!ultimoError.isEmpty() && System.currentTimeMillis() - ultimaSubida < minimo) return;
        long espera = minimo - (System.currentTimeMillis() - ultimaSubida);
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        Map<String, String> archivos = new LinkedHashMap<>(pendientes);
        Map<String, Object> e = new LinkedHashMap<>(estado);
        Map<String, Object> archivosEstado = new LinkedHashMap<>();
        for (String ruta : List.of("catalogo.json", "jugadores.json")) {
            String h = hashes.get(ruta);
            if (h != null) archivosEstado.put(ruta.replace(".json", ""), h);
        }
        e.put("archivos", archivosEstado);
        e.put("version", 1);
        e.put("generado", Instant.now().toString());
        archivos.put("estado.json", GSON.toJson(e));
        try {
            String commit = github.subir(archivos, "Datos del servidor " + Instant.now());
            for (String ruta : archivos.keySet()) {
                if (archivos.get(ruta).equals(pendientes.get(ruta))) pendientes.remove(ruta);
            }
            ultimoCommit = commit;
            ultimaSubida = System.currentTimeMillis();
            if (!ultimoError.isEmpty()) {
                log.info("[Web] La conexión con GitHub volvió a funcionar.");
                ultimoError = "";
            }
        } catch (Exception error) {
            ultimaSubida = System.currentTimeMillis();
            registrarError("No se pudo subir a GitHub: " + error.getMessage());
        }
    }

    private void registrarError(String mensaje) {
        // El mismo error se avisa una vez cada 10 minutos para no llenar la consola
        if (!mensaje.equals(ultimoError) || System.currentTimeMillis() - ultimoErrorEn > 600_000) {
            log.warning("[Web] " + mensaje);
            ultimoErrorEn = System.currentTimeMillis();
        }
        ultimoError = mensaje;
    }

    private static String hash(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (Exception error) {
            return Integer.toHexString(texto.hashCode());
        }
    }

    // ---------------------------------------------------------------- /web

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String accion = args.length == 0 ? "estado" : args[0].toLowerCase();
        String morado = "#C9A7EB";
        String celeste = "#9FD3FF";
        switch (accion) {
            case "recargar" -> {
                cargar();
                sender.sendMessage(ChatColor.of(morado) + "Web: " + ChatColor.WHITE + (activo
                        ? "configuración recargada, subiendo a " + repo + "."
                        : "la conexión está apagada (web.activado: false o falta el token)."));
                if (activo) {
                    hashes.clear();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, this::leerJugadores);
                }
            }
            case "subir" -> {
                if (!activo) {
                    sender.sendMessage(ChatColor.RED + "La conexión con la web está apagada. Configura web: en config.yml y usa /web recargar.");
                    return true;
                }
                hashes.clear();
                estado = Map.of();
                armarCatalogo();
                revisarEstado();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::leerJugadores);
                sender.sendMessage(ChatColor.of(morado) + "Web: " + ChatColor.WHITE + "subiendo el catálogo, el estado y los jugadores...");
            }
            case "catalogo" -> {
                if (!activo) {
                    sender.sendMessage(ChatColor.RED + "La conexión con la web está apagada.");
                    return true;
                }
                hashes.remove("catalogo.json");
                armarCatalogo();
                sender.sendMessage(ChatColor.of(morado) + "Web: " + ChatColor.WHITE + "subiendo el catálogo (items, misiones, crafteos, trabajos, habilidades y mobs)...");
            }
            default -> {
                sender.sendMessage(ChatColor.of(morado) + "" + ChatColor.BOLD + "Conexión con la web");
                sender.sendMessage(ChatColor.of(celeste) + "Estado: " + ChatColor.WHITE + (activo ? "activa → " + repo + " (" + rama + ")" : "apagada"));
                if (activo) {
                    long hace = ultimaSubida == 0 ? -1 : (System.currentTimeMillis() - ultimaSubida) / 1000;
                    sender.sendMessage(ChatColor.of(celeste) + "Última subida: " + ChatColor.WHITE
                            + (ultimoCommit.isEmpty() ? "todavía ninguna" : ultimoCommit.substring(0, 7) + " hace " + hace + " s"));
                    sender.sendMessage(ChatColor.of(celeste) + "Pendientes: " + ChatColor.WHITE + (pendientes.isEmpty() ? "nada" : String.join(", ", pendientes.keySet())));
                    if (!ultimoError.isEmpty()) sender.sendMessage(ChatColor.of("#FF7A9A") + "Último error: " + ChatColor.WHITE + ultimoError);
                }
                sender.sendMessage(ChatColor.GRAY + "/web subir · /web catalogo · /web recargar");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        for (String opcion : List.of("estado", "subir", "catalogo", "recargar")) {
            if (opcion.startsWith(args[0].toLowerCase())) out.add(opcion);
        }
        return out;
    }
}
