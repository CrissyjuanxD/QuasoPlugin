package Events.MissionSystem;

import Handlers.DatabaseManager;
import Handlers.DayHandler;
import TitleListener.RuletaAnimation;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissionHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final DatabaseManager dbManager;
    private final Map<UUID, Map<Integer, MissionData>> playerCache = new ConcurrentHashMap<>();
    private final Map<Integer, Mission> missions = new HashMap<>();
    private final Set<Integer> activeMissions = new HashSet<>();
    private final RuletaAnimation ruletaAnimation;

    public MissionHandler(JavaPlugin plugin, DatabaseManager dbManager, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.dayHandler = dayHandler;
        this.ruletaAnimation = new RuletaAnimation(plugin);

        registerMissions();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<Integer> dbActiveMissions = dbManager.getGlobalActiveMissions();
            activeMissions.addAll(dbActiveMissions);
            plugin.getLogger().info("Se han restaurado " + activeMissions.size() + " misiones activas desde la BD.");
        });

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::autoSaveAll, 3600L, 3600L);
    }

    private void registerMissions() {
        missions.put(1, new Mission1(plugin, this));

        missions.put(2, new Mission2(plugin, this));

        missions.put(3, new Mission3(plugin, this));

        missions.put(4, new Mission4(plugin, this));

        missions.put(5, new Mission5(plugin, this));

        missions.put(6, new Mission6(plugin, this));

        missions.put(7, new Mission7(plugin, this));

        missions.put(8, new Mission8(plugin, this));

        missions.put(9, new Mission9(plugin, this));

        missions.put(10, new Mission10(plugin, this));

        missions.put(11, new Mission11(plugin, this));

        missions.put(12, new Mission12(plugin, this));

        missions.put(13, new Mission13(plugin, this));

        missions.put(14, new Mission14(plugin, this));

        missions.put(15, new Mission15(plugin, this));

        missions.put(16, new Mission16(plugin, this));

        missions.put(17, new Mission17(plugin, this));

        missions.put(18, new Mission18(plugin, this));

        missions.put(19, new Mission19(plugin, this));

        missions.put(20, new Mission20(plugin, this));

        missions.put(21, new Mission21(plugin, this));

        missions.put(22, new Mission22(plugin, this));

        missions.put(23, new Mission23(plugin, this));

        missions.put(24, new Mission24(plugin, this));

        missions.put(25, new Mission25(plugin, this));

        missions.put(26, new Mission26(plugin, this));

        missions.put(27, new Mission27(plugin, this));
    }

    public void registerAllMissionListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Mission mission : missions.values()) {
            if (mission instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) mission, plugin);
            }
        }
        plugin.getLogger().info("Sistema de misiones: Listeners registrados correctamente.");
    }

    // --- GESTIÓN DE CACHÉ ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, MissionData> data = dbManager.loadPlayerMissions(uuid);
            playerCache.put(uuid, data);

            // Opcional: Si hay misiones activas globales que el jugador no tiene iniciadas, iniciarlas aquí
            for (int activeId : activeMissions) {
                if (!data.containsKey(activeId) || !data.get(activeId).isActive()) {
                    Bukkit.getScheduler().runTask(plugin, () -> initializePlayerMissionData(event.getPlayer().getName(), activeId));
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<Integer, MissionData> data = playerCache.get(uuid);
        if (data != null) {
            Map<Integer, MissionData> dirtyMissions = new HashMap<>();
            Map<Integer, String> missionNames = new HashMap<>();

            for (Map.Entry<Integer, MissionData> missionEntry : data.entrySet()) {
                if (missionEntry.getValue().isDirty()) {
                    dirtyMissions.put(missionEntry.getKey(), missionEntry.getValue());
                    missionEntry.getValue().setDirty(false);
                    String name = missions.containsKey(missionEntry.getKey()) ? ChatColor.stripColor(missions.get(missionEntry.getKey()).getName()) : "Unknown";
                    missionNames.put(missionEntry.getKey(), name);
                }
            }
            if (!dirtyMissions.isEmpty()) {
                dbManager.savePlayerMissionsBatchSync(uuid, player.getName(), dirtyMissions, missionNames);
            }
        }
        playerCache.remove(uuid);
    }

    public MissionData getData(Player player, int missionId) {
        Map<Integer, MissionData> pData = playerCache.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        return pData.computeIfAbsent(missionId, k -> new MissionData());
    }

    public void saveData(Player player, int missionId, MissionData data) {
        Map<Integer, MissionData> pData = playerCache.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        pData.put(missionId, data);

        data.setDirty(true);
    }

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
                    missionEntry.getValue().setDirty(false); // Reseteamos la marca de sucio

                    String name = missions.containsKey(missionEntry.getKey()) ? ChatColor.stripColor(missions.get(missionEntry.getKey()).getName()) : "Unknown";
                    missionNames.put(missionEntry.getKey(), name);
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

    public void activateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        if (activeMissions.contains(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " ya está activada.");
            return;
        }

        activeMissions.add(missionNumber);

        for (Player online : Bukkit.getOnlinePlayers()) {
            initializePlayerMissionData(online.getName(), missionNumber);
        }

        sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " activada y distribuida a jugadores online.");

        String missionName = missions.get(missionNumber).getName();
        String missionDesc = missions.get(missionNumber).getDescription();

        String safeDescription = missionDesc.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonMessage = String.format(
                "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"}," +
                        "{\"text\":\"NUEVA MISIÓN DESBLOQUEADA\",\"bold\":true,\"color\":\"#FFA500\"}," +
                        "{\"text\":\"\\n[\",\"color\":\"white\"}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#dda0dd\"," +
                        "\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\",\"color\":\"gray\"}}}," +
                        "{\"text\":\"]\\n\\n\",\"color\":\"white\"}," +
                        "{\"text\":\"usa /misiones para abrir su interfaz o usa el item de Misiones\",\"color\":\"gray\"}]",
                missionName,
                safeDescription
        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            ruletaAnimation.playAnimation(online, jsonMessage);
        }
    }

    public boolean deactivateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return false;
        }

        if (!activeMissions.contains(missionNumber)) {
            sender.sendMessage(ChatColor.RED + "La misión " + missionNumber + " no está activa, por lo que no se puede desactivar.");
            return false;
        }

        activeMissions.remove(missionNumber);

        for (Player online : Bukkit.getOnlinePlayers()) {
            MissionData data = getData(online, missionNumber);
            if (data.isActive()) {
                data.setActive(false);
                saveData(online, missionNumber, data);
            }
        }
        dbManager.deactivateMissionGlobally(missionNumber);
        sender.sendMessage(ChatColor.GREEN + "Misión " + missionNumber + " desactivada correctamente.");
        return true;
    }

    public void initializePlayerMissionData(String playerName, int missionNumber) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            MissionData data = getData(p, missionNumber);
            if (!data.isActive()) {
                data.setActive(true);
                saveData(p, missionNumber, data);
            }
        }
    }

    public boolean completeMission(String playerName, int missionNumber) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return false;

        MissionData data = getData(player, missionNumber);

        if (data.isCompleted()) return false;

        data.setCompleted(true);
        saveData(player, missionNumber, data);

        giveMissionToken(player, missionNumber);

        String missionName = missions.get(missionNumber).getName();

        String jsonMessage = String.format(
                "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#87ceeb\"}," +
                        "{\"text\":\" ha completado la misión \",\"color\":\"#7eaee4\"}," +
                        "{\"text\":\"[\",\"color\":\"white\"}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#dda0dd\"}," +
                        "{\"text\":\"]\\n\",\"color\":\"white\"}]",
                player.getName(),
                missionName
        );

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        long completedCount = playerCache.get(player.getUniqueId()).values().stream()
                .filter(MissionData::isCompleted).count();

        player.sendMessage(ChatColor.GREEN + "Progreso Total: " + ChatColor.GOLD + completedCount +
                ChatColor.GREEN + " misiones completadas.");

        return true;
    }

    // Métodos de utilidad
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

    public ItemStack createMissionToken(int missionNumber) {
        ItemStack token = new ItemStack(Material.POPPED_CHORUS_FRUIT);
        ItemMeta meta = token.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Ficha de Misión #" + missionNumber);
        meta.setCustomModelData(3000 + missionNumber);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        String missionName = "Misión Desconocida";
        if (missions.containsKey(missionNumber)) {
            missionName = missions.get(missionNumber).getName();
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Misión Completada:");
        lore.add(ChatColor.of("#FFCC99") + "Misión: " + ChatColor.WHITE + missionName);
        lore.add("");
        lore.add(ChatColor.GRAY + "Entrégalo en el spawn.");
        lore.add(ChatColor.GRAY + "> Click Derecho a la:");
        lore.add(ChatColor.of("#FFB347") + "Estatua de Recompensas");

        meta.setLore(lore);
        token.setItemMeta(meta);
        return token;
    }


    // Métodos addMissionToPlayer y removeMissionFromPlayer se mantienen igual que tu versión original
    // ya que son administrativos.
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

        // Forzamos que esté activa y la completamos
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

    // --- GETTERS ---

    public Map<Integer, Mission> getMissions() { return missions; }

    public Set<Integer> getActiveMissions() { return activeMissions; }

    public DayHandler getDayHandler() { return dayHandler; }

    public boolean isMissionActive(Player player, int missionId) {
        return getData(player, missionId).isActive();
    }

    public boolean isMissionCompleted(Player player, int missionId) {
        return getData(player, missionId).isCompleted();
    }

    // Método auxiliar para completar misión pasando Player directo (usado internamente por listeners)
    public void completeMission(Player player, int missionId) {
        completeMission(player.getName(), missionId);
    }
}