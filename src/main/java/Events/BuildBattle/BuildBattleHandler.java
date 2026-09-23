package Events.BuildBattle;

import Commands.TiempoCommand;
import Handlers.EventInventoryManager;
import Handlers.Teams.TeamType;
import TitleListener.EventoAnimation;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BuildBattleHandler implements Listener {

    private final JavaPlugin plugin;
    private final TiempoCommand tiempoCommand;
    private final EventoAnimation eventoAnimation;
    private EventInventoryManager eventInventoryManager;

    private final List<String> votantes = new ArrayList<>();
    private final List<String> admins = new ArrayList<>();
    private final List<String> participantes = new ArrayList<>();
    private final Map<Integer, BuildBattleParcel> parcelas = new HashMap<>();
    private final Map<String, String> originalTeams = new HashMap<>();
    private final List<String> pendingTpFinal = new ArrayList<>();
    private final List<BukkitTask> tareasActivas = new ArrayList<>();

    private final Map<Long, List<BuildBattleParcel>> chunkToParcels = new HashMap<>();
    private final Set<UUID> playersInArena = new HashSet<>();
    private BukkitTask locationTrackerTask;

    private final Set<UUID> inventariosGuardados = new HashSet<>();

    public final Map<UUID, Location> pos1Map = new HashMap<>();
    public final Map<UUID, Location> pos2Map = new HashMap<>();

    private boolean eventoIniciado = false;
    private boolean tpRealizado = false;
    private String fase = "Lobby";
    private String categoria = "Ninguna";
    private Location centroMapa;
    private int topGanadoresConfig = 3;
    private BukkitTask timerTask;
    private BukkitTask entityCleanupTask;
    private int timeLeftBuild = 0;

    // --- LOGICA DE TORNEO ---
    private String modoJuego = "NORMAL";
    private int rondasMaximas = 3;
    private int rondaActual = 1;
    private File puntajesFile;
    private FileConfiguration puntajesConfig;
    // ------------------------

    private boolean isTieBreaker = false;
    private boolean desempateJugado = false; // Flag de prevención de doble desempate
    private final List<String> empatadosNames = new ArrayList<>();

    private final List<String> cancionesDisponibles = new ArrayList<>();
    private final Map<String, Integer> duracionCanciones = new HashMap<>();
    private int indexCancion = 0;
    private BukkitTask musicTask;

    private int arenaMinX, arenaMaxX;
    private int arenaMinY, arenaMaxY;
    private int arenaMinZ, arenaMaxZ;

    private final Map<UUID, Integer> jugadorViendoParcelaId = new HashMap<>();
    private final Map<UUID, String> jugadorVotandoCategoria = new HashMap<>();

    private File configFile, dataFile;
    private FileConfiguration config, data;

    private final String cPrimary = ChatColor.of("#ae52e3").toString();
    private final String cSecondary = ChatColor.of("#F977F9").toString();
    private final String cCat = org.bukkit.ChatColor.AQUA.toString();

    public BuildBattleHandler(JavaPlugin plugin, TiempoCommand tiempoCommand) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.eventoAnimation = new EventoAnimation(plugin);
        loadFiles(); // Se ejecuta al iniciar

        duracionCanciones.put("minecraft:music_disc.creator", (2 * 60 + 56) * 20);
        duracionCanciones.put("minecraft:music_disc.precipice", (4 * 60 + 59) * 20);
        duracionCanciones.put("minecraft:music_disc.stal", (2 * 60 + 30) * 20);
        duracionCanciones.put("minecraft:music_disc.tears", (2 * 60 + 55) * 20);
        duracionCanciones.put("minecraft:music_disc.wait", (3 * 60 + 57) * 20);
        duracionCanciones.put("minecraft:music_disc.otherside", (3 * 60 + 15) * 20);
        duracionCanciones.put("minecraft:music_disc.pigstep", (2 * 60 + 28) * 20);
        duracionCanciones.put("minecraft:music_disc.relic", (3 * 60 + 38) * 20);
        duracionCanciones.put("minecraft:music_disc.lava_chicken", (2 * 60 + 14) * 20);

        cancionesDisponibles.addAll(duracionCanciones.keySet());

        startLocationTracker();
    }

    private void startLocationTracker() {
        locationTrackerTask = new BukkitRunnable() {
            @Override
            public void run() {
                playersInArena.clear();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isInsideArenaFast(p.getLocation())) {
                        playersInArena.add(p.getUniqueId());

                        if (eventoIniciado) {
                            Block bDown = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                            if (bDown.getType() == Material.REINFORCED_DEEPSLATE) {
                                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 4, false, false, false));
                            }

                            boolean isVotadorOrAdmin = votantes.contains(p.getName()) || admins.contains(p.getName());
                            boolean isConstruccion = fase.equals("Construccion");

                            BuildBattleParcel parcel = getParcelByLocationFast(p.getLocation());

                            if (parcel != null) {
                                if (isConstruccion && !isVotadorOrAdmin) {
                                    if (parcel.getOwnerUUID() != null && !parcel.getOwnerUUID().equals(p.getUniqueId())) {
                                        boolean parcelaActiva = !isTieBreaker || empatadosNames.contains(parcel.getOwnerName());

                                        if (parcelaActiva) {
                                            BuildBattleParcel myParcel = getParcelByOwner(p.getUniqueId());
                                            if (myParcel != null) p.teleport(myParcel.getCenterBottom());
                                            else p.teleport(centroMapa != null ? centroMapa : p.getWorld().getSpawnLocation());

                                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                                            p.sendMessage(ChatColor.RED + "No puedes entrar a la parcela de un jugador activo.");
                                            continue;
                                        }
                                    }
                                }

                                String owner = parcel.getOwnerName() != null ? parcel.getOwnerName() : "Nadie";
                                String msg = "§fParcela #" + parcel.getId() + " de: " + cSecondary + owner;
                                if (fase.equals("Votacion")) msg += " §8| " + cPrimary + "Puntos: §f" + parcel.getDisplayPoints();
                                p.sendActionBar(msg);

                                if (isVotadorOrAdmin) {
                                    jugadorViendoParcelaId.put(p.getUniqueId(), parcel.getId());
                                }
                            } else {
                                jugadorViendoParcelaId.remove(p.getUniqueId());
                            }
                        }
                    } else {
                        jugadorViendoParcelaId.remove(p.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void backupInventorySafe(Player p) {
        if (p == null) return;
        if (!inventariosGuardados.contains(p.getUniqueId())) {
            if (eventInventoryManager != null) {
                eventInventoryManager.saveAndClearInventory(p);
            } else {
                p.getInventory().clear();
            }
            inventariosGuardados.add(p.getUniqueId());
        } else {
            p.getInventory().clear();
        }
    }

    private void restoreInventorySafe(Player p) {
        if (p == null) return;
        if (inventariosGuardados.contains(p.getUniqueId())) {
            p.getInventory().clear();
            if (eventInventoryManager != null) {
                eventInventoryManager.restoreInventory(p);
            }
            inventariosGuardados.remove(p.getUniqueId());
        }
    }

    public boolean isEventoIniciado() { return eventoIniciado; }
    public boolean isTpRealizado() { return tpRealizado; }
    public String getFase() { return fase; }
    public boolean isPlayerInEvent(String name) {
        return participantes.contains(name) || admins.contains(name) || votantes.contains(name);
    }
    public boolean isVotante(String name) { return votantes.contains(name); }
    public boolean isAdmin(String name) { return admins.contains(name); }
    public boolean isParticipante(String name) { return participantes.contains(name); }

    public void loadFiles() {
        configFile = new File(plugin.getDataFolder() + "/buildbattle", "buildbattleconfig.yml");
        dataFile = new File(plugin.getDataFolder() + "/buildbattle", "buildbattledata.yml");
        puntajesFile = new File(plugin.getDataFolder() + "/buildbattle", "buildbattle_puntaje.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try { configFile.createNewFile(); } catch (IOException ignored) {}
            config = YamlConfiguration.loadConfiguration(configFile);
            config.set("tiempo_construccion", 25);
            config.set("tiempo_desempate", 7);
            config.set("top_ganadores", 3);
            config.set("tp_delay", 5);
            config.set("modo", "NORMAL");
            config.set("rondas_torneo", 3);

            config.set("zona.minX", 21916);
            config.set("zona.maxX", 22086);
            config.set("zona.minY", 97);
            config.set("zona.maxY", 167);
            config.set("zona.minZ", 21868);
            config.set("zona.maxZ", 22134);
            try { config.save(configFile); } catch (IOException ignored) {}
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        modoJuego = config.getString("modo", "NORMAL").toUpperCase();
        rondasMaximas = config.getInt("rondas_torneo", 3);

        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
            data = YamlConfiguration.loadConfiguration(dataFile);
            // CORRECCION DEL MUNDO QUE GENERABA EL ERROR "UNKNOWN WORLD"
            World mundoPrincipal = Bukkit.getWorlds().get(0);
            data.set("centro", new Location(mundoPrincipal, 22000, 101, 22000));
            try { data.save(dataFile); } catch (IOException ignored) {}
        } else {
            data = YamlConfiguration.loadConfiguration(dataFile);
        }

        if (!puntajesFile.exists()) {
            try { puntajesFile.createNewFile(); } catch (IOException ignored) {}
        }
        puntajesConfig = YamlConfiguration.loadConfiguration(puntajesFile);

        arenaMinX = Math.min(config.getInt("zona.minX"), config.getInt("zona.maxX"));
        arenaMaxX = Math.max(config.getInt("zona.minX"), config.getInt("zona.maxX"));
        arenaMinY = Math.min(config.getInt("zona.minY"), config.getInt("zona.maxY"));
        arenaMaxY = Math.max(config.getInt("zona.minY"), config.getInt("zona.maxY"));
        arenaMinZ = Math.min(config.getInt("zona.minZ"), config.getInt("zona.maxZ"));
        arenaMaxZ = Math.max(config.getInt("zona.minZ"), config.getInt("zona.maxZ"));

        // Proteccion: Si estamos en medio de un evento, no borramos las parcelas ni reseteamos.
        // Solo actualizamos coordenadas. Si no, las recargamos enteras.
        if (!eventoIniciado) {
            parcelas.clear();
            chunkToParcels.clear();

            if (data.contains("parcelas") && data.getConfigurationSection("parcelas") != null) {
                for (String key : data.getConfigurationSection("parcelas").getKeys(false)) {
                    try {
                        int id = Integer.parseInt(key);
                        Location p1 = data.getLocation("parcelas." + key + ".p1");
                        Location p2 = data.getLocation("parcelas." + key + ".p2");

                        if (p1 != null && p2 != null && p1.getWorld() != null && p2.getWorld() != null) {
                            BuildBattleParcel parcel = new BuildBattleParcel(id, p1, p2);
                            parcelas.put(id, parcel);

                            for (long chunkKey : parcel.getIntersectingChunks()) {
                                chunkToParcels.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(parcel);
                            }
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("[BuildBattle] ID de parcela no válido: " + key);
                    }
                }
            }
        }

        Location centro = data.getLocation("centro");
        if (centro != null && centro.getWorld() != null) centroMapa = centro;
        topGanadoresConfig = config.getInt("top_ganadores", 3);
    }

    private void guardarPuntajeYML(String playerName, int puntos) {
        puntajesConfig.set("puntajes." + playerName, puntos);
        try { puntajesConfig.save(puntajesFile); } catch (IOException ignored) {}
    }

    private boolean isInsideArenaFast(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= arenaMinX && x <= arenaMaxX && y >= arenaMinY && y <= arenaMaxY && z >= arenaMinZ && z <= arenaMaxZ;
    }

    public BuildBattleParcel getParcelByLocationFast(Location loc) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        long chunkKey = ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);

        List<BuildBattleParcel> inChunk = chunkToParcels.get(chunkKey);
        if (inChunk != null) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            for (BuildBattleParcel p : inChunk) {
                if (p.isInsideFast(x, y, z)) return p;
            }
        }
        return null;
    }

    private boolean isEliminatedInTieBreaker(Player p) {
        return isTieBreaker && participantes.contains(p.getName()) && !empatadosNames.contains(p.getName());
    }

    public void saveParcel(int id, Location p1, Location p2) {
        data.set("parcelas." + id + ".p1", p1);
        data.set("parcelas." + id + ".p2", p2);
        try { data.save(dataFile); } catch (IOException ignored) {}

        BuildBattleParcel parcel = new BuildBattleParcel(id, p1, p2);
        parcelas.put(id, parcel);
        for (long chunkKey : parcel.getIntersectingChunks()) {
            chunkToParcels.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(parcel);
        }
    }

    public void removeParcel(int id) {
        data.set("parcelas." + id, null);
        try { data.save(dataFile); } catch (IOException ignored) {}

        BuildBattleParcel parcel = parcelas.remove(id);
        if (parcel != null) {
            for (long chunkKey : parcel.getIntersectingChunks()) {
                List<BuildBattleParcel> list = chunkToParcels.get(chunkKey);
                if (list != null) list.remove(parcel);
            }
        }
    }

    public void setCentro(Location loc) {
        this.centroMapa = loc;
        data.set("centro", loc);
        try { data.save(dataFile); } catch (IOException ignored) {}
    }

    public void iniciarEvento() {
        if (eventoIniciado) return;
        eventoIniciado = true;
        tpRealizado = false;
        fase = "Lobby";
        isTieBreaker = false;
        desempateJugado = false;
        empatadosNames.clear();
        participantes.clear();
        pendingTpFinal.clear();
        inventariosGuardados.clear();

        rondaActual = 1;
        puntajesConfig.set("puntajes", null);
        try { puntajesConfig.save(puntajesFile); } catch (IOException ignored) {}

        World w = Bukkit.getWorlds().get(0);
        if (w != null) w.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, false);

        inicializarTeam();
        Collections.shuffle(cancionesDisponibles);

        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            if (!votantes.contains(name) && !admins.contains(name)) {
                participantes.add(name);
                aplicarTeam(p);
            } else {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            }
        }
        eliminarMobsExistentes();

        entityCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventoIniciado) return;
            World world = centroMapa != null ? centroMapa.getWorld() : Bukkit.getWorlds().get(0);
            if (world != null) {
                for (Entity ent : world.getEntities()) {
                    if (ent instanceof Player || ent instanceof TextDisplay || ent instanceof Painting || ent instanceof ItemFrame || ent instanceof ArmorStand) continue;
                    if (isInsideArenaFast(ent.getLocation())) {
                        if (getParcelByLocationFast(ent.getLocation()) == null) {
                            ent.remove();
                        }
                    }
                }
            }
        }, 0L, 60L);

        String jsonStart = "[\"\",{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\\n\\n\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"Ha empezado el evento \",\"color\":\"#c55cf3\"},{\"text\":\"Quaso Build Battle\",\"bold\":true,\"color\":\"#ae52e3\"},{\"text\":\".\\nTodos los seleccionados serán tepeados a unas zonas\\ndonde deberán construir de acuerdo a la categoría\\nque se asigne con un tiempo límite.\\n\\n\",\"color\":\"#c55cf3\"},{\"text\":\"En cuestión de minutos los jugadores serán tepeados a la zona del evento.\",\"color\":\"#F977F9\"}]";
        for (Player p : Bukkit.getOnlinePlayers()) {
            eventoAnimation.playAnimation(p, jsonStart);
        }
    }

    public void forceTp() {
        for (String name : participantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                if (isTieBreaker && !empatadosNames.contains(name)) continue;

                BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                if (parcel == null) {
                    parcel = getEmptyParcel();
                    if (parcel != null) {
                        parcel.setOwner(p.getUniqueId(), p.getName());
                    }
                }

                if (parcel != null) {
                    p.teleport(parcel.getCenterBottom());
                    if (fase.equals("Construccion")) {
                        p.setGameMode(GameMode.CREATIVE);
                    } else if (fase.equals("Votacion")) {
                        p.setGameMode(GameMode.ADVENTURE);
                        p.setAllowFlight(true);
                        p.setFlying(true);
                    } else {
                        p.setGameMode(GameMode.CREATIVE);
                    }
                } else {
                    p.sendMessage(cSecondary + "No hay más parcelas disponibles.");
                }
            }
        }
        broadcastEventZona("§aParticipantes forzados a sus parcelas.");
    }

    private void realizarTpProgresivo(Location center) {
        List<Player> aTeletransportar = new ArrayList<>();
        for (String name : votantes) { Player p = Bukkit.getPlayer(name); if (p != null) aTeletransportar.add(p); }
        for (String name : admins) { Player p = Bukkit.getPlayer(name); if (p != null) aTeletransportar.add(p); }
        for (String name : participantes) { Player p = Bukkit.getPlayer(name); if (p != null) aTeletransportar.add(p); }

        int total = aTeletransportar.size();

        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= aTeletransportar.size() || !eventoIniciado) {
                    this.cancel();
                    for (Player op : Bukkit.getOnlinePlayers()) if (op.isOp()) op.sendMessage(cPrimary + "TP realizado correctamente a " + total + " jugadores.");
                    actualizarScoreboards();
                    return;
                }
                Player p = aTeletransportar.get(index++);
                backupInventorySafe(p);

                if (participantes.contains(p.getName())) {
                    if (isTieBreaker && !empatadosNames.contains(p.getName())) return;
                    BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                    if (parcel == null) parcel = getEmptyParcel();

                    if (parcel != null) {
                        parcel.setOwner(p.getUniqueId(), p.getName());
                        teleportMagico(p, parcel.getCenterBottom());
                        p.setGameMode(GameMode.CREATIVE);
                    } else {
                        p.sendMessage(cSecondary + "No hay más parcelas disponibles. Pasas a espectador.");
                        p.setGameMode(GameMode.SPECTATOR);
                        teleportMagico(p, center);
                    }
                } else {
                    teleportMagico(p, center);
                }
            }
        }.runTaskTimer(plugin, 0L, 7L);
    }

    public void tpZona() {
        if (!eventoIniciado) return;
        int delay = config.getInt("tp_delay", 5);
        String tiempoFormateado = String.format("%02d:%02d", delay / 60, delay % 60);

        tiempoCommand.createBossBar("bb_timer", delay, "00:" + tiempoFormateado, "on");
        tiempoCommand.updateBossBarDisplayName("bb_timer", cPrimary + "§lTeletransporte en§f:");

        String textoDelay = (delay / 60 > 0 ? delay / 60 + " min " : "") + (delay % 60 > 0 ? delay % 60 + " seg" : "");
        String msgTp = "[\"\",{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\\n\\n\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"Todos los jugadores serán tepeados en \",\"color\":\"#c55cf3\"},{\"text\":\"" + textoDelay + "\",\"bold\":true,\"color\":\"#ae52e3\"},{\"text\":\".\\nPor favor se recomienda guardar spawn en sus bases y guardar lo más importante.\\n\\n\",\"color\":\"#c55cf3\"},{\"text\":\"(Si no te da tiempo a guardar los objetos no pasa nada,\\nse te devolverá todo al terminar el evento)\",\"italic\":true,\"color\":\"#aaaaaa\"}]";

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendJsonMessage(p, msgTp);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 1.2f);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                tiempoCommand.removeBossBar("bb_timer");
                tpRealizado = true;
                Location center = centroMapa != null ? centroMapa.clone().add(0, 2, 0) : Bukkit.getWorlds().get(0).getSpawnLocation();
                realizarTpProgresivo(center);
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    public void mostrarReglas() {
        if (!eventoIniciado) return;

        String[] reglas = {
                "[\"\",{\"text\":\"\\u06de Evento\",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\" \\u27a4\\n\\n\",\"bold\":true,\"color\":\"gray\"},{\"text\":\"Bienvenidos a todos al evento \",\"color\":\"#c55cf3\"},{\"text\":\"Quaso Build Battle\",\"bold\":true,\"color\":\"#ae52e3\"},{\"text\":\"\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\"},{\"text\":\"En este evento deberán competir por quién es mejor constructor\\ntendrán un tiempo límite para construir.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\"},{\"text\":\"Tendrán que construir de acuerdo con la \",\"color\":\"#c55cf3\"},{\"text\":\"categoría \",\"bold\":true,\"color\":\"#F977F9\"},{\"text\":\"que se asigne.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\"},{\"text\":\"Con el comando \",\"color\":\"#c55cf3\"},{\"text\":\"/floor\",\"bold\":true,\"color\":\"#ae52e3\"},{\"text\":\" y el bloque que tengan en la mano,\\nel suelo se transforma de ese bloque.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\"},{\"text\":\"Al finalizar el tiempo, los votadores elegirán según varios criterios:\\n\",\"color\":\"#c55cf3\"},{\"text\":\"Originalidad\",\"bold\":true,\"color\":\"green\"},{\"text\":\" - \",\"color\":\"gray\"},{\"text\":\"Complejidad\",\"bold\":true,\"color\":\"aqua\"},{\"text\":\" e \",\"color\":\"gray\"},{\"text\":\"Impacto Visual\",\"bold\":true,\"color\":\"gold\"},{\"text\":\"\\nCada criterio cuesta 10 puntos, en total 30 por votador.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"}]",
                "[\"\",{\"text\":\"\\n\\n\"},{\"text\":\"Los jugadores con mayor puntaje tendrán una recompensa.\",\"color\":\"#c55cf3\"},{\"text\":\"\\n\"},{\"text\":\"¡Buena suerte!\",\"bold\":true,\"color\":\"#ae52e3\"},{\"text\":\"\\n\"}]"
        };

        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.playSound(p.getLocation(), "minecraft:music_disc.stal", SoundCategory.RECORDS, Float.MAX_VALUE, 1.3f);
        }

        int[] delays = {0, 3, 4, 4, 5, 8};
        long accum = 0;
        for (int i = 0; i < reglas.length; i++) {
            final String regla = reglas[i];
            accum += (delays[i] * 20L);

            tareasActivas.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID id : playersInArena) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        sendJsonMessage(p, regla);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, Float.MAX_VALUE, 1.3f);
                    }
                }
            }, accum));
        }
    }

    public void startBuildPhase() {
        if (categoria.equals("Ninguna")) {
            broadcastEventZona("§c§lERROR §8» §fFalta asignar una categoría.");
            return;
        }

        cancelarTareasActivas();
        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.stopSound(SoundCategory.RECORDS);
        }

        new BukkitRunnable() {
            int contador = 10;
            @Override
            public void run() {
                if (!eventoIniciado) { this.cancel(); return; }

                if (contador > 0) {
                    float pitch = 1.0f;
                    String colorCode = "§d";
                    if (contador == 3) { colorCode = "§e"; pitch = 1.8f; }
                    else if (contador == 2) { colorCode = "§6"; pitch = 1.9f; }
                    else if (contador == 1) { colorCode = "§c"; pitch = 2.0f; }
                    else if (contador == 10) { pitch = 1.1f; }
                    else if (contador == 9) { pitch = 1.2f; }
                    else if (contador == 8) { pitch = 1.3f; }
                    else if (contador == 7) { pitch = 1.4f; }
                    else if (contador == 6) { pitch = 1.5f; }
                    else if (contador == 5) { pitch = 1.6f; }
                    else if (contador == 4) { pitch = 1.7f; }

                    String titulo = cPrimary + "§lEmpezando: ";
                    String subtitulo = "§l▶ " + colorCode + "§l" + contador + " §f§l◀";

                    for (UUID id : playersInArena) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS, Float.MAX_VALUE, pitch);
                            p.sendTitle(titulo, subtitulo, 0, 40, 0);
                        }
                    }
                    contador--;
                } else {
                    for (UUID id : playersInArena) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) {
                            p.sendTitle(cPrimary + "§l¡CONSTRUYE!", "§fCategoría: " + cCat + categoria, 15, 40, 15);
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                        }
                    }
                    this.cancel();
                    iniciarTemporizadorYMusica();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void iniciarTemporizadorYMusica() {
        fase = "Construccion";
        timeLeftBuild = config.getInt("tiempo_construccion", 25) * 60;

        tiempoCommand.createBossBar("bb_timer", timeLeftBuild, String.format("00:%02d:%02d", timeLeftBuild / 60, timeLeftBuild % 60), "on");
        tiempoCommand.updateBossBarDisplayName("bb_timer", cPrimary + "§lTiempo restante§f:");

        for (String name : participantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                p.getInventory().clear();
                if (isTieBreaker && !empatadosNames.contains(name)) continue;

                BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                if (parcel != null) {
                    p.teleport(parcel.getCenterBottom());
                    p.setGameMode(GameMode.CREATIVE);
                }
            }
        }

        // --- Asegurar que Jueces y Admins no se tepeen a sus parcelas ---
        for (String name : votantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) darItemCofreConstruccion(p);
        }
        for (String name : admins) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) darItemCofreConstruccion(p);
        }

        playNextSong();
        actualizarScoreboards();

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoIniciado) { this.cancel(); return; }
                timeLeftBuild--;
                if (timeLeftBuild <= 0) {
                    this.cancel();
                    startVotingPhase();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void playNextSong() {
        if (!eventoIniciado || !fase.equals("Construccion")) return;

        String cancion = cancionesDisponibles.get(indexCancion % cancionesDisponibles.size());
        int durationTicks = duracionCanciones.getOrDefault(cancion, 3600);
        indexCancion++;

        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.stopSound(SoundCategory.RECORDS);
                p.playSound(p.getLocation(), cancion, SoundCategory.RECORDS, Float.MAX_VALUE, 1.0f);
            }
        }

        musicTask = Bukkit.getScheduler().runTaskLater(plugin, this::playNextSong, durationTicks);
    }

    public void skipBuildPhase() {
        if (fase.equals("Construccion") || fase.equals("Desempate_Lobby")) {
            if (timerTask != null) timerTask.cancel();
            startVotingPhase();
        } else {
            broadcastEventZona("§cNo se puede omitir porque no estamos en fase de construcción.");
        }
    }

    private void startVotingPhase() {
        fase = "Votacion";
        tiempoCommand.removeBossBar("bb_timer");
        if (musicTask != null) musicTask.cancel();

        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.stopSound(SoundCategory.RECORDS);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                p.sendTitle(cPrimary + "§lTIEMPO AGOTADO", "§f¡Manos arriba!", 10, 60, 10);
            }
        }

        for (String name : participantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                p.setGameMode(GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(true);
                p.getInventory().clear();
                p.getInventory().setItem(8, createItem(Material.CHEST, "§e§lParcelas"));

                BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                if(parcel != null) {
                    parcel.updateTextDisplay();
                }
            }
        }

        for (String name : votantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) darItemsVotacion(p);
        }
        for (String name : admins) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) darItemsVotacion(p);
        }

        actualizarScoreboards();
    }

    private void mostrarTopYTerminar(List<BuildBattleParcel> validParcels) {
        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            StringBuilder msjTop = new StringBuilder();
            msjTop.append("\n§5§m                                                 \n");
            msjTop.append("         ").append(cPrimary).append("§lTOP JUGADORES BUILD BATTLE\n\n");

            int myRank = -1;
            String myPointsDisplay = "0";
            for (int i = 0; i < validParcels.size(); i++) {
                BuildBattleParcel bp = validParcels.get(i);
                if (i < 5) {
                    String cRank = (i == 0) ? "§6§l" : (i == 1) ? "§f§l" : "§7§l";
                    msjTop.append(cRank).append(i+1).append(". §f").append(bp.getOwnerName())
                            .append(" §8§l- Puntos: ").append(cSecondary).append("§l").append(bp.getDisplayPoints()).append("\n");
                }
                if (bp.getOwnerUUID() != null && bp.getOwnerUUID().equals(p.getUniqueId())) {
                    myRank = i + 1;
                    myPointsDisplay = bp.getDisplayPoints();
                }
            }

            String topRankStr = myRank != -1 ? String.valueOf(myRank) : "N/A";
            String pointsStr = myRank != -1 ? " (" + myPointsDisplay + " puntos)" : "";

            msjTop.append("\n").append(cPrimary).append("§lMi top: §f§l").append(topRankStr).append(pointsStr).append("\n");
            msjTop.append("§5§m                                                 \n");
            p.sendMessage(msjTop.toString());
        }

        new BukkitRunnable() {
            @Override
            public void run() { endEvent(); }
        }.runTaskLater(plugin, 200L);
    }

    // --- NUEVO METODO PARA MOSTRAR TOP DE RONDA SIN TERMINAR EL EVENTO ---
    private void mostrarTopRonda(List<BuildBattleParcel> validParcels) {
        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            StringBuilder msjTop = new StringBuilder();
            msjTop.append("\n§5§m                                                 \n");
            msjTop.append("         ").append(cPrimary).append("§lPUNTAJES - RONDA ").append(rondaActual).append("\n\n");

            int myRank = -1;
            String myPointsDisplay = "0";
            for (int i = 0; i < validParcels.size(); i++) {
                BuildBattleParcel bp = validParcels.get(i);
                if (i < 5) {
                    String cRank = (i == 0) ? "§6§l" : (i == 1) ? "§f§l" : "§7§l";
                    msjTop.append(cRank).append(i+1).append(". §f").append(bp.getOwnerName())
                            .append(" §8§l- Puntos: ").append(cSecondary).append("§l").append(bp.getDisplayPoints()).append("\n");
                }
                if (bp.getOwnerUUID() != null && bp.getOwnerUUID().equals(p.getUniqueId())) {
                    myRank = i + 1;
                    myPointsDisplay = bp.getDisplayPoints();
                }
            }

            String topRankStr = myRank != -1 ? String.valueOf(myRank) : "N/A";
            String pointsStr = myRank != -1 ? " (" + myPointsDisplay + " puntos)" : "";

            msjTop.append("\n").append(cPrimary).append("§lMi ranking actual: §f§l").append(topRankStr).append(pointsStr).append("\n");
            msjTop.append("§5§m                                                 \n");
            p.sendMessage(msjTop.toString());
        }
    }
    // ----------------------------------------------------------------------


    public void forzarGanador() {
        if (!fase.equals("Votacion")) {
            broadcastEventZona("§cSolo se puede forzar en fase de votación.");
            return;
        }

        List<BuildBattleParcel> validParcels = parcelas.values().stream()
                .filter(p -> p.getOwnerUUID() != null)
                .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                .collect(Collectors.toList());

        if (validParcels.isEmpty()) {
            broadcastEventZona("§cNo hubo construcciones válidas.");
            endEvent();
            return;
        }

        BuildBattleParcel top1 = validParcels.get(0);
        Player ganador = Bukkit.getPlayer(top1.getOwnerUUID());
        if (ganador != null) {
            for (UUID id : playersInArena) {
                Player all = Bukkit.getPlayer(id);
                if (all != null) {
                    all.sendTitle(cSecondary + "§lGANADOR", "§f" + ganador.getName(), 10, 100, 10);
                    all.playSound(all.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, Float.MAX_VALUE, 1);
                }
            }
        }
        mostrarTopYTerminar(validParcels);
    }

    public void declararGanadores() {
        if (!fase.equals("Votacion")) return;

        List<BuildBattleParcel> validParcels = parcelas.values().stream()
                .filter(p -> p.getOwnerUUID() != null)
                .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                .collect(Collectors.toList());

        if (validParcels.isEmpty()) {
            broadcastEventZona("§cNo hubo construcciones válidas.");
            endEvent();
            return;
        }

        int totalVoters = votantes.size();
        for (BuildBattleParcel parcel : validParcels) {
            if (isTieBreaker && !empatadosNames.contains(parcel.getOwnerName())) continue;
            if (!parcel.hasVotedAll(totalVoters)) {
                broadcastEventZona("§cNo se puede declarar ganador aún. Faltan votos en la parcela de " + parcel.getOwnerName());
                return;
            }
        }

        // --- LÓGICA DE MODO TORNEO ---
        if (!isTieBreaker && modoJuego.equalsIgnoreCase("TORNEO")) {
            if (rondaActual < rondasMaximas) {
                // Es una ronda intermedia
                Player ganadorRonda = Bukkit.getPlayer(validParcels.get(0).getOwnerUUID());
                if (ganadorRonda != null) {
                    for(UUID id : playersInArena) {
                        Player all = Bukkit.getPlayer(id);
                        if (all != null) {
                            all.sendTitle(cSecondary + "§lGANADOR RONDA " + rondaActual, "§f" + ganadorRonda.getName(), 10, 80, 10);
                            all.playSound(all.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                    }
                }

                mostrarTopRonda(validParcels); // Nuevo: Muestra el chat de puntos

                // Guardar puntos acumulados (para la sig ronda) y registrar en YML
                for (BuildBattleParcel p : validParcels) {
                    p.saveRoundPoints(); // Acumula en la base y limpia los votos recientes
                    guardarPuntajeYML(p.getOwnerName(), p.getTotalPoints());
                }

                // Limpiar parcela física para la nueva ronda
                for (BuildBattleParcel p : validParcels) {
                    p.clearParcel();
                    p.updateTextDisplay();
                }

                World world = centroMapa != null ? centroMapa.getWorld() : Bukkit.getWorlds().get(0);
                if (world != null) {
                    for (Entity ent : world.getEntities()) {
                        if (isInsideArenaFast(ent.getLocation())) {
                            if (ent instanceof Player || ent instanceof TextDisplay) continue;
                            ent.remove();
                        }
                    }
                }

                // Configurar el estado al lobby de nuevo, pero sin tepearlos.
                fase = "Lobby";
                rondaActual++;
                categoria = "Ninguna";

                actualizarScoreboards();
                broadcastEventZona(cPrimary + "§lLa Ronda ha terminado. Puntajes guardados. Preparando la Ronda " + rondaActual + "...");
                return; // Cortamos aquí para que no termine el evento.
            } else {
                // Es la última ronda del torneo. Guardamos también los totales antes de evaluar ganador/empate
                for (BuildBattleParcel p : validParcels) {
                    p.saveRoundPoints();
                    guardarPuntajeYML(p.getOwnerName(), p.getTotalPoints());
                }
            }
        }
        // -----------------------------

        if (isTieBreaker) {
            isTieBreaker = false;
            empatadosNames.clear();

            validParcels = parcelas.values().stream()
                    .filter(p -> p.getOwnerUUID() != null)
                    .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                    .collect(Collectors.toList());

            Player ganador = Bukkit.getPlayer(validParcels.get(0).getOwnerUUID());
            if (ganador != null) {
                for(UUID id : playersInArena) {
                    Player all = Bukkit.getPlayer(id);
                    if (all != null) {
                        all.sendTitle(cSecondary + "§lGANADOR", "§f" + ganador.getName(), 10, 100, 10);
                        all.playSound(all.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, Float.MAX_VALUE, 1);
                    }
                }
            }
            mostrarTopYTerminar(validParcels);
            return;
        }

        Map<Double, List<BuildBattleParcel>> groupedByScore = validParcels.stream()
                .collect(Collectors.groupingBy(BuildBattleParcel::getSortPoints));

        List<Double> sortedScores = validParcels.stream()
                .map(BuildBattleParcel::getSortPoints)
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        Set<Double> puntajesEmpatados = new HashSet<>();
        int placesFilled = 0;

        for (Double score : sortedScores) {
            List<BuildBattleParcel> tiedParcels = groupedByScore.get(score);
            if (placesFilled < topGanadoresConfig) {
                if (placesFilled + tiedParcels.size() > placesFilled && tiedParcels.size() > 1) {
                    puntajesEmpatados.add(score);
                }
                placesFilled += tiedParcels.size();
            } else {
                break;
            }
        }

        if (!puntajesEmpatados.isEmpty()) {
            if (desempateJugado) {
                puntajesEmpatados.clear();
            } else {
                desempateJugado = true;
                isTieBreaker = true;
                empatadosNames.clear();

                List<BuildBattleParcel> emptyParcels = parcelas.values().stream()
                        .filter(p -> p.getOwnerUUID() == null)
                        .collect(Collectors.toList());

                long tiedCount = validParcels.stream().filter(p -> puntajesEmpatados.contains(p.getSortPoints())).count();
                boolean useNewParcels = emptyParcels.size() >= tiedCount;
                Iterator<BuildBattleParcel> newParcelIterator = useNewParcels ? emptyParcels.iterator() : null;

                for (BuildBattleParcel p : validParcels) {
                    if (puntajesEmpatados.contains(p.getSortPoints())) {
                        empatadosNames.add(p.getOwnerName());
                        int oldScore = p.getTotalPoints();

                        if (useNewParcels) {
                            BuildBattleParcel newP = newParcelIterator.next();
                            newP.setOwner(p.getOwnerUUID(), p.getOwnerName());
                            newP.setStoredGlobalScore(oldScore);
                            newP.updateTextDisplay();

                            p.resetOwnerAndPoints();

                            Player player = Bukkit.getPlayer(newP.getOwnerUUID());
                            if (player != null) {
                                player.sendMessage(cPrimary + "¡Has empatado! Se te ha asignado una nueva parcela vacía para el desempate.");
                            }
                        } else {
                            p.clearParcel();
                            p.prepareForTieBreaker();
                            Player player = Bukkit.getPlayer(p.getOwnerUUID());
                            if (player != null) {
                                player.sendMessage(cPrimary + "¡Has empatado! Tu parcela ha sido limpiada para el desempate.");
                            }
                        }
                    } else {
                        p.setEliminated(true);
                    }
                }

                String empatadosStr = String.join("§7, §f", empatadosNames);
                for (UUID id : playersInArena) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.sendTitle(cSecondary + "§l¡DESEMPATE!", "§f" + empatadosStr, 15, 80, 15);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.2f);

                        StringBuilder msjEmpate = new StringBuilder();
                        msjEmpate.append("\n§5§m                                                 \n");
                        msjEmpate.append("         ").append(cPrimary).append("§lEMPATE DETECTADO\n\n");
                        msjEmpate.append("§7Los siguientes jugadores irán a una ronda de desempate:\n");
                        msjEmpate.append("§f").append(empatadosStr).append("\n");
                        msjEmpate.append("§5§m                                                 \n");
                        p.sendMessage(msjEmpate.toString());
                    }
                }

                categoria = "Ninguna";
                fase = "Desempate_Lobby";
                config.set("tiempo_construccion", config.getInt("tiempo_desempate", 7));
                actualizarScoreboards();
                return;
            }
        }

        Player ganador = Bukkit.getPlayer(validParcels.get(0).getOwnerUUID());
        if (ganador != null) {
            for(UUID id : playersInArena) {
                Player all = Bukkit.getPlayer(id);
                if (all != null) {
                    all.sendTitle(cSecondary + "§lGANADOR", "§f" + ganador.getName(), 10, 100, 10);
                    all.playSound(all.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.RECORDS, Float.MAX_VALUE, 1);
                }
            }
        }

        mostrarTopYTerminar(validParcels);
    }

    public void mostrarPuntosAdministrativos(Player sender, int pagina) {
        if (!admins.contains(sender.getName()) && !votantes.contains(sender.getName()) && !sender.hasPermission("buildbattle.admin")) {
            return;
        }

        List<BuildBattleParcel> validParcels = parcelas.values().stream()
                .filter(p -> p.getOwnerUUID() != null)
                .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                .collect(Collectors.toList());

        if (validParcels.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No hay parcelas con dueños para mostrar.");
            return;
        }

        int totalVoters = votantes.size();
        int elementosPorPagina = 15;
        int totalPaginas = (int) Math.ceil((double) validParcels.size() / elementosPorPagina);

        if (pagina < 1) pagina = 1;
        if (pagina > totalPaginas) pagina = totalPaginas;

        int inicio = (pagina - 1) * elementosPorPagina;
        int fin = Math.min(inicio + elementosPorPagina, validParcels.size());

        StringBuilder sb = new StringBuilder();
        sb.append("\n§5§m                                                 \n");
        sb.append("      ").append(cPrimary).append("§lPUNTUACIÓN EN VIVO §8- §7Pág §f").append(pagina).append("§8/§f").append(totalPaginas).append("\n\n");

        for (int i = inicio; i < fin; i++) {
            BuildBattleParcel bp = validParcels.get(i);
            String statusVoto = bp.hasVotedAll(totalVoters) ? "§a✔" : "§c✖";
            sb.append("§8").append(i + 1).append(". ").append(cSecondary).append(bp.getOwnerName())
                    .append(" §8- Pts: §f").append(bp.getDisplayPoints())
                    .append(" ").append(statusVoto).append("\n");
        }

        sb.append("§5§m                                                 \n");
        if (pagina < totalPaginas) {
            sb.append("§7Usa §f/buildbattle puntos ").append(pagina + 1).append(" §7para ver más.\n");
        }

        sender.sendMessage(sb.toString());
    }

    public void resetAllBuilds() {
        for (BuildBattleParcel parcel : parcelas.values()) {
            parcel.clearParcel();
            parcel.resetOwnerAndPoints();
        }

        World world = Bukkit.getWorld("world");
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                if (isInsideArenaFast(entity.getLocation())) {
                    if (entity instanceof Player) continue;
                    entity.remove();
                }
            }
        }

        broadcastEventZona(cPrimary + "Todas las parcelas han sido reseteadas.");
    }

    public void endEvent() {
        eventoIniciado = false;
        tpRealizado = false;
        fase = "Lobby";
        isTieBreaker = false;
        desempateJugado = false;
        categoria = "Ninguna";
        rondaActual = 1;

        if (timerTask != null) timerTask.cancel();
        if (musicTask != null) musicTask.cancel();
        tiempoCommand.removeBossBar("bb_timer");
        cancelarTareasActivas();

        World w = Bukkit.getWorlds().get(0);
        if (w != null) w.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, true);

        pendingTpFinal.clear();

        for (UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(SoundCategory.RECORDS);
        }

        Set<Player> jugadoresAProcesar = new HashSet<>();

        for (String name : participantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) jugadoresAProcesar.add(p);
        }
        for (String name : votantes) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) jugadoresAProcesar.add(p);
        }
        for (String name : admins) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) jugadoresAProcesar.add(p);
        }

        for (Player p : jugadoresAProcesar) {
            restoreInventorySafe(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            restaurarTeamOriginal(p.getName());
            pendingTpFinal.add(p.getName());
        }

        inventariosGuardados.clear();

        for(Player p : Bukkit.getOnlinePlayers()) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        participantes.clear();
        votantes.clear();
        admins.clear();
        empatadosNames.clear();
        jugadorViendoParcelaId.clear();
        jugadorVotandoCategoria.clear();

        loadFiles();

        for (BuildBattleParcel parcel : parcelas.values()) {
            parcel.resetOwnerAndPoints();
        }

        World worldToClean = Bukkit.getWorld("world");
        if (worldToClean != null) {
            for (Entity entity : worldToClean.getEntitiesByClass(TextDisplay.class)) {
                if (isInsideArenaFast(entity.getLocation())) {
                    entity.remove();
                }
            }
        }

        broadcastEventZona("§cEl evento ha terminado.");
    }

    public void spawnFinal() {
        if (pendingTpFinal.isEmpty()) {
            broadcastEventZona("§cNo hay jugadores pendientes de teletransportar.");
            return;
        }
        for (String name : pendingTpFinal) {
            Player p = Bukkit.getPlayer(name);
            if (p != null) teleportMagico(p, p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        pendingTpFinal.clear();
        broadcastEventZona(cPrimary + "Todos han sido teletransportados a sus bases.");
    }

    public void modifyTimer(int minutes) {
        if (fase.equals("Construccion") || fase.equals("Desempate_Lobby")) {
            int seconds = minutes * 60;
            timeLeftBuild += seconds;
            if(timeLeftBuild < 1) timeLeftBuild = 1;

            tiempoCommand.removeBossBar("bb_timer");
            int m = timeLeftBuild / 60;
            int s = timeLeftBuild % 60;
            tiempoCommand.createBossBar("bb_timer", timeLeftBuild, String.format("00:%02d:%02d", m, s), "on");
            tiempoCommand.updateBossBarDisplayName("bb_timer", cPrimary + "§lTiempo restante§f:");
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            BuildBattleParcel fromParcel = getParcelByLocationFast(e.getBlock().getLocation());
            if (fromParcel != null && !fromParcel.isInside(e.getToBlock().getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent e) {
        if (!eventoIniciado) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (isInsideArenaFast(from)) {
            BuildBattleParcel p = getParcelByLocationFast(from);
            if (p != null && !p.isInsideFast(to.getBlockX(), to.getBlockY(), to.getBlockZ())) {
                e.getVehicle().remove();
            }
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent e) {
        if (!eventoIniciado) return;
        for (org.bukkit.block.BlockState b : e.getBlocks()) {
            if (isInsideArenaFast(b.getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getFrom())) e.setCancelled(true);
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getEntity().getLocation())) {
            for (LivingEntity affected : e.getAffectedEntities()) {
                if (!(affected instanceof Player)) {
                    e.setIntensity(affected, 0);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!eventoIniciado) return;
        Projectile proj = e.getEntity();
        if (proj.getShooter() instanceof Player p) {
            if (isInsideArenaFast(proj.getLocation())) {
                BuildBattleParcel pShooter = getParcelByOwner(p.getUniqueId());
                if (pShooter != null && !pShooter.isInside(proj.getLocation())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (!eventoIniciado) return;
        Entity ent = e.getEntity();
        if (ent instanceof Player || ent instanceof TextDisplay || ent instanceof ArmorStand || ent instanceof Painting || ent instanceof ItemFrame) return;

        Location loc = e.getLocation();
        if (isInsideArenaFast(loc)) {
            BuildBattleParcel p = getParcelByLocationFast(loc);
            if (p != null) {
                if (p.getEntityCount() >= 30) {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        if (!eventoIniciado) return;
        Player p = e.getPlayer();
        if (isInsideArenaFast(p.getLocation()) && e.isFlying()) {
            if (p.getAllowFlight()) {
                e.setCancelled(false);
                p.setFlying(true);
            } else {
                e.setCancelled(true);
                p.setFlying(false);
            }
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!eventoIniciado) return;
        if (e.getMessage().toLowerCase().startsWith("/floor")) {
            e.setCancelled(true);
            Player p = e.getPlayer();

            if (!participantes.contains(p.getName())) {
                p.sendMessage(ChatColor.RED + "Solo los constructores pueden usar este comando.");
                return;
            }
            if (!fase.equals("Construccion")) {
                p.sendMessage(ChatColor.RED + "Solo puedes cambiar el suelo durante la fase de construcción.");
                return;
            }
            if (isEliminatedInTieBreaker(p)) {
                p.sendMessage(ChatColor.RED + "Estás eliminado, no puedes cambiar el suelo.");
                return;
            }

            BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
            if (parcel == null || !parcel.isInsideFast(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                p.sendMessage(ChatColor.RED + "Debes estar dentro de tu parcela para cambiar el suelo.");
                return;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || !hand.getType().isBlock() || hand.getType() == Material.AIR) {
                p.sendMessage(ChatColor.RED + "Debes tener un bloque válido en tu mano principal para usar /floor.");
                return;
            }

            parcel.fillFloor(hand.getType());
            p.sendMessage(cPrimary + "¡Suelo de tu parcela actualizado a " + cSecondary + hand.getType().name() + cPrimary + "!");
            p.playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 1f);
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (e.getEntity() instanceof Player p && isInsideArenaFast(p.getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!isInsideArenaFast(loc)) return;
        Player p = e.getPlayer();

        if (admins.contains(p.getName()) || votantes.contains(p.getName())) return;

        if (!eventoIniciado) { e.setCancelled(true); return; }
        if (isEliminatedInTieBreaker(p)) { e.setCancelled(true); return; }

        if (fase.equals("Construccion") && participantes.contains(p.getName())) {
            BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
            if (parcel != null && parcel.isInside(loc)) return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!isInsideArenaFast(loc)) return;
        Player p = e.getPlayer();

        if (admins.contains(p.getName()) || votantes.contains(p.getName())) return;

        if (!eventoIniciado) { e.setCancelled(true); return; }
        if (isEliminatedInTieBreaker(p)) { e.setCancelled(true); return; }

        if (fase.equals("Construccion") && participantes.contains(p.getName())) {
            BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
            if (parcel != null && parcel.isInside(loc)) return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!eventoIniciado) return;
        Location loc = e.getEntity().getLocation();

        if (e.getDamager() instanceof Player p) {
            if (admins.contains(p.getName()) || votantes.contains(p.getName())) return;
            if (isEliminatedInTieBreaker(p)) { e.setCancelled(true); return; }

            if (participantes.contains(p.getName())) {
                BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                if (parcel == null || !parcel.isInsideFast(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) e.setCancelled(true);
            } else if (isInsideArenaFast(loc)) e.setCancelled(true);

        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            if (admins.contains(p.getName()) || votantes.contains(p.getName())) return;
            if (isEliminatedInTieBreaker(p)) { e.setCancelled(true); return; }

            if (participantes.contains(p.getName())) {
                BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                if (parcel == null || !parcel.isInsideFast(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) e.setCancelled(true);
            } else if (isInsideArenaFast(loc)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobDamageExplosion(EntityDamageEvent e) {
        if (!eventoIniciado) return;
        if (e.getEntity() instanceof Player) return;
        if (isInsideArenaFast(e.getEntity().getLocation())) {
            if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (!eventoIniciado) return;
        Player p = e.getPlayer();
        if (p == null) return;

        if (admins.contains(p.getName()) || votantes.contains(p.getName())) return;
        if (isEliminatedInTieBreaker(p)) { e.setCancelled(true); return; }

        if (participantes.contains(p.getName())) {
            BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
            if (parcel == null || !parcel.isInsideFast(e.getEntity().getLocation().getBlockX(), e.getEntity().getLocation().getBlockY(), e.getEntity().getLocation().getBlockZ())) e.setCancelled(true);
        } else if (isInsideArenaFast(e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(block -> isInsideArenaFast(block.getLocation()));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(block -> isInsideArenaFast(block.getLocation()));
    }

    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            e.setCancelled(true);
        } else {
            for (Block b : e.getBlocks()) {
                if (isInsideArenaFast(b.getLocation())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            e.setCancelled(true);
        } else {
            for (Block b : e.getBlocks()) {
                if (isInsideArenaFast(b.getLocation())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockDispense(org.bukkit.event.block.BlockDispenseEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            if (e.getCause() == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.SPREAD ||
                    e.getCause() == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.LAVA ||
                    e.getCause() == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent e) {
        if (!eventoIniciado) return;
        if (isInsideArenaFast(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!isInsideArenaFast(e.getLocation())) return;

        EntityType t = e.getEntityType();
        if (t == EntityType.ENDER_DRAGON || t == EntityType.WITHER || t == EntityType.ELDER_GUARDIAN) {
            e.setCancelled(true);
            return;
        }

        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            if (eventoIniciado) e.getEntity().setAI(false);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Monster mob && isInsideArenaFast(entity.getLocation())) mob.remove();
        }
    }

    public void eliminarMobsExistentes() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster mob && isInsideArenaFast(entity.getLocation())) mob.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!eventoIniciado) {
            restaurarTeamOriginal(p.getName());
            restoreInventorySafe(p);
            if (isInsideArenaFast(p.getLocation())) {
                p.teleportAsync(p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.ADVENTURE) p.setGameMode(GameMode.SURVIVAL);
        } else {
            if (participantes.contains(p.getName())) aplicarTeam(p);
            if (isInsideArenaFast(p.getLocation())) {
                List<BuildBattleParcel> validParcelsPrecalc = parcelas.values().stream().filter(pa -> pa.getOwnerUUID() != null).sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints())).collect(Collectors.toList());
                actualizarScoreboardIndividual(p, validParcelsPrecalc);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (participantes.contains(p.getName()) || votantes.contains(p.getName()) || admins.contains(p.getName())) {
            restaurarTeamOriginal(p.getName());
        }
    }

    private void darItemsVotacion(Player p) {
        p.getInventory().clear();
        p.getInventory().setItem(0, createItem(Material.LIME_CONCRETE, "§a§lOriginalidad"));
        p.getInventory().setItem(2, createItem(Material.CYAN_CONCRETE, "§b§lComplejidad"));
        p.getInventory().setItem(4, createItem(Material.ORANGE_CONCRETE, "§6§lImpacto Visual"));
        p.getInventory().setItem(8, createItem(Material.CHEST, "§e§lParcelas"));
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (hand != null && hand.getType() == Material.STICK && hand.hasItemMeta() && "§d§lVarita de Parcelas".equals(hand.getItemMeta().getDisplayName())) {
            e.setCancelled(true);
            Block b = e.getClickedBlock();
            if (b != null) {
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    pos1Map.put(p.getUniqueId(), b.getLocation());
                    p.sendMessage(cSecondary + "Posición 1 fijada.");
                } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    pos2Map.put(p.getUniqueId(), b.getLocation());
                    p.sendMessage(cSecondary + "Posición 2 fijada.");
                }
            }
            return;
        }

        if (isEliminatedInTieBreaker(p)) {
            if (hand != null && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName() && hand.getItemMeta().getDisplayName().contains("Parcelas")) {
                e.setCancelled(true);
                if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    abrirMenuParcelas(p);
                }
                return;
            }
            e.setCancelled(true);
            return;
        }

        if (eventoIniciado && participantes.contains(p.getName()) && e.getClickedBlock() != null) {
            if (isInsideArenaFast(p.getLocation())) {
                if (fase.equals("Construccion")) {
                    BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                    if (parcel == null || !parcel.isInsideFast(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ())) {
                        e.setCancelled(true);
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }

        if (!eventoIniciado) return;

        boolean isAdOrVot = votantes.contains(p.getName()) || admins.contains(p.getName());

        if (fase.equals("Votacion") || fase.equals("Construccion") || isAdOrVot) {
            if (hand != null && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
                String name = hand.getItemMeta().getDisplayName();

                if (name.contains("Parcelas") || name.contains("Originalidad") || name.contains("Complejidad") || name.contains("Impacto Visual")) {
                    e.setCancelled(true);

                    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK ||
                            e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {

                        if (name.contains("Parcelas")) {
                            abrirMenuParcelas(p);
                        } else if (isAdOrVot) {
                            Integer idParcela = jugadorViendoParcelaId.get(p.getUniqueId());
                            if (idParcela == null || !parcelas.containsKey(idParcela)) {
                                p.sendMessage(ChatColor.RED + "Debes estar dentro de una parcela para votar.");
                                return;
                            }
                            BuildBattleParcel parcel = parcelas.get(idParcela);
                            if (parcel.getOwnerUUID() == null) {
                                p.sendMessage(ChatColor.RED + "Esta parcela está vacía.");
                                return;
                            }
                            jugadorVotandoCategoria.put(p.getUniqueId(), ChatColor.stripColor(name));
                            abrirMenuPuntos(p, name);
                        }
                    }
                }
            }
        }
    }

    private void abrirMenuParcelas(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Menú de Parcelas");
        int totalVoters = votantes.size();

        for (BuildBattleParcel parcel : parcelas.values()) {
            if (parcel.getOwnerUUID() != null) {
                if (isTieBreaker && !empatadosNames.contains(parcel.getOwnerName())) continue;

                boolean enVotacion = false;
                for(String vName : votantes) {
                    Player v = Bukkit.getPlayer(vName);
                    if(v != null && parcel.isInsideFast(v.getLocation().getBlockX(), v.getLocation().getBlockY(), v.getLocation().getBlockZ())) { enVotacion = true; break; }
                }
                if (!enVotacion) {
                    for(String aName : admins) {
                        Player a = Bukkit.getPlayer(aName);
                        if(a != null && parcel.isInsideFast(a.getLocation().getBlockX(), a.getLocation().getBlockY(), a.getLocation().getBlockZ())) { enVotacion = true; break; }
                    }
                }

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(parcel.getOwnerUUID()));
                meta.setDisplayName(cSecondary + "Parcela de " + parcel.getOwnerName());

                List<String> lore = new ArrayList<>();
                lore.add("§7Puntos Totales: §e" + parcel.getDisplayPoints());

                if (fase.equals("Construccion")) {
                    lore.add("§b[En Construcción]");
                } else {
                    boolean allVoted = parcel.hasVotedAll(totalVoters);
                    lore.add(allVoted ? "§a[VOTADO]" : "§c[NO VOTADO]");
                    if (enVotacion) lore.add("§6[EN VOTACIÓN]");
                }

                lore.add("§8Click para Teletransportarte");
                meta.setLore(lore);
                head.setItemMeta(meta);

                inv.addItem(head);
            }
        }
        p.openInventory(inv);
    }

    private void abrirMenuPuntos(Player p, String tituloCategoria) {
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        String categoriaLimpia = ChatColor.stripColor(tituloCategoria);

        String hexColorPrincipal = "#ae52e3";
        String hexColorAcento = "#F977F9";
        String articulo = "la";

        if (categoriaLimpia.contains("Originalidad")) {
            hexColorPrincipal = "#6cd96c";
            hexColorAcento = "#98f298";
            articulo = "la";
        } else if (categoriaLimpia.contains("Complejidad")) {
            hexColorPrincipal = "#4ab0b0";
            hexColorAcento = "#7ae0e0";
            articulo = "la";
        } else if (categoriaLimpia.contains("Impacto Visual")) {
            hexColorPrincipal = "#e39a44";
            hexColorAcento = "#ffbe73";
            articulo = "el";
        }

        net.kyori.adventure.text.Component title = mm.deserialize("<" + hexColorAcento + "><bold>⭐ Votación: " + categoriaLimpia + " ⭐</bold>");

        String bodyString = "<" + hexColorPrincipal + ">Estás a punto de evaluar " + articulo + " <" + hexColorAcento + "><bold>" + categoriaLimpia + "</bold> <" + hexColorPrincipal + ">de esta construcción.<br><br>" +
                "<" + hexColorPrincipal + ">Escribe un puntaje del <white><bold>1 al 10</bold></white> en el recuadro de abajo.<br>" +
                "<gray><i>(Valores fuera de este rango o letras serán ignorados)</i></gray>";

        net.kyori.adventure.text.Component bodyText = mm.deserialize(bodyString);

        io.papermc.paper.registry.data.dialog.input.DialogInput input = io.papermc.paper.registry.data.dialog.input.DialogInput.text(
                "puntos",
                200,
                net.kyori.adventure.text.Component.text("Ejemplo: 10"),
                true,
                "",
                2,
                null
        );

        io.papermc.paper.registry.data.dialog.DialogBase base = io.papermc.paper.registry.data.dialog.DialogBase.builder(title)
                .body(List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(bodyText)))
                .inputs(List.of(input))
                .canCloseWithEscape(true)
                .build();

        net.kyori.adventure.key.Key submitKey = net.kyori.adventure.key.Key.key("buildbattle", "votar");
        io.papermc.paper.registry.data.dialog.ActionButton submit = io.papermc.paper.registry.data.dialog.ActionButton.builder(mm.deserialize("<" + hexColorPrincipal + ">✔ Enviar Voto"))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick(submitKey, null))
                .build();

        io.papermc.paper.dialog.Dialog dialog = io.papermc.paper.dialog.Dialog.create(b -> b.empty().base(base).type(io.papermc.paper.registry.data.dialog.type.DialogType.notice(submit)));
        p.showDialog(dialog);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!eventoIniciado) return;

        String title = e.getView().getTitle();
        if (title != null && title.contains("Menú de Parcelas")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                    String ownerName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace("Parcela de ", "");
                    BuildBattleParcel parcel = parcelas.values().stream().filter(par -> ownerName.equals(par.getOwnerName())).findFirst().orElse(null);
                    if (parcel != null) {
                        Location tpLoc = parcel.getCenterBottom().clone().add(0, 30, 0);
                        tpLoc.setPitch(90f);
                        p.teleport(tpLoc);
                        p.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCustomDialogClick(io.papermc.paper.event.player.PlayerCustomClickEvent e) {
        String keyStr = e.getIdentifier().asString();
        if (keyStr.equals("buildbattle:votar")) {
            Player p = null;
            if (e.getCommonConnection() instanceof Player pl) p = pl;
            else if (e.getCommonConnection() instanceof io.papermc.paper.connection.PlayerGameConnection pgc) p = pgc.getPlayer();

            if (p == null || !eventoIniciado) return;

            var view = e.getDialogResponseView();
            if (view == null) return;

            String puntosStr = view.getText("puntos");
            if (puntosStr == null || puntosStr.trim().isEmpty()) {
                p.sendMessage(ChatColor.RED + "No ingresaste ningún puntaje.");
                return;
            }

            int puntos;
            try {
                puntos = Integer.parseInt(puntosStr.trim());
            } catch (NumberFormatException ex) {
                p.sendMessage(ChatColor.RED + "Error: '" + puntosStr.trim() + "' no es un número válido.");
                return;
            }

            if (puntos < 1 || puntos > 10) {
                p.sendMessage(ChatColor.RED + "Error: El puntaje ingresado (" + puntos + ") debe ser entre 1 y 10.");
                return;
            }

            String categoriaVotada = jugadorVotandoCategoria.get(p.getUniqueId());
            Integer idParcela = jugadorViendoParcelaId.get(p.getUniqueId());

            if (categoriaVotada != null && idParcela != null && parcelas.containsKey(idParcela)) {
                BuildBattleParcel parcel = parcelas.get(idParcela);

                if (categoriaVotada.contains("Originalidad")) parcel.setVoteOriginality(p.getUniqueId(), puntos);
                else if (categoriaVotada.contains("Complejidad")) parcel.setVoteComplexity(p.getUniqueId(), puntos);
                else if (categoriaVotada.contains("Impacto Visual")) parcel.setVoteVisual(p.getUniqueId(), puntos);

                p.sendMessage(cSecondary + "Has dado §f§l" + puntos + " " + cSecondary + "puntos en §l" + ChatColor.stripColor(categoriaVotada) + " " + cSecondary + "a " + parcel.getOwnerName());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                List<BuildBattleParcel> validParcelsPrecalc = parcelas.values().stream()
                        .filter(pa -> pa.getOwnerUUID() != null)
                        .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                        .collect(Collectors.toList());
                actualizarScoreboardIndividual(p, validParcelsPrecalc);
            }
        }
    }

    private ItemStack createItem(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(name);
        i.setItemMeta(meta);
        return i;
    }

    private void darItemCofreConstruccion(Player p) {
        p.getInventory().clear();
        p.getInventory().setItem(8, createItem(Material.CHEST, "§e§lParcelas"));
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    private BuildBattleParcel getEmptyParcel() {
        return parcelas.values().stream().filter(p -> p.getOwnerUUID() == null).findFirst().orElse(null);
    }

    private BuildBattleParcel getParcelByOwner(UUID uuid) {
        return parcelas.values().stream().filter(p -> uuid.equals(p.getOwnerUUID())).findFirst().orElse(null);
    }

    private void broadcastEventZona(String msg) {
        for(UUID id : playersInArena) {
            Player p = Bukkit.getPlayer(id);
            if(p != null) p.sendMessage(" " + msg);
        }
    }

    private void inicializarTeam() {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = mainBoard.getTeam(TeamType.BUILDBATTLE.getId());
        if (t == null) t = mainBoard.registerNewTeam(TeamType.BUILDBATTLE.getId());
        t.setPrefix(TeamType.BUILDBATTLE.getChatPrefix());
        t.setColor(TeamType.BUILDBATTLE.getBukkitColor());
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    private void aplicarTeam(Player p) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team oldTeam = mainBoard.getEntryTeam(p.getName());

        if (oldTeam != null) {
            originalTeams.put(p.getName(), oldTeam.getName());
        } else {
            originalTeams.put(p.getName(), "");
        }

        Team hpTeam = mainBoard.getTeam(TeamType.BUILDBATTLE.getId());
        if (hpTeam != null) hpTeam.addEntry(p.getName());

        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }

    private void restaurarTeamOriginal(String playerName) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team hpTeam = mainBoard.getTeam(TeamType.BUILDBATTLE.getId());
        if (hpTeam != null && hpTeam.hasEntry(playerName)) {
            hpTeam.removeEntry(playerName);
        }

        if (originalTeams.containsKey(playerName)) {
            String oldTeamName = originalTeams.get(playerName);
            if (oldTeamName != null && !oldTeamName.isEmpty()) {
                Team oldTeam = mainBoard.getTeam(oldTeamName);
                if (oldTeam != null) oldTeam.addEntry(playerName);
            }
            originalTeams.remove(playerName);
        }

        Player p = Bukkit.getPlayer(playerName);
        if (p != null) p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    private void actualizarScoreboards() {
        if (!eventoIniciado) return;

        List<BuildBattleParcel> validParcelsPrecalc = parcelas.values().stream()
                .filter(pa -> pa.getOwnerUUID() != null)
                .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                .collect(Collectors.toList());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playersInArena.contains(p.getUniqueId())) {
                actualizarScoreboardIndividual(p, validParcelsPrecalc);
            } else {
                if (!p.getScoreboard().equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }
    }

    private void updateScoreboardLine(Scoreboard board, Objective obj, int score, String text) {
        String invisibleEntry = "§" + (score % 10) + "§" + (score / 10) + "§r";

        Team lineTeam = board.getTeam("line_" + score);
        if (lineTeam == null) {
            lineTeam = board.registerNewTeam("line_" + score);
            lineTeam.addEntry(invisibleEntry);
        }

        lineTeam.setPrefix(text);
        obj.getScore(invisibleEntry).setScore(score);
    }

    private void clearUnusedScoreboardLines(Scoreboard board, int maxUsedScore) {
        for (int i = maxUsedScore + 1; i <= 20; i++) {
            String invisibleEntry = "§" + (i % 10) + "§" + (i / 10) + "§r";
            board.resetScores(invisibleEntry);
        }
    }

    private void actualizarScoreboardIndividual(Player p) {
        List<BuildBattleParcel> validParcelsPrecalc = parcelas.values().stream()
                .filter(pa -> pa.getOwnerUUID() != null)
                .sorted((p1, p2) -> Double.compare(p2.getSortPoints(), p1.getSortPoints()))
                .collect(Collectors.toList());
        actualizarScoreboardIndividual(p, validParcelsPrecalc);
    }

    private void actualizarScoreboardIndividual(Player p, List<BuildBattleParcel> validParcels) {
        Scoreboard board = p.getScoreboard();

        if (board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(board);
            Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team mainTeam : mainBoard.getTeams()) {
                Team boardTeam = board.getTeam(mainTeam.getName());
                if (boardTeam == null) boardTeam = board.registerNewTeam(mainTeam.getName());
                boardTeam.setPrefix(mainTeam.getPrefix());
                boardTeam.setSuffix(mainTeam.getSuffix());
                try { boardTeam.setColor(mainTeam.getColor()); } catch (Exception ignored) {}
                boardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, mainTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));
                for (String entry : mainTeam.getEntries()) if (!boardTeam.hasEntry(entry)) boardTeam.addEntry(entry);
            }
        }

        Objective obj = board.getObjective("bb");
        if (obj == null) {
            obj = board.registerNewObjective("bb", "dummy", cPrimary + "§lQUASO BUILD BATTLE");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        if (isTieBreaker && fase.equals("Construccion")) {
            updateScoreboardLine(board, obj, 10, "§1");
            updateScoreboardLine(board, obj, 9, "§fFase: §c§lDESEMPATE");
            updateScoreboardLine(board, obj, 8, "§2");
            updateScoreboardLine(board, obj, 7, "§fCategoría:");
            updateScoreboardLine(board, obj, 6, "  " + cCat + categoria);
            updateScoreboardLine(board, obj, 5, "§3");
            updateScoreboardLine(board, obj, 4, cSecondary + "§lEn Desempate:");

            int score = 3;
            for (String emp : empatadosNames) {
                updateScoreboardLine(board, obj, score--, "§7- §f" + emp);
            }
            clearUnusedScoreboardLines(board, 10);

        } else if (fase.equals("Construccion")) {
            BuildBattleParcel miP = getParcelByOwner(p.getUniqueId());
            String numParcela = (miP != null) ? String.valueOf(miP.getId()) : "N/A";

            updateScoreboardLine(board, obj, 8, "§1");
            updateScoreboardLine(board, obj, 7, "§fFase: §a§lCONSTRUCCIÓN");

            if (modoJuego.equalsIgnoreCase("TORNEO")) {
                updateScoreboardLine(board, obj, 6, "§fRonda: §e" + rondaActual + "§8/§e" + rondasMaximas);
            } else {
                updateScoreboardLine(board, obj, 6, "§2");
            }

            updateScoreboardLine(board, obj, 5, "§fCategoría:");
            updateScoreboardLine(board, obj, 4, "  " + cCat + categoria);
            updateScoreboardLine(board, obj, 3, "§3");
            updateScoreboardLine(board, obj, 2, "§fTu Parcela: §e#" + numParcela);
            updateScoreboardLine(board, obj, 1, "§fJugadores: §d" + participantes.size());

            clearUnusedScoreboardLines(board, 8);

        } else if (fase.equals("Votacion")) {
            updateScoreboardLine(board, obj, 17, "§1");

            if (modoJuego.equalsIgnoreCase("TORNEO")) {
                updateScoreboardLine(board, obj, 16, "§fFase: §6" + fase + " §8(§e" + rondaActual + "§8)");
            } else {
                updateScoreboardLine(board, obj, 16, "§fFase: §6" + fase);
            }

            updateScoreboardLine(board, obj, 15, "§fCategoría: " + cCat + categoria);
            updateScoreboardLine(board, obj, 14, "§r");

            updateScoreboardLine(board, obj, 13, cSecondary + "§lTop Actual:");
            int startScore = 12;

            for (int i = 0; i < Math.min(3, validParcels.size()); i++) {
                BuildBattleParcel bp = validParcels.get(i);
                String pName = bp.getOwnerName();
                if (pName.length() > 10) pName = pName.substring(0, 10) + "..";
                updateScoreboardLine(board, obj, startScore--, "§7" + (i+1) + ". §f" + pName + " " + cPrimary + bp.getDisplayPoints());
            }

            updateScoreboardLine(board, obj, startScore--, "§r ");

            int myRank = -1;
            String myPointsDisplay = "0";
            for (int i = 0; i < validParcels.size(); i++) {
                if (validParcels.get(i).getOwnerUUID().equals(p.getUniqueId())) {
                    myRank = i + 1;
                    myPointsDisplay = validParcels.get(i).getDisplayPoints();
                }
            }

            String myRankStr = myRank != -1 ? String.valueOf(myRank) : "N/A";
            String myPointsStr = myRank != -1 ? " §7(" + myPointsDisplay + " pts)" : "";

            updateScoreboardLine(board, obj, startScore--, cSecondary + "§lMi top: §f" + myRankStr + myPointsStr);
            updateScoreboardLine(board, obj, startScore--, "§r  ");

            while(startScore > 0) {
                board.resetScores("§" + (startScore % 10) + "§" + (startScore / 10) + "§r");
                startScore--;
            }

            clearUnusedScoreboardLines(board, 17);

        } else {
            String displayFase = fase.equals("Desempate_Lobby") ? "Desempate" : fase;

            if (modoJuego.equalsIgnoreCase("TORNEO") && !fase.equals("Desempate_Lobby")) {
                displayFase += " (Ronda " + rondaActual + "/" + rondasMaximas + ")";
            }

            updateScoreboardLine(board, obj, 6, "§1");
            updateScoreboardLine(board, obj, 5, "§fFase: §6" + displayFase);
            updateScoreboardLine(board, obj, 4, "§fCategoría: " + cCat + categoria);
            updateScoreboardLine(board, obj, 3, "§2");

            clearUnusedScoreboardLines(board, 6);
        }
    }

    private void cancelarTareasActivas() {
        for (BukkitTask t : tareasActivas) if(t != null && !t.isCancelled()) t.cancel();
        tareasActivas.clear();
    }

    public void addVotador(String n) {
        if (!votantes.contains(n)) {
            votantes.add(n);
            Player p = Bukkit.getPlayerExact(n);
            if (p != null && eventoIniciado) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
                if (tpRealizado) {
                    backupInventorySafe(p);
                    if (fase.equals("Votacion")) {
                        darItemsVotacion(p);
                    } else if (fase.equals("Construccion")) {
                        darItemCofreConstruccion(p);
                    }
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                }
            }
        }
    }

    public void removeVotador(String n) {
        votantes.remove(n);
        Player p = Bukkit.getPlayerExact(n);
        if (p != null) {
            if (isPlayerInEvent(n)) {
                p.getInventory().clear();
                if (admins.contains(n)) {
                    if (fase.equals("Votacion")) darItemsVotacion(p);
                    else if (fase.equals("Construccion")) darItemCofreConstruccion(p);
                    p.setGameMode(GameMode.ADVENTURE);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                } else if (participantes.contains(n)) {
                    if (fase.equals("Construccion")) p.setGameMode(GameMode.CREATIVE);
                }
            } else {
                restoreInventorySafe(p);
                restaurarTeamOriginal(n);
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                Location spawn = p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
                p.teleport(spawn);
            }
        }
    }

    public void addAdmin(String n) {
        if (!admins.contains(n)) {
            admins.add(n);
            Player p = Bukkit.getPlayerExact(n);
            if (p != null && eventoIniciado) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
                if (tpRealizado) {
                    backupInventorySafe(p);
                    if (fase.equals("Votacion")) {
                        darItemsVotacion(p);
                    } else if (fase.equals("Construccion")) {
                        darItemCofreConstruccion(p);
                    }
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                }
            }
        }
    }

    public void removeAdmin(String n) {
        admins.remove(n);
        Player p = Bukkit.getPlayerExact(n);
        if (p != null) {
            if (isPlayerInEvent(n)) {
                p.getInventory().clear();
                if (votantes.contains(n)) {
                    if (fase.equals("Votacion")) darItemsVotacion(p);
                    else if (fase.equals("Construccion")) darItemCofreConstruccion(p);
                    p.setGameMode(GameMode.ADVENTURE);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                } else if (participantes.contains(n)) {
                    if (fase.equals("Construccion")) p.setGameMode(GameMode.CREATIVE);
                }
            } else {
                restoreInventorySafe(p);
                restaurarTeamOriginal(n);
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                Location spawn = p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
                p.teleport(spawn);
            }
        }
    }

    public void addPlayerEnMedio(String name) {
        if (!participantes.contains(name)) {
            participantes.add(name);
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                aplicarTeam(p);

                if (tpRealizado) {
                    backupInventorySafe(p);

                    BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
                    if (parcel == null) parcel = getEmptyParcel();

                    if (parcel != null) {
                        parcel.setOwner(p.getUniqueId(), p.getName());
                        p.teleport(parcel.getCenterBottom());
                        if (fase.equals("Construccion")) {
                            p.setGameMode(GameMode.CREATIVE);
                        } else if (fase.equals("Votacion")) {
                            p.setGameMode(GameMode.ADVENTURE);
                            p.setAllowFlight(true);
                            p.setFlying(true);
                            p.getInventory().setItem(8, createItem(Material.CHEST, "§e§lParcelas"));
                            parcel.updateTextDisplay();
                        } else {
                            p.setGameMode(GameMode.CREATIVE);
                        }
                    } else {
                        p.sendMessage(cSecondary + "No hay más parcelas disponibles. Pasas a espectador.");
                        p.setGameMode(GameMode.SPECTATOR);
                        Location center = centroMapa != null ? centroMapa.clone().add(0, 2, 0) : p.getWorld().getSpawnLocation();
                        p.teleport(center);
                    }
                } else {
                    p.sendMessage(cPrimary + "Añadido al evento. Espera el teletransporte general.");
                }

                if (isInsideArenaFast(p.getLocation())) {
                    actualizarScoreboardIndividual(p);
                } else {
                    actualizarScoreboards();
                }
            }
        }
    }

    public void removePlayerEnMedio(String name) {
        participantes.remove(name);
        Player p = Bukkit.getPlayerExact(name);

        if (p != null) {
            BuildBattleParcel parcel = getParcelByOwner(p.getUniqueId());
            if (parcel != null) {
                parcel.clearParcel();
                parcel.resetOwnerAndPoints();
            }

            if (isPlayerInEvent(name)) {
                p.getInventory().clear();
                if (fase.equals("Votacion")) darItemsVotacion(p);
                else if (fase.equals("Construccion")) darItemCofreConstruccion(p);
                p.setGameMode(GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(true);
                p.teleport(centroMapa != null ? centroMapa.clone().add(0, 2, 0) : p.getWorld().getSpawnLocation());
            } else {
                restoreInventorySafe(p);
                restaurarTeamOriginal(name);
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
                p.setFlying(false);

                Location spawn = p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
                p.teleport(spawn);

                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                actualizarScoreboards();
            }
        }
    }

    private void sendJsonMessage(Player p, String json) {
        p.sendMessage(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json));
    }

    private void teleportMagico(Player p, Location loc) {
        String comando = String.format(Locale.US, "magictp %s %.2f %.2f %.2f", p.getName(), loc.getX(), loc.getY(), loc.getZ());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
    }

    public void setCategoria(String cat) { this.categoria = cat; actualizarScoreboards(); }
    public void setEventInventoryManager(EventInventoryManager manager) { this.eventInventoryManager = manager; }
}