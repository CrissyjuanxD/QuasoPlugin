package Events.MissionSystem;

import Handlers.DatabaseManager;
import Handlers.Teams.TeamType;
import TitleListener.MisionAnimation;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class MissionHandler implements Listener {
    // Con 30 misiones completas pasa a DinoNugget+
    private static final int MISSIONS_FOR_PLUS = 30;
    private static final NamespacedKey TOKEN_KEY = new NamespacedKey("quasoplugin", "ficha_mision");

    private static final List<BiFunction<JavaPlugin, MissionHandler, Mission>> MISSION_LIST = List.of(
            Mission1::new, Mission2::new, Mission3::new, Mission4::new, Mission5::new,
            Mission6::new, Mission7::new, Mission8::new, Mission9::new, Mission10::new,
            Mission11::new, Mission12::new, Mission13::new, Mission14::new, Mission15::new,
            Mission16::new, Mission17::new, Mission18::new, Mission19::new, Mission20::new,
            Mission21::new, Mission22::new, Mission23::new, Mission24::new, Mission25::new,
            Mission26::new, Mission27::new, Mission28::new, Mission29::new, Mission30::new,
            Mission31::new, Mission32::new, Mission33::new, Mission34::new, Mission35::new,
            Mission36::new, Mission37::new, Mission38::new, Mission39::new, Mission40::new,
            Mission41::new, Mission42::new, Mission43::new, Mission44::new, Mission45::new,
            Mission46::new, Mission47::new, Mission48::new, Mission49::new, Mission50::new,
            Mission51::new, Mission52::new, Mission53::new, Mission54::new, Mission55::new,
            Mission56::new, Mission57::new, Mission58::new, Mission59::new, Mission60::new,
            Mission61::new, Mission62::new, Mission63::new, Mission64::new, Mission65::new,
            Mission66::new, Mission67::new, Mission68::new, Mission69::new, Mission70::new,
            Mission71::new, Mission72::new, Mission73::new, Mission74::new, Mission75::new,
            Mission76::new, Mission77::new, Mission78::new, Mission79::new, Mission80::new,
            Mission81::new, Mission82::new, Mission83::new, Mission84::new, Mission85::new,
            Mission86::new, Mission87::new, Mission88::new, Mission89::new, Mission90::new,
            Mission91::new, Mission92::new, Mission93::new, Mission94::new, Mission95::new,
            Mission96::new, Mission97::new, Mission98::new, Mission99::new, Mission100::new,
            Mission101::new, Mission102::new, Mission103::new, Mission104::new, Mission105::new,
            Mission106::new, Mission107::new, Mission108::new, Mission109::new, Mission110::new,
            Mission111::new, Mission112::new, Mission113::new, Mission114::new, Mission115::new,
            Mission116::new, Mission117::new, Mission118::new, Mission119::new, Mission120::new,
            Mission121::new, Mission122::new, Mission123::new, Mission124::new, Mission125::new,
            Mission126::new, Mission127::new, Mission128::new, Mission129::new, Mission130::new,
            Mission131::new, Mission132::new, Mission133::new, Mission134::new, Mission135::new,
            Mission136::new, Mission137::new, Mission138::new, Mission139::new, Mission140::new
    );

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final Map<UUID, Map<Integer, MissionData>> playerCache = new ConcurrentHashMap<>();
    private final Map<Integer, Mission> missions = new TreeMap<>();
    private final Set<Integer> globalActiveMissions = ConcurrentHashMap.newKeySet();
    private final MisionAnimation ruletaAnimation;
    private long tickSecond = 0;

    public MissionHandler(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.ruletaAnimation = new MisionAnimation(plugin);

        registerMissions();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<Integer> dbActiveMissions = dbManager.getGlobalActiveMissions();
            globalActiveMissions.addAll(dbActiveMissions);
            plugin.getLogger().info("Se han restaurado " + globalActiveMissions.size() + " misiones globales activas desde la BD.");
        });

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::autoSaveAll, 3600L, 3600L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickMissions, 20L, 20L);
        Bukkit.getScheduler().runTask(plugin, this::validateRewards);
    }

    private void registerMissions() {
        for (BiFunction<JavaPlugin, MissionHandler, Mission> factory : MISSION_LIST) {
            Mission mission = factory.apply(plugin, this);
            missions.put(mission.getMissionNumber(), mission);
        }
    }

    // Avisa en consola si alguna recompensa apunta a un item que no existe
    private void validateRewards() {
        for (Mission mission : missions.values()) {
            List<ItemStack> rewards = mission.getRewards();
            if (rewards.size() != 27 || rewards.contains(null)) {
                plugin.getLogger().warning("La recompensa de la misión " + mission.getMissionNumber() + " tiene items vacíos.");
            }
        }
    }

    public void registerAllMissionListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        MissionUtils.init(plugin);

        for (Mission mission : missions.values()) {
            if (mission instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) mission, plugin);
            }
        }
        plugin.getLogger().info("Sistema de misiones: " + missions.size() + " misiones registradas.");
    }

    // Una sola tarea por segundo para todas las misiones que miden tiempo, distancia o inventario
    private void tickMissions() {
        tickSecond++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerCache.containsKey(player.getUniqueId())) continue;
            for (int id : globalActiveMissions) {
                if (!(missions.get(id) instanceof BaseMission mission)) continue;
                try {
                    mission.runTick(player, tickSecond);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error en la misión " + id + ": " + e.getMessage());
                }
            }
        }
    }

    // Carga las misiones del jugador de la base de datos en async y las sincroniza con las activas globales
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, MissionData> data = dbManager.loadPlayerMissions(uuid);

            for (int missionId : globalActiveMissions) {
                if (!data.containsKey(missionId)) {
                    MissionData newMission = new MissionData(true, false, false, "{}");
                    newMission.setDirty(true);
                    data.put(missionId, newMission);
                } else {
                    MissionData existingMission = data.get(missionId);
                    if (!existingMission.isActive()) {
                        existingMission.setActive(true);
                        existingMission.setDirty(true);
                    }
                }
            }

            for (Map.Entry<Integer, MissionData> entry : data.entrySet()) {
                if (!globalActiveMissions.contains(entry.getKey())) {
                    if (entry.getValue().isActive()) {
                        entry.getValue().setActive(false);
                        entry.getValue().setDirty(true);
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    playerCache.put(uuid, data);
                }
            });
        });
    }

    // Al salir guarda solo las misiones que cambiaron
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        for (Mission mission : missions.values()) {
            if (mission instanceof BaseMission base) base.onQuit(player);
        }

        Map<Integer, MissionData> data = playerCache.get(uuid);
        if (data != null) {
            Map<Integer, MissionData> dirtyMissions = new HashMap<>();
            Map<Integer, String> missionNames = new HashMap<>();

            for (Map.Entry<Integer, MissionData> missionEntry : data.entrySet()) {
                if (missionEntry.getValue().isDirty()) {
                    dirtyMissions.put(missionEntry.getKey(), missionEntry.getValue());
                    missionEntry.getValue().setDirty(false);
                    missionNames.put(missionEntry.getKey(), plainName(missionEntry.getKey()));
                }
            }
            if (!dirtyMissions.isEmpty()) {
                dbManager.savePlayerMissionsBatchSync(uuid, player.getName(), dirtyMissions, missionNames);
            }
        }
        playerCache.remove(uuid);
    }

    // Si el jugador todavía no cargó devuelve una misión vacía e inactiva
    public MissionData getData(Player player, int missionId) {
        if (!playerCache.containsKey(player.getUniqueId())) {
            MissionData dummy = new MissionData();
            dummy.setActive(false);
            return dummy;
        }

        Map<Integer, MissionData> pData = playerCache.get(player.getUniqueId());
        MissionData data = pData.computeIfAbsent(missionId, k -> new MissionData());

        data.setActive(globalActiveMissions.contains(missionId));
        return data;
    }

    public void saveData(Player player, int missionId, MissionData data) {
        if (!playerCache.containsKey(player.getUniqueId())) {
            return;
        }

        Map<Integer, MissionData> pData = playerCache.get(player.getUniqueId());
        pData.put(missionId, data);
        data.setDirty(true);
    }

    // Guarda en la base de datos todo lo que quedó pendiente en caché (cada 3 minutos y al apagar)
    public void autoSaveAll() {
        for (Map.Entry<UUID, Map<Integer, MissionData>> entry : playerCache.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player != null ? player.getName() : "Unknown";

            Map<Integer, MissionData> dirtyMissions = new HashMap<>();
            Map<Integer, String> missionNames = new HashMap<>();

            for (Map.Entry<Integer, MissionData> missionEntry : entry.getValue().entrySet()) {
                if (missionEntry.getValue().isDirty()) {
                    dirtyMissions.put(missionEntry.getKey(), missionEntry.getValue());
                    missionEntry.getValue().setDirty(false);
                    missionNames.put(missionEntry.getKey(), plainName(missionEntry.getKey()));
                }
            }

            if (!dirtyMissions.isEmpty()) {
                dbManager.savePlayerMissionsBatchSync(uuid, playerName, dirtyMissions, missionNames);
            }
        }
    }

    public void forceSaveAllOnShutdown() {
        plugin.getLogger().info("Forzando guardado de todas las misiones (Apagado)...");
        autoSaveAll();
    }

    // "#52 Bajo presión" o "Extra #2 Lancero", como se ve en el menú y en los anuncios
    public String displayName(int missionNumber) {
        Mission mission = missions.get(missionNumber);
        return tag(missionNumber) + " " + (mission != null ? mission.getName() : "Misión Desconocida");
    }

    // Las extras se muestran con el número de la misión con la que salen
    public String tag(int missionNumber) {
        Mission mission = missions.get(missionNumber);
        return mission != null && mission.getParentMission() > 0 ? "Extra #" + mission.getParentMission() : "#" + missionNumber;
    }

    private String plainName(int missionNumber) {
        Mission mission = missions.get(missionNumber);
        return mission != null ? ChatColor.stripColor(mission.getName()) : "Unknown";
    }

    // Las misiones extra que se activan junto con esta
    public List<Integer> getExtras(int missionNumber) {
        List<Integer> extras = new ArrayList<>();
        for (Mission mission : missions.values()) {
            if (mission.getParentMission() == missionNumber) extras.add(mission.getMissionNumber());
        }
        return extras;
    }

    // Activa la misión (y sus extras) para todos y lo anuncia con la animación
    public void activateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        if (globalActiveMissions.contains(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " ya está activada globalmente.");
            return;
        }

        List<Integer> extras = new ArrayList<>();
        for (int extra : getExtras(missionNumber)) {
            if (!globalActiveMissions.contains(extra)) extras.add(extra);
        }

        setGlobalState(missionNumber, true);
        for (int extra : extras) setGlobalState(extra, true);

        if (extras.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " activada globalmente.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " activada globalmente junto con la extra " + extras + ".");
        }

        StringBuilder json = new StringBuilder("[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"},")
                .append("{\"text\":\"NUEVA MISIÓN DESBLOQUEADA\",\"bold\":true,\"color\":\"#FFA500\"},")
                .append(missionLine(missionNumber, "#dda0dd"));
        for (int extra : extras) {
            json.append(",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"},")
                    .append("{\"text\":\"MISIÓN EXTRA\",\"bold\":true,\"color\":\"#7FD4FF\"},")
                    .append(missionLine(extra, "#9fe2bf"));
        }
        json.append(",{\"text\":\"usa /misiones para abrir su interfaz o usa el item de Misiones\",\"color\":\"gray\"}]");

        for (Player online : Bukkit.getOnlinePlayers()) {
            ruletaAnimation.playAnimation(online, json.toString());
        }
    }

    private String missionLine(int missionNumber, String color) {
        String safeName = displayName(missionNumber).replace("\"", "\\\"");
        String safeDescription = missions.get(missionNumber).getDescription().replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"text\":\"\\n[\",\"color\":\"white\"}," +
                "{\"text\":\"" + safeName + "\",\"bold\":true,\"color\":\"" + color + "\"," +
                "\"hover_event\":{\"action\":\"show_text\",\"value\":{\"text\":\"" + safeDescription + "\",\"color\":\"gray\"}}}," +
                "{\"text\":\"]\\n\\n\",\"color\":\"white\"}";
    }

    private void setGlobalState(int missionNumber, boolean active) {
        if (active) globalActiveMissions.add(missionNumber);
        else globalActiveMissions.remove(missionNumber);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.setMissionGlobalState(missionNumber, active));

        for (Player online : Bukkit.getOnlinePlayers()) {
            MissionData data = getData(online, missionNumber);
            data.setActive(active);
            saveData(online, missionNumber, data);
        }
    }

    // Desactiva la misión y también sus extras
    public boolean deactivateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return false;
        }

        if (!globalActiveMissions.contains(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no está activa globalmente.");
            return false;
        }

        setGlobalState(missionNumber, false);
        List<Integer> extras = new ArrayList<>();
        for (int extra : getExtras(missionNumber)) {
            if (globalActiveMissions.contains(extra)) {
                setGlobalState(extra, false);
                extras.add(extra);
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " desactivada globalmente"
                + (extras.isEmpty() ? "." : " junto con la extra " + extras + "."));
        return true;
    }

    // Activa de una todas las que falten, con un solo anuncio
    public void activateAll(CommandSender sender) {
        int count = 0;
        for (int number : missions.keySet()) {
            if (globalActiveMissions.contains(number)) continue;
            setGlobalState(number, true);
            count++;
        }
        if (count == 0) {
            sender.sendMessage(ChatColor.RED + "Ya están todas las misiones activas.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Se activaron " + count + " misiones.");

        String json = "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"}," +
                "{\"text\":\"NUEVAS MISIONES DESBLOQUEADAS\",\"bold\":true,\"color\":\"#FFA500\"}," +
                "{\"text\":\"\\nSe abrieron " + count + " misiones de golpe.\\n\\n\",\"color\":\"#dda0dd\"}," +
                "{\"text\":\"usa /misiones para abrir su interfaz o usa el item de Misiones\",\"color\":\"gray\"}]";
        for (Player online : Bukkit.getOnlinePlayers()) {
            ruletaAnimation.playAnimation(online, json);
        }
    }

    public void deactivateAll(CommandSender sender) {
        List<Integer> active = new ArrayList<>(globalActiveMissions);
        for (int number : active) setGlobalState(number, false);
        sender.sendMessage(active.isEmpty() ? ChatColor.RED + "No hay misiones activas."
                : ChatColor.GREEN + "Se desactivaron " + active.size() + " misiones.");
    }

    // Desactiva todo y borra el progreso de todos en la base de datos; no tiene vuelta atrás
    public void resetAll(CommandSender sender) {
        globalActiveMissions.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (playerCache.containsKey(online.getUniqueId())) playerCache.put(online.getUniqueId(), new HashMap<>());
            for (Mission mission : missions.values()) {
                if (mission instanceof BaseMission base) base.onQuit(online);
            }
        }

        // Espera 2 segundos por si justo había un guardado automático en curso
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            dbManager.deleteAllMissionData();
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.GREEN
                    + "Misiones reiniciadas: todas desactivadas y la base de datos de misiones quedó vacía."));
        }, 40L);
    }

    public void initializePlayerMissionData(String playerName, int missionNumber) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            getData(p, missionNumber);
        }
    }

    // Marca la misión, le da la ficha para canjear la recompensa y lo anuncia a todos
    public boolean completeMission(String playerName, int missionNumber) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return false;

        MissionData data = getData(player, missionNumber);

        if (data.isCompleted()) return false;

        data.setCompleted(true);
        saveData(player, missionNumber, data);

        giveMissionToken(player, missionNumber);

        String missionName = displayName(missionNumber);

        String jsonMessage = String.format(
                "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#87ceeb\"}," +
                        "{\"text\":\" ha completado la misión \",\"color\":\"#7eaee4\"}," +
                        "{\"text\":\"[\",\"color\":\"white\"}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#dda0dd\"}," +
                        "{\"text\":\"]\\n\",\"color\":\"white\"}]",
                player.getName(),
                missionName.replace("\"", "\\\"")
        );

        String consoleMessage = player.getName() + " ha completado la misión [" + missionName + "]";
        plugin.getLogger().info(consoleMessage);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tellraw " + onlinePlayer.getName() + " " + jsonMessage);

                if (onlinePlayer.equals(player)) {
                    onlinePlayer.playSound(player.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                } else {
                    try {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.MASTER, 1f, 2.0f);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Error al reproducir sonido personalizado: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error al notificar al jugador: " + e.getMessage());
            }
        }

        long completedCount = getCompletedCount(player);

        player.sendMessage(ChatColor.GREEN + "Progreso Total: " + ChatColor.GOLD + completedCount +
                ChatColor.GREEN + " misiones completadas.");

        updateRole(player, completedCount, missionNumber);
        return true;
    }

    public long getCompletedCount(Player player) {
        Map<Integer, MissionData> data = playerCache.get(player.getUniqueId());
        if (data == null) return 0;
        return data.values().stream().filter(MissionData::isCompleted).count();
    }

    // DinoNugget+ con 30 misiones y DinoLeyenda con la 100; nunca baja a nadie de rango
    private void updateRole(Player player, long completedCount, int missionNumber) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team current = board.getEntryTeam(player.getName());
        String currentId = current != null ? current.getName() : null;

        TeamType target = null;
        if (missionNumber == 100 && (currentId == null || currentId.equals(TeamType.Z_MIEMBRO.getId())
                || currentId.equals(TeamType.Y_MIEMBRO.getId()))) {
            target = TeamType.X_LEYENDA;
        } else if (completedCount >= MISSIONS_FOR_PLUS && (currentId == null || currentId.equals(TeamType.Z_MIEMBRO.getId()))) {
            target = TeamType.Y_MIEMBRO;
        }
        if (target == null) return;

        Team team = board.getTeam(target.getId());
        if (team != null) {
            team.addEntry(player.getName());
            player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Ahora eres " + target.getChatPrefix().trim() + ChatColor.of("#FFCC99") + "!");
        }
    }

    public void giveMissionToken(Player player, int missionNumber) {
        ItemStack token = createMissionToken(missionNumber);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(token);

        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(ChatColor.of("#FFA07A") + "¡Inventario lleno! Token dropeado al suelo.");
        }
    }

    // Ficha que se entrega en la Estatua de Recompensas para recibir el cofre; el número de misión va en la PDC
    public ItemStack createMissionToken(int missionNumber) {
        ItemStack token = new ItemStack(Material.POPPED_CHORUS_FRUIT);
        ItemMeta meta = token.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Ficha de Misión " + tag(missionNumber));
        meta.getPersistentDataContainer().set(TOKEN_KEY, PersistentDataType.INTEGER, missionNumber);
        meta.setItemModel(NamespacedKey.minecraft("popped_chorus_fruit"));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Misión Completada:");
        lore.add(ChatColor.of("#FFCC99") + "Misión: " + ChatColor.WHITE + displayName(missionNumber));
        lore.add("");
        lore.add(ChatColor.GRAY + "Entrégalo en el spawn.");
        lore.add(ChatColor.GRAY + "> Click Derecho a la:");
        lore.add(ChatColor.of("#FFB347") + "Estatua de Recompensas");

        meta.setLore(lore);
        token.setItemMeta(meta);
        return token;
    }

    // Número de misión de una ficha, o -1 si no es una (las de antes de la 26.2 usaban custom model data 3000 + número)
    @SuppressWarnings("deprecation")
    public static int getTokenMission(ItemStack item) {
        if (item == null || item.getType() != Material.POPPED_CHORUS_FRUIT || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        Integer number = meta.getPersistentDataContainer().get(TOKEN_KEY, PersistentDataType.INTEGER);
        if (number != null) return number;
        if (meta.hasCustomModelData() && meta.getCustomModelData() > 3000 && meta.getCustomModelData() <= 5000) {
            return meta.getCustomModelData() - 3000;
        }
        return -1;
    }

    public void addMissionToPlayer(CommandSender sender, String playerName, int missionNumber) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado o offline.");
            return;
        }

        MissionData data = getData(target, missionNumber);
        if (data.isCompleted()) {
            sender.sendMessage(ChatColor.YELLOW + "El jugador ya tiene completada esta misión.");
            return;
        }

        data.setActive(true);
        saveData(target, missionNumber, data);

        completeMission(target.getName(), missionNumber);

        sender.sendMessage(ChatColor.GREEN + "Has forzado la completación de la misión " + missionNumber + " para " + playerName);
    }

    public void removeMissionFromPlayer(CommandSender sender, String playerName, int missionNumber) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado o offline.");
            return;
        }

        MissionData data = getData(target, missionNumber);

        data.setActive(true);
        data.setCompleted(false);
        data.setRewardClaimed(false);
        data.getProgress().clear();

        saveData(target, missionNumber, data);

        sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " reiniciada para " + playerName);
    }

    public Map<Integer, Mission> getMissions() { return missions; }

    public Set<Integer> getActiveMissions() { return globalActiveMissions; }

    public boolean isMissionActive(Player player, int missionId) {
        return globalActiveMissions.contains(missionId);
    }

    public boolean isMissionCompleted(Player player, int missionId) {
        return getData(player, missionId).isCompleted();
    }

    public void completeMission(Player player, int missionId) {
        completeMission(player.getName(), missionId);
    }

    public int getTotalMissionCount() { return missions.size(); }

    public int getCompletedMissionCount(Player player) {
        Map<Integer, MissionData> data = playerCache.get(player.getUniqueId());
        if (data == null) return 0;

        int completed = 0;
        for (Integer missionId : missions.keySet()) {
            MissionData missionData = data.get(missionId);
            if (missionData != null && missionData.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }
}
