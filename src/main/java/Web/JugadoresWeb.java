package Web;

import Commands.Homes;
import Events.MissionSystem.MissionHandler;
import Habilidades.HabilidadesManager;
import Habilidades.HabilidadesType;
import Handlers.DatabaseManager;
import Trabajos.TrabajosManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Las estadísticas de cada jugador para la página de Jugadores. Se arma en tres pasos:
// 1. (asíncrono) la base de datos: jugadores, DinoCoins, misiones completadas y trabajos
// 2. (hilo principal) lo que está en memoria: rango, homes, habilidades y los datos en vivo de los conectados
// 3. (asíncrono) las estadísticas de Minecraft de los desconectados, leídas de world/stats/<uuid>.json
final class JugadoresWeb {

    // Una fila de la base de datos y lo que se le va sumando
    static final class Jugador {
        UUID uuid;
        String nombre;
        String primeraVez = "";
        String rangoGuardado = "";
        int dinocoins;
        int dinofichas;
        Set<Integer> misiones = new TreeSet<>();
        String trabajoActivo;
        final Map<String, double[]> trabajos = new LinkedHashMap<>();
        // Paso 2
        boolean online;
        String rango = "";
        int homes;
        final Map<String, Integer> habilidades = new LinkedHashMap<>();
        long ultimaVez;
        Map<String, Number> stats;
    }

    private final File carpetaStats;
    private final Set<String> bloques;
    // Las estadísticas leídas de cada archivo, por fecha de modificación (solo se vuelve a leer si cambió)
    private final Map<UUID, Object[]> cacheStats = new ConcurrentHashMap<>();

    JugadoresWeb(File carpetaStats) {
        this.carpetaStats = carpetaStats;
        Set<String> set = new HashSet<>();
        for (Material material : Material.values()) {
            try {
                if (!material.isLegacy() && material.isBlock() && material.isItem()) set.add(material.getKey().getKey());
            } catch (Throwable ignored) {
                // Algún material sin bloque o sin item: se salta
            }
        }
        this.bloques = set;
    }

    // ---------------------------------------------------------------- paso 1: base de datos (asíncrono)

    List<Jugador> leerBase(DatabaseManager db) throws Exception {
        Map<UUID, Jugador> jugadores = new LinkedHashMap<>();
        db.consultar(conn -> {
            try (PreparedStatement st = conn.prepareStatement("SELECT uuid, name, first_join, team_name, dinocoins, dinofichas FROM players");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = uuid(rs.getString("uuid"));
                    if (uuid == null) continue;
                    Jugador j = new Jugador();
                    j.uuid = uuid;
                    j.nombre = rs.getString("name");
                    Timestamp primera = rs.getTimestamp("first_join");
                    if (primera != null) j.primeraVez = primera.toInstant().toString();
                    j.rangoGuardado = valor(rs.getString("team_name"));
                    j.dinocoins = rs.getInt("dinocoins");
                    j.dinofichas = rs.getInt("dinofichas");
                    jugadores.put(uuid, j);
                }
            }
            try (PreparedStatement st = conn.prepareStatement("SELECT uuid, mission_id FROM player_missions WHERE is_completed = 1");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Jugador j = jugadores.get(uuid(rs.getString("uuid")));
                    if (j != null) j.misiones.add(rs.getInt("mission_id"));
                }
            }
            try (PreparedStatement st = conn.prepareStatement("SELECT uuid, job, level, xp FROM player_jobs");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Jugador j = jugadores.get(uuid(rs.getString("uuid")));
                    if (j != null) j.trabajos.put(rs.getString("job"), new double[]{rs.getInt("level"), rs.getDouble("xp")});
                }
            }
            try (PreparedStatement st = conn.prepareStatement("SELECT uuid, active_job FROM player_job_state");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Jugador j = jugadores.get(uuid(rs.getString("uuid")));
                    if (j != null) j.trabajoActivo = rs.getString("active_job");
                }
            }
            return null;
        });
        return new ArrayList<>(jugadores.values());
    }

    // ---------------------------------------------------------------- paso 2: memoria del servidor (hilo principal)

    void completarEnMemoria(List<Jugador> jugadores, MissionHandler misiones, HabilidadesManager habilidades, Homes homes) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Map<UUID, Jugador> porUuid = new HashMap<>();
        for (Jugador j : jugadores) porUuid.put(j.uuid, j);

        // Los conectados que todavía no están en la base de datos también salen
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (porUuid.containsKey(player.getUniqueId())) continue;
            Jugador j = new Jugador();
            j.uuid = player.getUniqueId();
            j.nombre = player.getName();
            jugadores.add(j);
            porUuid.put(j.uuid, j);
        }

        for (Jugador j : jugadores) {
            Player player = Bukkit.getPlayer(j.uuid);
            if (player != null) j.nombre = player.getName();
            if (j.nombre == null || j.nombre.isBlank()) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(j.uuid);
                j.nombre = offline.getName() == null ? j.uuid.toString().substring(0, 8) : offline.getName();
            }
            Team team = scoreboard.getEntryTeam(j.nombre);
            j.rango = team != null ? team.getName() : j.rangoGuardado;
            if (homes != null) j.homes = homes.contarHomes(j.nombre);
            if (habilidades != null) {
                for (HabilidadesType tipo : HabilidadesType.values()) {
                    j.habilidades.put(tipo.name().toLowerCase(Locale.ROOT), habilidades.getHighestLevel(j.uuid, tipo));
                }
            }
            if (player != null) {
                j.online = true;
                j.ultimaVez = System.currentTimeMillis();
                if (misiones != null) {
                    Set<Integer> enMemoria = misiones.completadasEnMemoria(j.uuid);
                    if (enMemoria != null) j.misiones = new TreeSet<>(enMemoria);
                }
                DatabaseManager.JobsData trabajos = TrabajosManager.datosWeb(j.uuid);
                if (trabajos != null) {
                    j.trabajoActivo = trabajos.activeJob();
                    j.trabajos.clear();
                    trabajos.progress().forEach((id, p) -> j.trabajos.put(id, new double[]{p.level(), p.xp()}));
                }
                j.stats = statsEnVivo(player);
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(j.uuid);
                j.ultimaVez = offline.getLastSeen();
            }
        }
    }

    // Las estadísticas que cambian mientras juega (las de bloques rotos y colocados salen del archivo)
    private static Map<String, Number> statsEnVivo(Player player) {
        Map<String, Number> s = new LinkedHashMap<>();
        s.put("ticks", player.getStatistic(Statistic.PLAY_ONE_MINUTE));
        s.put("muertes", player.getStatistic(Statistic.DEATHS));
        s.put("mobs", player.getStatistic(Statistic.MOB_KILLS));
        s.put("jugadores", player.getStatistic(Statistic.PLAYER_KILLS));
        s.put("pesca", player.getStatistic(Statistic.FISH_CAUGHT));
        s.put("aldeanos", player.getStatistic(Statistic.TRADED_WITH_VILLAGER));
        s.put("saltos", player.getStatistic(Statistic.JUMP));
        long cm = 0;
        for (Statistic statistic : Statistic.values()) {
            if (statistic.getType() == Statistic.Type.UNTYPED && statistic.name().endsWith("_ONE_CM")) {
                try {
                    cm += player.getStatistic(statistic);
                } catch (Throwable ignored) {
                    // Estadística que no existe en esta versión
                }
            }
        }
        s.put("cm", cm);
        return s;
    }

    // ---------------------------------------------------------------- paso 3: archivos de estadísticas (asíncrono)

    Map<String, Object> armar(List<Jugador> jugadores) {
        List<Object> lista = new ArrayList<>();
        for (Jugador j : jugadores) {
            Map<String, Number> archivo = statsArchivo(j.uuid);
            Map<String, Number> vivo = j.stats;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("nombre", j.nombre);
            p.put("uuid", j.uuid.toString());
            p.put("online", j.online);
            if (j.uuid.getMostSignificantBits() == 0) p.put("bedrock", true);
            if (!j.rango.isEmpty()) p.put("rango", j.rango);
            if (!j.primeraVez.isEmpty()) p.put("primeraVez", j.primeraVez);
            if (j.ultimaVez > 0) p.put("ultimaVez", Instant.ofEpochMilli(j.ultimaVez).toString());
            long ticks = numero(vivo, "ticks", numero(archivo, "ticks", 0));
            p.put("horas", Math.round(ticks / 72000.0 * 10) / 10.0);
            p.put("dinocoins", j.dinocoins);
            p.put("dinofichas", j.dinofichas);
            p.put("misiones", new ArrayList<>(j.misiones));
            Map<String, Object> trabajo = new LinkedHashMap<>();
            if (j.trabajoActivo != null && !j.trabajoActivo.isBlank()) trabajo.put("activo", j.trabajoActivo);
            Map<String, Object> niveles = new LinkedHashMap<>();
            j.trabajos.forEach((id, d) -> {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("nivel", (int) d[0]);
                t.put("xp", Math.round(d[1] * 10) / 10.0);
                niveles.put(id, t);
            });
            trabajo.put("niveles", niveles);
            p.put("trabajo", trabajo);
            p.put("habilidades", j.habilidades);
            p.put("homes", j.homes);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("muertes", numero(vivo, "muertes", numero(archivo, "muertes", 0)));
            stats.put("mobs", numero(vivo, "mobs", numero(archivo, "mobs", 0)));
            stats.put("jugadores", numero(vivo, "jugadores", numero(archivo, "jugadores", 0)));
            stats.put("bloquesRotos", numero(archivo, "bloquesRotos", 0));
            stats.put("bloquesColocados", numero(archivo, "bloquesColocados", 0));
            stats.put("distancia", Math.round(numero(vivo, "cm", numero(archivo, "cm", 0)) / 10000.0) / 10.0);
            stats.put("pesca", numero(vivo, "pesca", numero(archivo, "pesca", 0)));
            stats.put("aldeanos", numero(vivo, "aldeanos", numero(archivo, "aldeanos", 0)));
            stats.put("saltos", numero(vivo, "saltos", numero(archivo, "saltos", 0)));
            p.put("stats", stats);
            lista.add(p);
        }
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("version", 1);
        raiz.put("total", lista.size());
        raiz.put("jugadores", lista);
        return raiz;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Number> statsArchivo(UUID uuid) {
        if (carpetaStats == null) return Map.of();
        File archivo = new File(carpetaStats, uuid + ".json");
        if (!archivo.isFile()) return Map.of();
        long modificado = archivo.lastModified();
        Object[] guardado = cacheStats.get(uuid);
        if (guardado != null && (long) guardado[0] == modificado) return (Map<String, Number>) guardado[1];
        Map<String, Number> s = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            JsonObject raiz = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject stats = raiz.has("stats") ? raiz.getAsJsonObject("stats") : new JsonObject();
            JsonObject custom = objeto(stats, "minecraft:custom");
            s.put("ticks", entero(custom, "minecraft:play_time"));
            s.put("muertes", entero(custom, "minecraft:deaths"));
            s.put("mobs", entero(custom, "minecraft:mob_kills"));
            s.put("jugadores", entero(custom, "minecraft:player_kills"));
            s.put("pesca", entero(custom, "minecraft:fish_caught"));
            s.put("aldeanos", entero(custom, "minecraft:traded_with_villager"));
            s.put("saltos", entero(custom, "minecraft:jump"));
            long cm = 0;
            for (Map.Entry<String, JsonElement> entry : custom.entrySet()) {
                if (entry.getKey().endsWith("_one_cm")) cm += entry.getValue().getAsLong();
            }
            s.put("cm", cm);
            long rotos = 0;
            for (Map.Entry<String, JsonElement> entry : objeto(stats, "minecraft:mined").entrySet()) rotos += entry.getValue().getAsLong();
            s.put("bloquesRotos", rotos);
            long colocados = 0;
            for (Map.Entry<String, JsonElement> entry : objeto(stats, "minecraft:used").entrySet()) {
                String id = entry.getKey().startsWith("minecraft:") ? entry.getKey().substring(10) : entry.getKey();
                if (bloques.contains(id)) colocados += entry.getValue().getAsLong();
            }
            s.put("bloquesColocados", colocados);
        } catch (Exception error) {
            return Map.of();
        }
        cacheStats.put(uuid, new Object[]{modificado, s});
        return s;
    }

    private static JsonObject objeto(JsonObject padre, String clave) {
        JsonElement e = padre.get(clave);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static long entero(JsonObject padre, String clave) {
        JsonElement e = padre.get(clave);
        return e != null && e.isJsonPrimitive() ? e.getAsLong() : 0;
    }

    private static long numero(Map<String, Number> mapa, String clave, long porDefecto) {
        if (mapa == null) return porDefecto;
        Number n = mapa.get(clave);
        return n == null ? porDefecto : n.longValue();
    }

    private static UUID uuid(String texto) {
        try {
            return texto == null ? null : UUID.fromString(texto);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
