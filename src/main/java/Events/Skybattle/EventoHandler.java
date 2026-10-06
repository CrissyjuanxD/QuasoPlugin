package Events.Skybattle;

import Habilidades.HabilidadesEffects;
import Habilidades.HabilidadesManager;
import Handlers.EventInventoryManager;
import Handlers.Teams.TeamType;
import TitleListener.EventoAnimation;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EventoHandler implements Listener {

    private final HabilidadesManager habilidadesManager;
    private final HabilidadesEffects habilidadesEffects;
    private final EventoAnimation eventoAnimation;
    private EventInventoryManager eventInventoryManager;

    private final Set<String> participantes = new HashSet<>();
    private final Set<String> excludedPlayers = new HashSet<>();
    private final Map<String, Integer> kills = new HashMap<>();
    private final List<String> ordenEliminados = new ArrayList<>();

    private List<Location> ubicacionesShroomlightOriginales = new ArrayList<>();
    private final Map<String, String> originalTeams = new HashMap<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();

    private boolean eventoActivo = false;
    private boolean secuenciaBatallaIniciada = false;
    private final int MAX_PARTICIPANTES = 20;
    private final JavaPlugin plugin;
    private final CofresHandler cofresHandler;
    private Scoreboard eventoScoreboard;

    private File configFile;
    private FileConfiguration config;
    private String modoIngreso = "block";
    private String timerStart = "00:04:00";
    private int tiempoBordeInicial = 120;
    private Location zonaEspectadores;
    private int topRecompensas = 3;

    private int arenaMinX, arenaMaxX, arenaMinY, arenaMaxY, arenaMinZ, arenaMaxZ;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private int tiempoBorde = 120;

    private boolean bordeReduciendose = false;
    private boolean eventoEnCurso = false;
    private boolean preparacion = false;
    private final File estadoArchivo;

    private BukkitRunnable taskBordeParticulas;
    private BukkitRunnable taskReduccionBorde;
    private BukkitRunnable taskReduccionBordeContinuo;

    private BukkitTask pauseMonitorTask;
    private List<String> ultimosDesconectados = new ArrayList<>();

    private Material bloqueActual;
    private String nombreBloqueActual;

    private final List<String> cancionesDisponibles = new ArrayList<>();
    private final Map<String, Integer> duracionCanciones = new HashMap<>();
    private int indexCancion = 0;
    private BukkitTask musicTask;
    private float currentMusicPitch = 1.0f;

    public EventoHandler(JavaPlugin plugin, HabilidadesManager habilidadesManager, HabilidadesEffects habilidadesEffects) {
        this.plugin = plugin;
        this.habilidadesManager = habilidadesManager;
        this.habilidadesEffects = habilidadesEffects;
        this.cofresHandler = new CofresHandler(plugin);
        this.eventoAnimation = new EventoAnimation(plugin);
        this.estadoArchivo = new File(plugin.getDataFolder(), "estado_evento.yml");

        duracionCanciones.put("minecraft:music_disc.lava_chicken", (2 * 60 + 14) * 20);
        duracionCanciones.put("minecraft:music_disc.pigstep", (2 * 60 + 28) * 20);
        duracionCanciones.put("minecraft:music_disc.creator", (2 * 60 + 56) * 20);
        duracionCanciones.put("minecraft:music_disc.tears", (2 * 60 + 55) * 20);
        duracionCanciones.put("minecraft:music_disc.precipice", (4 * 60 + 59) * 20);
        cancionesDisponibles.addAll(duracionCanciones.keySet());

        crearYcargarConfig();
        verificarEstadoEvento();
    }

    // Crea lavaclashconfig.yml con los valores por defecto y lee el modo de ingreso, tiempos y la arena
    public void crearYcargarConfig() {
        configFile = new File(plugin.getDataFolder(), "lavaclashconfig.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.options().setHeader(List.of("=== Configuración de LavaClash ==="));

        if (!config.contains("modo_ingreso")) {
            config.set("modo_ingreso", "block");
            config.set("timer_start", "00:04:00");
            config.set("tiempo_borde_segundos", 120);
            config.set("top_recompensas", 3);

            config.set("zona.minX", 19904);
            config.set("zona.maxX", 20096);
            config.set("zona.minY", 27);
            config.set("zona.maxY", 110);
            config.set("zona.minZ", 19904);
            config.set("zona.maxZ", 20096);

            config.set("espectadores.x", 20015.00);
            config.set("espectadores.y", 106.00);
            config.set("espectadores.z", 20000.27);
            config.set("espectadores.yaw", -1979.64);
            config.set("espectadores.pitch", 0.46);

            try { config.save(configFile); } catch (IOException ignored) {}
        }

        modoIngreso = config.getString("modo_ingreso", "block");
        timerStart = config.getString("timer_start", "00:04:00");
        tiempoBordeInicial = config.getInt("tiempo_borde_segundos", 120);
        topRecompensas = config.getInt("top_recompensas", 3);

        arenaMinX = config.getInt("zona.minX", 19904);
        arenaMaxX = config.getInt("zona.maxX", 20096);
        arenaMinY = config.getInt("zona.minY", 27);
        arenaMaxY = config.getInt("zona.maxY", 110);
        arenaMinZ = config.getInt("zona.minZ", 19904);
        arenaMaxZ = config.getInt("zona.maxZ", 20096);

        minX = arenaMinX;
        maxX = arenaMaxX;
        minY = arenaMinY;
        maxY = arenaMaxY;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;
        tiempoBorde = tiempoBordeInicial;

        double espX = config.getDouble("espectadores.x", 20015.00);
        double espY = config.getDouble("espectadores.y", 106.00);
        double espZ = config.getDouble("espectadores.z", 20000.27);
        float espYaw = (float) config.getDouble("espectadores.yaw", -1979.64);
        float espPitch = (float) config.getDouble("espectadores.pitch", 0.46);
        zonaEspectadores = new Location(Bukkit.getWorld("world"), espX, espY, espZ, espYaw, espPitch);
    }

    private int parseTimeToSeconds(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length == 3) {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        }
        return 240;
    }

    private boolean isInsideArenaGeneral(Location loc) {
        return loc.getBlockX() >= arenaMinX && loc.getBlockX() <= arenaMaxX
                && loc.getBlockY() >= arenaMinY && loc.getBlockY() <= arenaMaxY
                && loc.getBlockZ() >= arenaMinZ && loc.getBlockZ() <= arenaMaxZ;
    }

    private List<Player> getJugadoresEnZona() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> isInsideArenaGeneral(p.getLocation()))
                .collect(Collectors.toList());
    }

    private void enviarMensajeZona(String mensaje) {
        getJugadoresEnZona().forEach(p -> p.sendMessage(mensaje));
    }

    public void setEventInventoryManager(EventInventoryManager manager) {
        this.eventInventoryManager = manager;
    }

    public boolean isEventoActivo() {
        return eventoActivo;
    }

    public boolean isSecuenciaBatallaIniciada() {
        return secuenciaBatallaIniciada;
    }

    public boolean isParticipante(String name) {
        return participantes.contains(name);
    }

    // Modo random: elige hasta 20 jugadores al azar. Modo block: los primeros 20 en romper el bloque elegido consiguen ticket
    public void iniciarEvento(boolean silencioso) {
        if (eventoActivo) return;
        eventoActivo = true;
        secuenciaBatallaIniciada = false;

        crearYcargarConfig();
        participantes.clear();
        ordenEliminados.clear();
        kills.clear();

        inicializarTeamLavaClashMain();

        if (modoIngreso.equalsIgnoreCase("random")) {
            List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !excludedPlayers.contains(p.getName()))
                    .collect(Collectors.toList());

            Collections.shuffle(onlinePlayers);
            int amount = Math.min(MAX_PARTICIPANTES, onlinePlayers.size());

            for (int i = 0; i < amount; i++) {
                Player p = onlinePlayers.get(i);
                participantes.add(p.getName());
                aplicarTeamLavaClash(p);
            }

            if (!silencioso) {
                String jsonMessage = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#c55cf3\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\n\",\"color\":\"#c55cf3\"},{\"text\":\"" + amount + " jugadores aleatorios han sido seleccionados.\\nSerán teletransportados en breve.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n \"}]";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    eventoAnimation.playAnimation(p, jsonMessage);
                }
            }

            World world = Bukkit.getWorld("world");
            if (world != null) world.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, false);
            guardarEstadoEvento();

            if (participantes.size() == MAX_PARTICIPANTES) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (eventoActivo && !preparacion) {
                            teletransportarJugadores();
                        }
                    }
                }.runTaskLater(plugin, 840L);
            }

        } else {
            List<Material> bloquesPosibles = List.of(
                    Material.POPPY, Material.STRIPPED_OAK_LOG, Material.GLASS,
                    Material.COAL_ORE
            );

            Map<Material, String> nombresBloques = Map.of(
                    Material.POPPY, "Flor Roja (Amapola)",
                    Material.STRIPPED_OAK_LOG, "Tronco de Roble sin corteza",
                    Material.GLASS, "Cristal",
                    Material.COAL_ORE, "Mineral de Carbon"
            );

            bloqueActual = bloquesPosibles.get(new Random().nextInt(bloquesPosibles.size()));
            nombreBloqueActual = nombresBloques.get(bloqueActual);

            if (!silencioso) {
                String jsonMessage = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Ha comenzado el evento \",\"color\":\"#c55cf3\"},{\"text\":\"LAVACLASH\",\"bold\":true,\"color\":\"#D98836\"},{\"text\":\"!\\nLos primeros \",\"color\":\"#c55cf3\"},{\"text\":\"20\",\"bold\":true,\"color\":\"#c55cf3\"},{\"text\":\" jugadores en obtener\\nun \",\"color\":\"#c55cf3\"},{\"text\":\"Quaso Ticket\",\"bold\":true,\"color\":\"#E9BF66\"},{\"text\":\" participarán\\n\\nPara obtener el ticket deberan romper un \",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"},{\"text\":\"" + nombreBloqueActual + "\",\"bold\":true,\"color\":\"#57A9CB\"},{\"text\":\"\\n \"}]";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    eventoAnimation.playAnimation(p, jsonMessage);
                }
            }

            World world = Bukkit.getWorld("world");
            if (world != null) world.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, false);
            guardarEstadoEvento();
        }
    }

    private void inicializarTeamLavaClashMain() {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (t == null) {
            t = mainBoard.registerNewTeam(TeamType.LAVACLASH.getId());
        }
        t.setPrefix(TeamType.LAVACLASH.getChatPrefix());
        t.setColor(TeamType.LAVACLASH.getBukkitColor());
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private void aplicarTeamLavaClash(Player p) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = mainBoard.getEntryTeam(p.getName());
        if (oldTeam != null) {
            originalTeams.put(p.getName(), oldTeam.getName());
        } else {
            originalTeams.put(p.getName(), null);
        }

        Team lcTeam = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (lcTeam != null) lcTeam.addEntry(p.getName());
    }

    private void restaurarTeamOriginal(String playerName) {
        Player p = Bukkit.getPlayer(playerName);
        if (p == null) return;

        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team lcTeam = mainBoard.getTeam(TeamType.LAVACLASH.getId());
        if (lcTeam != null) lcTeam.removeEntry(playerName);

        if (eventoScoreboard != null) {
            Team evtLcTeam = eventoScoreboard.getTeam(TeamType.LAVACLASH.getId());
            if (evtLcTeam != null) evtLcTeam.removeEntry(playerName);
        }

        if (originalTeams.containsKey(playerName)) {
            String oldTeamName = originalTeams.get(playerName);
            if (oldTeamName != null) {
                Team old = mainBoard.getTeam(oldTeamName);
                if (old != null) old.addEntry(playerName);
            }
        }

        p.setScoreboard(mainBoard);
    }

    public void forzarEvento() {
        if (eventoActivo && participantes.size() < MAX_PARTICIPANTES) {
            teletransportarJugadores();
        }
    }

    // Devuelve inventarios y teams, limpia la arena y restaura los cofres y los shroomlights
    public void terminarEvento() {
        guardarDatosFinales();

        boolean estabaEnFaseAvanzada = preparacion || eventoEnCurso;

        eventoActivo = false;
        eventoEnCurso = false;
        preparacion = false;
        secuenciaBatallaIniciada = false;

        if (pauseMonitorTask != null && !pauseMonitorTask.isCancelled()) {
            pauseMonitorTask.cancel();
        }

        for (String nombre : originalTeams.keySet()) {
            restaurarTeamOriginal(nombre);
            Player p = Bukkit.getPlayer(nombre);
            if (p != null) {
                if (estabaEnFaseAvanzada && participantes.contains(nombre)) {
                    p.getInventory().clear();
                    if (eventInventoryManager != null) {
                        eventInventoryManager.restoreInventory(p);
                    }
                }
                habilidadesManager.enableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
            }
        }

        World world = Bukkit.getWorld("world");
        if (world != null) {
            org.bukkit.util.BoundingBox arenaBox = new org.bukkit.util.BoundingBox(arenaMinX, arenaMinY, arenaMinZ, arenaMaxX, arenaMaxY, arenaMaxZ);
            for (Entity entity : world.getNearbyEntities(arenaBox)) {
                if (entity instanceof Item) {
                    entity.remove();
                }
            }
            world.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, true);
        }

        participantes.clear();
        originalTeams.clear();
        ordenEliminados.clear();
        kills.clear();
        excludedPlayers.clear();

        for (Player p : getJugadoresEnZona()) {
            p.sendMessage(" ");
            p.stopSound(SoundCategory.RECORDS);
            p.sendMessage("§c۞ El evento ha terminado.");
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        if (eventoScoreboard != null) {
            Objective obj = eventoScoreboard.getObjective("skybattle");
            if (obj != null) obj.unregister();
            eventoScoreboard = null;
        }

        restaurarShroomlights();
        eliminarPurpleConcrete();
        restaurarContenidoCofres();
        guardarEstadoEvento();
        cargarCofresDesdeArchivo();
        restaurarContenidoCofres();
        cancelarTareasActivas();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
            if (archivo.exists()) {
                archivo.delete();
            }
        }, 140L);
    }

    public void spawnFinal() {
        List<Player> enZona = getJugadoresEnZona();
        if (enZona.isEmpty()) {
            Bukkit.broadcastMessage("§cNo hay jugadores en la zona para teletransportar al spawn.");
            return;
        }
        for (Player p : enZona) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magictp " + p.getName() + " spawn");
        }
    }

    private void reiniciarVariablesBorde() {
        minX = arenaMinX;
        maxX = arenaMaxX;
        minZ = arenaMinZ;
        maxZ = arenaMaxZ;
        tiempoBorde = tiempoBordeInicial;
        bordeReduciendose = false;
    }

    // En modo block romper el bloque del evento da el Quaso Ticket y mete al jugador
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventoActivo || preparacion || participantes.size() >= MAX_PARTICIPANTES) return;
        if (!modoIngreso.equalsIgnoreCase("block")) return;

        Player jugador = event.getPlayer();
        if (participantes.contains(jugador.getName())) return;

        if (excludedPlayers.contains(jugador.getName())) {
            if (event.getBlock().getType() == bloqueActual) {
                jugador.sendMessage(ChatColor.RED + "Estás excluido de este evento y no puedes participar.");
                event.setCancelled(true);
            }
            return;
        }

        if (event.getBlock().getType() == bloqueActual) {
            ItemStack ticket = new ItemStack(Material.PAPER);
            ItemMeta meta = ticket.getItemMeta();
            meta.setDisplayName("§e§lQuaso Ticket");
            meta.setCustomModelData(1);
            meta.setItemModel(NamespacedKey.minecraft("quaso_ticket"));
            ticket.setItemMeta(meta);
            jugador.getInventory().addItem(ticket);

            participantes.add(jugador.getName());
            aplicarTeamLavaClash(jugador);

            String jsonMessage = "[\"\","
                    + "{\"text\":\"\u06de\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + jugador.getName() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\" ha obtenido el \",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"Quaso ticket\",\"bold\":true,\"color\":\"#E9BF66\"},"
                    + "{\"text\":\" - Ticket:\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + participantes.size() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"/\",\"bold\":true,\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"" + MAX_PARTICIPANTES + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"\\n \"}"
                    + "]";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);

            if (participantes.size() == MAX_PARTICIPANTES) {
                Bukkit.broadcastMessage("§e۞ ¡Ya no hay más tickets disponibles! Los jugadores serán teletransportados en 10 segundos.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (eventoActivo && !preparacion) {
                            teletransportarJugadores();
                        }
                    }
                }.runTaskLater(plugin, 200L);
            }
        }
    }

    // Después del timer guarda los cofres, desactiva habilidades, guarda inventarios y tepea a cada uno a un shroomlight
    private void teletransportarJugadores() {
        List<Location> shroomlightLocations = obtenerShroomlightLocations();

        if (shroomlightLocations.size() < participantes.size()) {
            Bukkit.broadcastMessage("§c[Error] No hay suficientes ubicaciones disponibles para teletransportar a los jugadores.");
            return;
        }

        int totalSegundos = parseTimeToSeconds(timerStart);
        int m = totalSegundos / 60;
        int s = totalSegundos % 60;
        String tiempoTexto = m + (s > 0 ? " min y " + s + " seg" : " minutos");

        String tellrawCommand2 = "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Evento \",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\"\\u27a4\",\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"El evento empezará en\",\"color\":\"#C55CF3\"},{\"text\":\" " + tiempoTexto + ".\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\n\"},{\"text\":\"Se recomienda a los\",\"color\":\"#C55CF3\"},{\"text\":\" jugadores\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" que entraron en el evento\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\"},{\"text\":\"que\",\"color\":\"#C55CF3\"},{\"text\":\" guarden\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" sus cosas en\",\"color\":\"#C55CF3\"},{\"text\":\" cofres por seguridad.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\n\\n\"},{\"text\":\"IMPORTANTE\",\"bold\":true,\"color\":\"#F12C51\"},{\"text\":\":\",\"bold\":true,\"color\":\"gray\"},{\"text\":\" Guardar spawn en una cama antes del tp.\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\"\\n \"}]";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + tellrawCommand2);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:block.note_block.pling ambient @a ~ ~ ~ 1 1.3 1");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "timers crear @a tiempo=" + timerStart + " sonido=on");

        long delayTicks = (totalSegundos * 20L) + 50L;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            guardarContenidoCofres();
            eliminarMobsExistentes();
            this.preparacion = true;

            Collections.shuffle(shroomlightLocations);

            int i = 0;
            for (String jugador : participantes) {
                Player p = Bukkit.getPlayer(jugador);
                if (p != null && i < shroomlightLocations.size()) {

                    if (habilidadesManager != null) {
                        habilidadesManager.disableHabilidades(p);
                    }
                    if (habilidadesEffects != null && habilidadesManager != null) {
                        habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
                    }

                    if (eventInventoryManager != null) {
                        eventInventoryManager.saveAndClearInventory(p);
                    }

                    p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());

                    Location loc = shroomlightLocations.get(i).add(0.5, 1, 0.5);
                    String comando = String.format(Locale.US, "magictp %s %.2f %.2f %.2f", jugador, loc.getX(), loc.getY(), loc.getZ());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                    i++;
                }
            }
        }, delayTicks);
    }

    private List<Location> obtenerShroomlightLocations() {
        List<Location> locations = new ArrayList<>();
        for (int x = arenaMinX; x <= arenaMaxX; x++) {
            for (int y = arenaMinY; y <= arenaMaxY; y++) {
                for (int z = arenaMinZ; z <= arenaMaxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.SHROOMLIGHT) {
                        locations.add(loc);
                    }
                }
            }
        }
        return locations;
    }

    private List<String> obtenerJugadoresOffline() {
        List<String> offline = new ArrayList<>();
        for (String nombre : participantes) {
            Player p = Bukkit.getPlayer(nombre);
            if (p == null || !p.isOnline()) {
                offline.add(nombre);
            }
        }
        return offline;
    }

    // Si falta alguien conectado el evento queda en pausa hasta que vuelvan todos
    private void iniciarMonitorPausa() {
        pauseMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }

                List<String> offlinePlayers = obtenerJugadoresOffline();

                if (offlinePlayers.isEmpty()) {
                    enviarMensajeZona("§aTodos los jugadores están conectados.");
                    this.cancel();
                } else {
                    if (!offlinePlayers.equals(ultimosDesconectados)) {
                        String nombres = String.join(", ", offlinePlayers);

                        enviarMensajeZona("§fSe ha sufrido una desconexión de los jugadores:");
                        enviarMensajeZona("§b" + nombres);
                        enviarMensajeZona("§fEl evento ha sido pausado.");

                        ultimosDesconectados = new ArrayList<>(offlinePlayers);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void iniciarSecuenciaInicioSkyBattle() {
        if (!eventoActivo) return;

        List<String> offlinePlayers = obtenerJugadoresOffline();

        if (!offlinePlayers.isEmpty()) {
            if (pauseMonitorTask == null || pauseMonitorTask.isCancelled()) {
                ultimosDesconectados.clear();
                iniciarMonitorPausa();
            }
            return;
        }

        if (pauseMonitorTask != null) pauseMonitorTask.cancel();

        if (secuenciaBatallaIniciada) return;
        secuenciaBatallaIniciada = true;

        ejecutarSecuenciaBatalla();
    }


    // Cuenta regresiva, dificultad en hard al empezar y a los ~7 minutos la fase final con velocidad II
    private void ejecutarSecuenciaBatalla() {
        cancelarTareasActivas();
        getJugadoresEnZona().forEach(p -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stopsound " + p.getName()));

        BukkitTask feverTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!eventoActivo) return;

            currentMusicPitch = 1.4f;
            iniciarPlaylist(true);

            String colorEmoji = ChatColor.of("#FFB3BA").toString();
            String colorTexto = ChatColor.of("#AEC6CF").toString();
            String colorFuerte = ChatColor.of("#FDFD96").toString();

            String msgFever = colorEmoji + "⌚" + colorTexto + " Se ha activado la fase final, a todos se les ha aplicado " + colorFuerte + "velocidad II" + colorTexto + ".";

            for (Player p : getJugadoresEnZona()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false, false));
                p.sendMessage(msgFever);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 2.0f);
            }
        }, 8020L);
        tareasActivas.add(feverTask);

        new BukkitRunnable() {
            int contador = 10;

            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }

                if (contador > 0) {
                    String color = "#D172F6";
                    float pitch = 1.0f;

                    if (contador == 3) { color = "yellow"; pitch = 1.8f; }
                    else if (contador == 2) { color = "gold"; pitch = 1.9f; }
                    else if (contador == 1) {
                        color = "red";
                        pitch = 2.0f;
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty peaceful");
                    } else if (contador == 10) {
                        pitch = 1.1f;
                        currentMusicPitch = 1.0f;
                        iniciarPlaylist(true);
                    } else if (contador == 9) pitch = 1.2f;
                    else if (contador == 8) pitch = 1.3f;
                    else if (contador == 7) pitch = 1.4f;
                    else if (contador == 6) pitch = 1.5f;
                    else if (contador == 5) pitch = 1.6f;
                    else if (contador == 4) pitch = 1.7f;

                    for (Player p : getJugadoresEnZona()) {
                        p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.RECORDS, 1, pitch);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " times 0 40 0");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title [\"\",{\"text\":\"Start\",\"bold\":true,\"color\":\"#D172F6\"},{\"text\":\":\",\"bold\":true,\"color\":\"gray\"}]");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u25b6\",\"bold\":true},{\"text\":\"" + contador + "\",\"bold\":true,\"color\":\"" + color + "\"},{\"text\":\"\\u25c0\",\"bold\":true}]");
                    }
                    contador--;
                } else {
                    for (Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\"Fight!\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle {\"text\":\"\",\"bold\":true,\"color\":\"#7E5FE6\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " times 15 25 15");
                    }
                    this.cancel();
                    if (eventoActivo) {
                        iniciarSkyBattle();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty hard");
                    }
                }
            }
        }.runTaskTimer(plugin, 350, 20);
    }

    // Da los kits, quita los shroomlights de spawn y arranca el borde y el daño fuera de él
    public void iniciarSkyBattle() {
        this.eventoEnCurso = true;

        int colorIndex = 0;
        for (String jugador : participantes) {
            Player p = Bukkit.getPlayer(jugador);
            if (p != null) {
                darKitBatalla(p, colorIndex);
                colorIndex++;
            }
        }

        inicializarScoreboard();

        ubicacionesShroomlightOriginales = obtenerShroomlightLocations();
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.AIR);
        }

        iniciarReduccionBorde();
        aplicarDanioFueraDelBorde();
    }

    // Kit básico con armadura de cuero de un color distinto para cada jugador
    private void darKitBatalla(Player p, int colorIndex) {
        p.getInventory().clear();
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));

        Color[] COLORES_ARMADURA = {
                Color.fromRGB(255, 0, 0), Color.fromRGB(0, 0, 255), Color.fromRGB(0, 255, 0),
                Color.fromRGB(255, 255, 0), Color.fromRGB(255, 128, 0), Color.fromRGB(128, 0, 128),
                Color.fromRGB(255, 192, 203), Color.fromRGB(0, 255, 255), Color.fromRGB(0, 255, 128),
                Color.fromRGB(255, 0, 255), Color.fromRGB(0, 128, 128), Color.fromRGB(0, 0, 128),
                Color.fromRGB(128, 0, 0), Color.fromRGB(128, 128, 0), Color.fromRGB(255, 255, 255),
                Color.fromRGB(128, 128, 128), Color.fromRGB(0, 0, 0), Color.fromRGB(192, 192, 192),
                Color.fromRGB(255, 215, 0), Color.fromRGB(75, 0, 130)
        };

        Color colorArmadura = COLORES_ARMADURA[colorIndex % COLORES_ARMADURA.length];

        ItemStack pechera = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack pantalones = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack botas = new ItemStack(Material.LEATHER_BOOTS);

        ItemStack[] armadura = {pechera, pantalones, botas};
        for (ItemStack pieza : armadura) {
            org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) pieza.getItemMeta();
            if (meta != null) {
                meta.setColor(colorArmadura);
                meta.addEnchant(Enchantment.PROTECTION, 1, true);
                pieza.setItemMeta(meta);
            }
        }

        p.getInventory().setChestplate(pechera);
        p.getInventory().setLeggings(pantalones);
        p.getInventory().setBoots(botas);

        p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        p.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE));
        p.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 64));
    }

    private void iniciarPlaylist(boolean reset) {
        if (musicTask != null) {
            musicTask.cancel();
            musicTask = null;
        }
        if (reset) {
            indexCancion = new Random().nextInt(cancionesDisponibles.size());
        }
        playNextSong();
    }
    // La duración de cada canción se ajusta al pitch con el que suena
    private void playNextSong() {
        if (!eventoActivo) return;

        String cancion = cancionesDisponibles.get(indexCancion % cancionesDisponibles.size());
        int baseDurationTicks = duracionCanciones.getOrDefault(cancion, 3600);

        int actualDurationTicks = (int) (baseDurationTicks / currentMusicPitch);
        indexCancion++;

        for (Player p : getJugadoresEnZona()) {
            p.stopSound(SoundCategory.RECORDS);
            p.playSound(p.getLocation(), cancion, SoundCategory.RECORDS, Float.MAX_VALUE, currentMusicPitch);
        }

        musicTask = Bukkit.getScheduler().runTaskLater(plugin, this::playNextSong, actualDurationTicks);
        tareasActivas.add(musicTask);
    }

    private void inicializarScoreboard() {
        if (eventoScoreboard == null) {
            eventoScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (org.bukkit.scoreboard.Team mainTeam : mainBoard.getTeams()) {
            org.bukkit.scoreboard.Team t = eventoScoreboard.getTeam(mainTeam.getName());
            if (t == null) {
                t = eventoScoreboard.registerNewTeam(mainTeam.getName());
            }
            t.setPrefix(mainTeam.getPrefix());
            t.setSuffix(mainTeam.getSuffix());
            try { t.setColor(mainTeam.getColor()); } catch (Exception ignored) {}
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, mainTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));

            for (String entry : mainTeam.getEntries()) {
                if (!t.hasEntry(entry)) {
                    t.addEntry(entry);
                }
            }
        }

        for (Objective mainObj : mainBoard.getObjectives()) {
            if (mainObj.getDisplaySlot() == DisplaySlot.PLAYER_LIST || mainObj.getDisplaySlot() == DisplaySlot.BELOW_NAME) {
                Objective newObj = eventoScoreboard.getObjective(mainObj.getName());
                if (newObj == null) {
                    newObj = eventoScoreboard.registerNewObjective(mainObj.getName(), mainObj.getCriteria(), mainObj.getDisplayName());
                    newObj.setDisplaySlot(mainObj.getDisplaySlot());
                }

                if (!mainObj.getCriteria().equalsIgnoreCase("health")) {
                    for (String entry : mainBoard.getEntries()) {
                        Score score = mainObj.getScore(entry);
                        if (score.isScoreSet()) {
                            try {
                                newObj.getScore(entry).setScore(score.getScore());
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }

        TeamType lc = TeamType.LAVACLASH;
        org.bukkit.scoreboard.Team teamLC = eventoScoreboard.getTeam(lc.getId());
        if (teamLC == null) teamLC = eventoScoreboard.registerNewTeam(lc.getId());
        teamLC.setPrefix(lc.getChatPrefix());
        teamLC.setColor(lc.getBukkitColor());
        teamLC.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        for (String pName : participantes) {
            if (Bukkit.getPlayer(pName) != null) {
                teamLC.addEntry(pName);
            }
        }

        Objective objective = eventoScoreboard.getObjective("skybattle");
        if (objective == null) {
            objective = eventoScoreboard.registerNewObjective("skybattle", "dummy", "§c§lLAVA§6§lCLA§e§lSH");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        crearTeamScoreboard("sb_players", "  §5\uD83D\uDDE1§6§l Players Left: ", "§a§l" + participantes.size(), 3);
        crearTeamScoreboard("sb_borde", "  §e⚠§c§l Borde: ", "§7§l02:00", 1);

        objective.getScore("§r     ").setScore(5);
        objective.getScore("§r      ").setScore(4);
        objective.getScore("§r          ").setScore(2);
        objective.getScore("§r           ").setScore(0);

        for (Player player : getJugadoresEnZona()) {
            player.setScoreboard(eventoScoreboard);
        }
    }

    private void crearTeamScoreboard(String teamName, String prefix, String suffix, int score) {
        Objective obj = eventoScoreboard.getObjective("skybattle");
        org.bukkit.scoreboard.Team team = eventoScoreboard.getTeam(teamName);
        if (team == null) team = eventoScoreboard.registerNewTeam(teamName);

        String entry = ChatColor.values()[score].toString();
        team.addEntry(entry);
        team.setPrefix(prefix);
        team.setSuffix(suffix);
        obj.getScore(entry).setScore(score);
    }

    private void actualizarScoreboard() {
        if (eventoScoreboard == null) return;

        org.bukkit.scoreboard.Team tPlayers = eventoScoreboard.getTeam("sb_players");
        if (tPlayers != null) {
            tPlayers.setSuffix("§a§l" + participantes.size());
        }

        org.bukkit.scoreboard.Team tBorde = eventoScoreboard.getTeam("sb_borde");
        if (tBorde != null) {
            String tiempo = bordeReduciendose ? "§4§l> > >" : (tiempoBorde == -1 ? "§7§l00:00" : "§7§l" + obtenerTiempoBorde());
            tBorde.setSuffix(tiempo);
        }

        for (Player p : getJugadoresEnZona()) {
            if (p.getScoreboard() != eventoScoreboard) {
                p.setScoreboard(eventoScoreboard);
            }
        }
    }

    private String obtenerTiempoBorde() {
        if (tiempoBorde == -1) {
            return "00:00";
        }
        int minutos = tiempoBorde / 60;
        int segundos = tiempoBorde % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    private void mostrarBordeParticulas(int minX, int maxX, int minZ, int maxZ) {
        List<Player> espectadores = getJugadoresEnZona();
        if (espectadores.isEmpty()) return;

        Particle particula = Particle.SONIC_BOOM;
        int stepY = 6, stepXZ = 4;

        for (int y = arenaMinY; y <= arenaMaxY; y += stepY) {
            for (int z = minZ; z <= maxZ; z += stepXZ) {
                spawnParticleIfClose(espectadores, particula, minX, y, z);
                spawnParticleIfClose(espectadores, particula, maxX, y, z);
            }
            for (int x = minX; x <= maxX; x += stepXZ) {
                spawnParticleIfClose(espectadores, particula, x, y, minZ);
                spawnParticleIfClose(espectadores, particula, x, y, maxZ);
            }
        }
    }

    private void spawnParticleIfClose(List<Player> players, Particle p, int x, int y, int z) {
        if (x == 20000 && z == 20000) return;

        Location loc = new Location(Bukkit.getWorld("world"), x + 0.5, y + 0.5, z + 0.5);
        for (Player player : players) {
            if (player.getLocation().distanceSquared(loc) < 1600) {
                player.spawnParticle(p, loc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void mostrarBordeParticulasContinuo() {
        taskBordeParticulas = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    this.cancel();
                    return;
                }
                mostrarBordeParticulas(minX, maxX, minZ, maxZ);
            }
        };
        taskBordeParticulas.runTaskTimer(plugin, 0L, 20L);
    }

    // Cada cierto tiempo cierra el borde hacia el centro hasta dejarlo en 3x3
    private void iniciarReduccionBorde() {
        taskReduccionBorde = new BukkitRunnable() {
            @Override
            public void run() {
                if (participantes.size() <= 1 || !eventoActivo || !eventoEnCurso) {
                    this.cancel();
                    return;
                }

                if (tiempoBorde <= 0 && !bordeReduciendose) {
                    bordeReduciendose = true;

                    if (minX >= 19999 && maxX <= 20001 && minZ >= 19999 && maxZ <= 20001) {
                        minX = 19999;
                        maxX = 20001;
                        minZ = 19999;
                        maxZ = 20001;
                        tiempoBorde = -1;

                        enviarMensajeZona("§§§e§l۞§6§l El borde ha llegado a su límite.§r§§ ");
                        for(Player p : getJugadoresEnZona()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"yellow\"},{\"text\":\" El borde ya no se moverá.\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"yellow\"}]");
                            p.playSound(p.getLocation(), "block.note_block.bell", 1, 0.1f);
                        }

                        bordeReduciendose = false;
                        actualizarScoreboard();
                        this.cancel();
                        return;
                    }

                    new BukkitRunnable() {
                        int countdown = 3;

                        @Override
                        public void run() {
                            if (!eventoActivo || !eventoEnCurso) {
                                this.cancel();
                                return;
                            }

                            if (countdown == 0) {
                                enviarMensajeZona("§§§c§l۞§4§l El borde ha comenzado a reducirse!!§r§§ ");
                                for(Player p : getJugadoresEnZona()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"},{\"text\":\" Borde reduciéndose. \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"red\"}]");
                                    p.playSound(p.getLocation(), "block.note_block.bell", 1, 2f);
                                }

                                iniciarReduccionBordeContinuo();
                                this.cancel();
                            } else {
                                String color = switch (countdown) {
                                    case 3 -> "§e§l";
                                    case 2 -> "§6§l";
                                    case 1 -> "§c§l";
                                    default -> "§f§l";
                                };

                                enviarMensajeZona("§c۞§7§l El §4§lborde§7§l se reducirá en " + color + countdown);
                                getJugadoresEnZona().forEach(p -> p.playSound(p.getLocation(), "block.note_block.bell", 1, 1.5f));
                                countdown--;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                }

                actualizarScoreboard();
                tiempoBorde--;
            }
        };

        taskReduccionBorde.runTaskTimer(plugin, 0L, 15L);
        mostrarBordeParticulasContinuo();
    }

    private void iniciarReduccionBordeContinuo() {
        taskReduccionBordeContinuo = new BukkitRunnable() {
            int steps = 10;
            int stepSize = 2;

            @Override
            public void run() {
                if (steps <= 0) {
                    tiempoBorde = tiempoBordeInicial;
                    bordeReduciendose = false;

                    enviarMensajeZona("§§§6§l۞ §c§lEl borde se ha detenido. §r§l§§§7§lTiempo reiniciado a§6§l " + (tiempoBordeInicial / 60) + " minutos");
                    for(Player p : getJugadoresEnZona()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " title {\"text\":\" \",\"bold\":true,\"color\":\"red\"}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " subtitle [\"\",{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" Borde Detenido.\",\"bold\":true,\"color\":\"red\"},{\"text\":\" \\u26a0\",\"bold\":true,\"color\":\"gold\"}]");
                        p.playSound(p.getLocation(), "block.note_block.bell", 1, 0.1f);
                    }

                    actualizarScoreboard();
                    this.cancel();
                } else {
                    minX = Math.min(minX + stepSize, 19999);
                    maxX = Math.max(maxX - stepSize, 20001);
                    minZ = Math.min(minZ + stepSize, 19999);
                    maxZ = Math.max(maxZ - stepSize, 20001);

                    mostrarBordeParticulas(minX, maxX, minZ, maxZ);
                    steps--;
                }
            }
        };

        taskReduccionBordeContinuo.runTaskTimer(plugin, 0L, 20L);
    }

    // Tira sus items, le devuelve su inventario y le suma la kill al que lo mató
    private void procesarEliminacionJugador(Player jugador, Player atacante, String eliminado, String asesino) {
        jugador.setHealth(jugador.getAttribute(Attribute.MAX_HEALTH).getValue());

        jugador.getInventory().forEach(item -> {
            if (item != null) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), item);
            }
        });
        jugador.getInventory().clear();

        if (eventInventoryManager != null) {
            eventInventoryManager.restoreInventory(jugador);
        }

        habilidadesManager.enableHabilidades(jugador);
        habilidadesEffects.reapplyAllEffects(jugador, habilidadesManager);

        if (!ordenEliminados.contains(eliminado)) {
            ordenEliminados.add(eliminado);
        }

        if (asesino != null && !asesino.isEmpty()) {
            kills.put(asesino, kills.getOrDefault(asesino, 0) + 1);
        }

        Location deathLocation = jugador.getLocation();
        jugador.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLocation.add(0, 1, 0), 100, 1, 1, 1, 0.5);

        String mensaje = "§8§l[§c§l☠§8§l]§6§l " + jugador.getName() + " §r§7ha perdido el §7§lLavaClash";
        if (atacante != null) {
            mensaje += " §r§7por §6§l" + atacante.getName();

            atacante.sendTitle(
                    "",
                    "§5§l\ud83d\udde1 §c§l \u2620§r§6" + jugador.getName(),
                    15, 25, 15
            );

            atacante.playSound(atacante.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 2.0f);
            atacante.playSound(atacante.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 2.0f);
        }

        enviarMensajeZona(mensaje);
        getJugadoresEnZona().forEach(player -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 0.1f);
        });

        jugador.teleport(zonaEspectadores);

        participantes.remove(eliminado);
        actualizarScoreboard();

        if (participantes.size() == 1) {
            declararGanador();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!eventoActivo) return;

        Player jugador = event.getPlayer();
        String nombreJugador = jugador.getName();

        if (participantes.contains(nombreJugador)) {
            if (this.eventoEnCurso) {
                procesarEliminacionJugador(jugador, null, nombreJugador, "");
                enviarMensajeZona("§8§l[§c§l☠§8§l]§6§l " + nombreJugador + " §r§7ha abandonado el evento");
            }
        }
    }

    // En la zona de espectadores no hay daño; en la partida la muerte se maneja a mano para no perder el inventario
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!eventoActivo || !(event.getEntity() instanceof Player)) return;

        Player jugador = (Player) event.getEntity();

        double alturaSegura = zonaEspectadores.getY() - 1.0;

        if (isInsideArenaGeneral(jugador.getLocation()) && jugador.getLocation().getY() >= alturaSegura) {
            event.setCancelled(true);
        }

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
            if (evt.getDamager() instanceof Player) {
                Player attacker = (Player) evt.getDamager();
                if (isInsideArenaGeneral(attacker.getLocation()) && attacker.getLocation().getY() >= alturaSegura) {
                    event.setCancelled(true);
                }
            } else if (evt.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) evt.getDamager();
                if (proj.getShooter() instanceof Player) {
                    Player shooter = (Player) proj.getShooter();
                    if (isInsideArenaGeneral(shooter.getLocation()) && shooter.getLocation().getY() >= alturaSegura) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (event.isCancelled()) return;

        if (participantes.contains(jugador.getName())) {
            if (eventoEnCurso) {
                double nuevaVida = jugador.getHealth() - event.getFinalDamage();
                if (nuevaVida <= 0.3) {
                    event.setCancelled(true);
                    Player atacante = getAtacante(jugador);
                    procesarEliminacionJugador(jugador, atacante, jugador.getName(),
                            atacante != null ? atacante.getName() : "");
                }
            }
        }
    }

    // Saca el atacante del último daño (proyectiles, TNT y creepers incluidos)
    private Player getAtacante(Player jugador) {
        EntityDamageEvent lastDamage = jugador.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) lastDamage;
            Entity damager = damageByEntityEvent.getDamager();

            if (damager instanceof Player) {
                return (Player) damager;
            }
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player) {
                    return (Player) projectile.getShooter();
                }
            }
            if (damager instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) damager;
                if (tnt.getSource() instanceof Player) {
                    return (Player) tnt.getSource();
                }
            }
            if (damager instanceof Creeper) {
                Creeper creeper = (Creeper) damager;
                if (creeper.getTarget() instanceof Player) {
                    return (Player) creeper.getTarget();
                }
            }
        }
        if (lastDamage.getCause() == EntityDamageEvent.DamageCause.FALL ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.CONTACT ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.MAGIC ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.POISON ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.CUSTOM ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.DRYOUT ||
                lastDamage.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            return jugador.getKiller();
        }
        return null;
    }

    public void mostrarTopJugadores() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StringBuilder mensaje = new StringBuilder();

            mensaje.append("\n" + ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                             " + ChatColor.RESET + "\n");
            mensaje.append("         " + ChatColor.GOLD + ChatColor.BOLD + "Top Jugadores\n\n");
            List<String> topJugadores = new ArrayList<>(ordenEliminados);
            Collections.reverse(topJugadores);

            for (String superviviente : participantes) {
                if (!topJugadores.contains(superviviente)) {
                    topJugadores.add(0, superviviente);
                }
            }

            String[] colores = {
                    ChatColor.of("#ffbf00").toString(),
                    ChatColor.of("#e3e4e5").toString(),
                    ChatColor.of("#cd7f32").toString(),
                    ChatColor.of("#00aaaa").toString(),
                    ChatColor.of("#00aaaa").toString()
            };

            for (int i = 0; i < Math.min(5, topJugadores.size()); i++) {
                String jugador = topJugadores.get(i);
                int killsJugador = kills.getOrDefault(jugador, 0);
                String colorNombre = colores[Math.min(i, colores.length - 1)];

                if (i == 3) {
                    mensaje.append("\n");
                }

                mensaje.append(String.format(
                        ChatColor.GRAY + "" + ChatColor.BOLD + "%d. " +
                                colorNombre + "%s " +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + "(" +
                                ChatColor.RED + "" + ChatColor.BOLD + "%d" +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + ")\n",
                        i + 1, jugador, killsJugador
                ));
            }

            mensaje.append(ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                             " + ChatColor.RESET + "\n");

            enviarMensajeZona(mensaje.toString());

        }, 100L);
    }

    private void declararGanador() {
        for (String jugador : participantes) {
            Player ganador = Bukkit.getPlayer(jugador);
            if (ganador != null) {
                cancelarEfectosBorde();
                reiniciarVariablesBorde();

                if (musicTask != null) {
                    musicTask.cancel();
                    musicTask = null;
                }

                enviarMensajeZona("§d۞§6§l " + ganador.getName() + " §f§lha ganado el evento §e§lLavaClash!§7§l.");

                getJugadoresEnZona().forEach(p -> {
                    p.stopSound(SoundCategory.RECORDS);
                    p.stopSound("minecraft:music_disc.lava_chicken", SoundCategory.RECORDS);
                    p.stopSound("minecraft:music_disc.pigstep", SoundCategory.RECORDS);
                    p.stopSound("minecraft:music_disc.creator", SoundCategory.RECORDS);
                    p.stopSound("minecraft:music_disc.tears", SoundCategory.RECORDS);
                    p.stopSound("minecraft:music_disc.precipice", SoundCategory.RECORDS);
                });

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick rate 20");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:speed");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect clear @a minecraft:night_vision");

                habilidadesManager.enableHabilidades(ganador);
                habilidadesEffects.reapplyAllEffects(ganador, habilidadesManager);

                ganador.teleport(zonaEspectadores);
                ganador.getInventory().clear();

                ganador.setHealth(ganador.getAttribute(Attribute.MAX_HEALTH).getValue());

                for (Player p : getJugadoresEnZona()) {
                    p.sendTitle(
                            "§6§lGANADOR",
                            "§8§l>§6§l " + ganador.getName() + "§8§l <",
                            15, 150, 15
                    );
                    p.playSound(p.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, 5, 1);
                }

                Bukkit.getScheduler().runTaskLater(plugin, this::mostrarTopJugadores, 100L);
                Bukkit.getScheduler().runTaskLater(plugin, this::terminarEvento, 300L);
                break;
            }
        }
    }

    private void restaurarShroomlights() {
        for (Location loc : ubicacionesShroomlightOriginales) {
            loc.getBlock().setType(Material.SHROOMLIGHT);
        }
    }

    public void eliminarPurpleConcrete() {
        for (int x = arenaMinX; x <= arenaMaxX; x++) {
            for (int y = arenaMinY; y <= arenaMaxY; y++) {
                for (int z = arenaMinZ; z <= arenaMaxZ; z++) {
                    Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                    if (loc.getBlock().getType() == Material.PURPLE_CONCRETE || loc.getBlock().getType() == Material.COBWEB) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void aplicarDanioFueraDelBorde() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo || !eventoEnCurso) {
                    this.cancel();
                    return;
                }

                for (String jugador : new ArrayList<>(participantes)) {
                    Player p = Bukkit.getPlayer(jugador);
                    if (p != null && p.getGameMode() == GameMode.SURVIVAL) {
                        Location loc = p.getLocation();
                        if (loc.getX() < minX || loc.getX() > maxX || loc.getZ() < minZ || loc.getZ() > maxZ) {
                            double nuevaVida = p.getHealth() - 1.0;
                            if (nuevaVida <= 0.3) {
                                procesarEliminacionJugador(p, null, p.getName(), "");
                            } else {
                                p.damage(1.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void guardarEstadoEvento() {
        FileConfiguration configState = YamlConfiguration.loadConfiguration(estadoArchivo);
        configState.set("eventoActivo", eventoActivo);
        try {
            configState.save(estadoArchivo);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el estado del evento: " + e.getMessage());
        }
    }

    // Si el server se reinició con el evento activo lo termina para devolver todo
    private void verificarEstadoEvento() {
        if (!estadoArchivo.exists()) return;

        FileConfiguration configState = YamlConfiguration.loadConfiguration(estadoArchivo);
        boolean estadoGuardado = configState.getBoolean("eventoActivo", false);

        if (estadoGuardado) {
            plugin.getLogger().warning("El evento estaba activo antes del reinicio. Finalizando automáticamente...");
            terminarEvento();
        }
    }

    @EventHandler
    public void onBlockEvent(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (isInsideArenaGeneral(block.getLocation())) {
            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB || block.getType() == Material.SCAFFOLDING) {
                event.setDropItems(false);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes romper este bloque aquí.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (isInsideArenaGeneral(block.getLocation())) {

            if (event.getBlockReplacedState().getType() == Material.LAVA) {
                org.bukkit.block.data.Levelled lava = (org.bukkit.block.data.Levelled) event.getBlockReplacedState().getBlockData();
                if (lava.getLevel() == 0) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "No puedes colocar bloques sobre la lava.");
                    return;
                }
            }

            if (!eventoActivo) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
                return;
            }

            if (block.getType() == Material.PURPLE_CONCRETE || block.getType() == Material.COBWEB || block.getType() == Material.SCAFFOLDING) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    EquipmentSlot hand = event.getHand();
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().addItem(new ItemStack(Material.PURPLE_CONCRETE, 1));
                    } else if (hand == EquipmentSlot.OFF_HAND) {
                        ItemStack offHandItem = player.getInventory().getItemInOffHand();
                        if (offHandItem.getType() == Material.PURPLE_CONCRETE) {
                            offHandItem.setAmount(offHandItem.getAmount() + 1);
                            player.getInventory().setItemInOffHand(offHandItem);
                        } else {
                            player.getInventory().setItemInOffHand(new ItemStack(Material.PURPLE_CONCRETE, 1));
                        }
                    }
                });
            } else if (block.getType() == Material.TNT) {
                TNTPrimed tnt = (TNTPrimed) block.getWorld().spawn(block.getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(60);
                block.setType(Material.AIR);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes colocar este bloque aquí.");
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInsideArenaGeneral(event.getLocation())) {
            event.blockList().removeIf(block -> block.getType() != Material.PURPLE_CONCRETE);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (eventoActivo && isInsideArenaGeneral(event.getLocation())) {
            if (event.getEntity() instanceof Monster || event.getEntity() instanceof Creature) {
                if (event instanceof CreatureSpawnEvent creatureEvent) {
                    if (creatureEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                        Player player = creatureEvent.getEntity().getWorld().getNearbyEntities(event.getLocation(), 1, 1, 1).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .findFirst()
                                .orElse(null);

                        if (player != null) {
                            if (event.getEntity() instanceof Monster) {
                                Monster mob = (Monster) event.getEntity();
                                mob.setPersistent(true);
                                mob.setTarget(null);
                                mob.setAware(true);
                                mob.addScoreboardTag("owner:" + player.getUniqueId());
                            }
                        } else {
                            event.setCancelled(true);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (eventoActivo) {
            Entity entity = event.getEntity();

            if (!(entity instanceof Player)) {
                if (event.getTo() != null && isInsideArenaGeneral(event.getTo())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (eventoActivo) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof Monster mob && isInsideArenaGeneral(entity.getLocation())) {
                    if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                        mob.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (eventoActivo && event.getEntity() instanceof Monster mob && event.getTarget() instanceof Player player) {
            if (mob.getScoreboardTags().contains("owner:" + player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    public void eliminarMobsExistentes() {
        if (!eventoActivo) return;
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        org.bukkit.util.BoundingBox arenaBox = new org.bukkit.util.BoundingBox(arenaMinX, arenaMinY, arenaMinZ, arenaMaxX, arenaMaxY, arenaMaxZ);

        for (Entity entity : world.getNearbyEntities(arenaBox)) {
            if (entity instanceof Monster mob) {
                if (!mob.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("owner:"))) {
                    mob.remove();
                }
            }
        }
    }

    public void espera1() {
        new BukkitRunnable() {
            @Override
            public void run() {
                getJugadoresEnZona().forEach(p -> {
                    p.playSound(p.getLocation(), "minecraft:music_disc.stal", SoundCategory.RECORDS, 15.0f, 1.3f);
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player p : getJugadoresEnZona()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"\\u06de instrucciones \",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\"\\u27a4\",\"color\":\"gray\"},{\"text\":\"\\n\\n\"},{\"text\":\"Bienvenidos todos al primer evento \",\"color\":\"#C55CF3\"},{\"text\":\"\\\"Lavaclash\\\"\",\"bold\":true,\"color\":\"gold\"},{\"text\":\".\\nAquí unas pequeñas instrucciones para que sepan\\nde qué trata:\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\\n\"}]");
                            p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                        }

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (Player p : getJugadoresEnZona()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"El \",\"color\":\"#C55CF3\"},{\"text\":\"LavaClash\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" básicamente consiste en una\\ncombinación de \",\"color\":\"#C55CF3\"},{\"text\":\"\\\"skywars\\\"\",\"bold\":true,\"color\":\"#518BEC\"},{\"text\":\" y \",\"color\":\"#C55CF3\"},{\"text\":\"\\\"skybattle\\\"\",\"bold\":true,\"color\":\"#518BEC\"},{\"text\":\". Básicamente,\\ndeberán \",\"color\":\"#C55CF3\"},{\"text\":\"lootear\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\" y \",\"color\":\"#C55CF3\"},{\"text\":\"matar\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\" a los jugadores.\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\\n\\n\"}]");
                                    p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                                }

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        for (Player p : getJugadoresEnZona()) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"En el mapa habran \",\"color\":\"#C55CF3\"},{\"text\":\"items especiales\",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\" que les ayudara\\na tener ventajas de otros, como las \",\"color\":\"#C55CF3\"},{\"text\":\"\\\"WindCharge\\\"\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"\\n\"},{\"text\":\"\\\"Manzana de vida\\\"\",\"bold\":true,\"color\":\"#EC6451\"},{\"text\":\" o la \",\"color\":\"#C55CF3\"},{\"text\":\"\\\"Pluma de Levitación\\\"\",\"bold\":true,\"color\":\"#518BEC\"},{\"text\":\"\\n\\n\\n\"}]");
                                            p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                                        }

                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                for (Player p : getJugadoresEnZona()) {
                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"Cada \",\"color\":\"#C55CF3\"},{\"text\":\"2 minutos\",\"bold\":true,\"color\":\"red\"},{\"text\":\" habrá un borde que se \",\"color\":\"#C55CF3\"},{\"text\":\"reducirá\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\"\\nhasta llegar al \",\"color\":\"#C55CF3\"},{\"text\":\"centro del mapa\",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\".\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\\n\\n\"}]");
                                                    p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                                                }

                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        for (Player p : getJugadoresEnZona()) {
                                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"El \",\"color\":\"#C55CF3\"},{\"text\":\"último\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\" que quede vivo \",\"color\":\"#C55CF3\"},{\"text\":\"ganará\",\"bold\":true,\"color\":\"gold\"},{\"text\":\", y habrá un\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\"},{\"text\":\"top 5\",\"bold\":true,\"color\":\"red\"},{\"text\":\" de los últimos supervivientes con un contador\\nde kills.\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\\n\\n\"}]");
                                                            p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                                                        }

                                                        new BukkitRunnable() {
                                                            @Override
                                                            public void run() {
                                                                for (Player p : getJugadoresEnZona()) {
                                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + p.getName() + " [\"\",{\"text\":\"\\n\\n\\n\\n\\n\"},{\"text\":\"Los últimos " + topRecompensas + "\",\"bold\":true,\"color\":\"#C55CF3\"},{\"text\":\" que sobrevivan al \",\"color\":\"#C55CF3\"},{\"text\":\"LavaClash\",\"bold\":true,\"color\":\"gold\"},{\"text\":\" recibirán\\nuna \",\"color\":\"#C55CF3\"},{\"text\":\"recompensa\",\"bold\":true,\"color\":\"#B8F794\"},{\"text\":\".\",\"color\":\"#C55CF3\"},{\"text\":\"\\n\\n\"},{\"text\":\"¡Buena suerte a los participantes!\",\"bold\":true,\"color\":\"light_purple\"},{\"text\":\"\\n\\n\\n\"}]");
                                                                    p.playSound(p.getLocation(), "minecraft:block.note_block.pling", SoundCategory.AMBIENT, 1, 1.3f);
                                                                }
                                                            }
                                                        }.runTaskLater(plugin, 20 * 6);
                                                    }
                                                }.runTaskLater(plugin, 20 * 5);
                                            }
                                        }.runTaskLater(plugin, 20 * 10);
                                    }
                                }.runTaskLater(plugin, 20 * 10);
                            }
                        }.runTaskLater(plugin, 20 * 10);
                    }
                }.runTaskLater(plugin, 20 * 5);
            }
        }.runTaskLater(plugin, 20 * 4);
    }

    public void guardarContenidoCofres() {
        cofresHandler.guardarContenidoCofres(arenaMinX, arenaMaxX, arenaMinZ, arenaMaxZ);
    }

    public void restaurarContenidoCofres() {
        cofresHandler.restaurarContenidoCofres();
    }

    public void cargarCofresDesdeArchivo() {
        cofresHandler.cargarCofresDesdeArchivo();
    }

    private void guardarDatosFinales() {
        File archivo = new File(plugin.getDataFolder(), "resultados.yml");
        FileConfiguration resultsConfig = YamlConfiguration.loadConfiguration(archivo);

        resultsConfig.set("orden_eliminados", ordenEliminados);
        for (Map.Entry<String, Integer> entry : kills.entrySet()) {
            resultsConfig.set("kills." + entry.getKey(), entry.getValue());
        }

        try {
            resultsConfig.save(archivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelarTareasActivas() {
        for (BukkitTask tarea : tareasActivas) {
            if (tarea != null && !tarea.isCancelled()) {
                tarea.cancel();
            }
        }
        tareasActivas.clear();
    }

    private void cancelarEfectosBorde() {
        getJugadoresEnZona().forEach(p -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title " + p.getName() + " clear");
            p.stopSound("block.note_block.bell");
        });

        if (taskBordeParticulas != null) {
            taskBordeParticulas.cancel();
            taskBordeParticulas = null;
        }

        if (taskReduccionBorde != null) {
            taskReduccionBorde.cancel();
            taskReduccionBorde = null;
        }

        if (taskReduccionBordeContinuo != null) {
            taskReduccionBordeContinuo.cancel();
            taskReduccionBordeContinuo = null;
        }
    }

    public void gestionarParticipantes(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /lavaclash <list|add|remove> [jugador]");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                listarParticipantes(sender);
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash add <jugador>");
                    return;
                }
                agregarParticipante(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash remove <jugador>");
                    return;
                }
                removerParticipante(sender, args[1]);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando no válido. Usa list, add o remove");
        }
    }

    private void listarParticipantes(CommandSender sender) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append(ChatColor.GOLD).append("=== Participantes (").append(participantes.size()).append("/").append(MAX_PARTICIPANTES).append(") ===\n");

        for (String nombre : participantes) {
            Player p = Bukkit.getPlayer(nombre);
            String estado = (p != null && p.isOnline()) ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline";
            mensaje.append(ChatColor.YELLOW).append("- ").append(nombre).append(": ").append(estado).append("\n");
        }

        sender.sendMessage(mensaje.toString());
    }

    private void agregarParticipante(CommandSender sender, String nombreJugador) {
        if (participantes.contains(nombreJugador)) {
            sender.sendMessage(ChatColor.RED + "El jugador ya está en la lista de participantes");
            return;
        }

        if (participantes.size() >= MAX_PARTICIPANTES) {
            sender.sendMessage(ChatColor.RED + "No hay espacio para más participantes");
            return;
        }

        participantes.add(nombreJugador);
        Player p = Bukkit.getPlayer(nombreJugador);
        if (p != null) {
            aplicarTeamLavaClash(p);

            if (eventoEnCurso || preparacion) {
                habilidadesManager.disableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);

                if (eventInventoryManager != null) {
                    eventInventoryManager.saveAndClearInventory(p);
                }

                if (eventoEnCurso) {
                    darKitBatalla(p, participantes.size());
                }
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Jugador " + nombreJugador + " agregado al evento");

        if (modoIngreso.equalsIgnoreCase("block")) {
            String jsonMessage = "[\"\","
                    + "{\"text\":\"\u06de\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + nombreJugador + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\" ha obtenido el \",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"Quaso ticket\",\"bold\":true,\"color\":\"#E9BF66\"},"
                    + "{\"text\":\" - Ticket:\",\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\" " + participantes.size() + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"/\",\"bold\":true,\"color\":\"#BA7FD0\"},"
                    + "{\"text\":\"" + MAX_PARTICIPANTES + "\",\"bold\":true,\"color\":\"#863ECF\"},"
                    + "{\"text\":\"\\n \"}"
                    + "]";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
        }
    }

    private void removerParticipante(CommandSender sender, String nombreJugador) {
        if (!participantes.contains(nombreJugador)) {
            sender.sendMessage(ChatColor.RED + "El jugador no está en la lista de participantes");
            return;
        }

        participantes.remove(nombreJugador);
        restaurarTeamOriginal(nombreJugador);
        Player p = Bukkit.getPlayer(nombreJugador);

        if (p != null) {
            if (preparacion || eventoEnCurso) {
                p.getInventory().clear();
                if (eventInventoryManager != null) {
                    eventInventoryManager.restoreInventory(p);
                }
                habilidadesManager.enableHabilidades(p);
                habilidadesEffects.reapplyAllEffects(p, habilidadesManager);
            } else {
                for (ItemStack item : p.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.PAPER && item.hasItemMeta() &&
                            item.getItemMeta().getDisplayName() != null &&
                            item.getItemMeta().getDisplayName().contains("Quaso Ticket")) {

                        p.getInventory().remove(item);
                    }
                }
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Jugador " + nombreJugador + " eliminado del evento");
    }

    public void excludePlayer(CommandSender sender, String playerName) {
        if (excludedPlayers.contains(playerName)) {
            sender.sendMessage(ChatColor.YELLOW + "El jugador " + playerName + " ya estaba en la lista de excluidos.");
        } else {
            excludedPlayers.add(playerName);
            sender.sendMessage(ChatColor.GREEN + "Jugador " + playerName + " ha sido excluido del evento.");

            if (participantes.contains(playerName)) {
                removerParticipante(sender, playerName);
            }
        }
    }
}