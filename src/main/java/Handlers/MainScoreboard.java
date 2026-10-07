package Handlers;

import Trabajos.TrabajosManager;
import Events.MissionSystem.MissionHandler;
import Handlers.Teams.TeamType;
import Gui.dinocoins.DinoCoinsManager;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class MainScoreboard extends BukkitRunnable implements Listener {

    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final DinoCoinsManager dinoCoinsManager;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public MainScoreboard(JavaPlugin plugin, MissionHandler missionHandler, DinoCoinsManager dinoCoinsManager) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.dinoCoinsManager = dinoCoinsManager;
        addConfigDefaults();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        Objective sidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null && !sidebar.getName().equals("QuasoBoard")) return;

        if (scoreboard == mainBoard || scoreboard.getObjective("QuasoBoard") == null) {
            scoreboard = playerBoards.computeIfAbsent(player.getUniqueId(),
                    id -> Bukkit.getScoreboardManager().getNewScoreboard());
            player.setScoreboard(scoreboard);
        }

        for (Team mainTeam : mainBoard.getTeams()) {
            Team pTeam = scoreboard.getTeam(mainTeam.getName());
            if (pTeam == null) {
                pTeam = scoreboard.registerNewTeam(mainTeam.getName());
            }
            pTeam.setPrefix(mainTeam.getPrefix());
            pTeam.setSuffix(mainTeam.getSuffix());
            pTeam.setColor(mainTeam.getColor());
            pTeam.setAllowFriendlyFire(mainTeam.allowFriendlyFire());
            pTeam.setCanSeeFriendlyInvisibles(mainTeam.canSeeFriendlyInvisibles());
            pTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, mainTeam.getOption(Team.Option.NAME_TAG_VISIBILITY));
            pTeam.setOption(Team.Option.COLLISION_RULE, mainTeam.getOption(Team.Option.COLLISION_RULE));
            for (String entry : new HashSet<>(pTeam.getEntries())) {
                if (!mainTeam.hasEntry(entry)) pTeam.removeEntry(entry);
            }
            for (String entry : mainTeam.getEntries()) {
                if (!pTeam.hasEntry(entry)) {
                    pTeam.addEntry(entry);
                }
            }
        }

        Objective objective = scoreboard.getObjective("QuasoBoard");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("QuasoBoard", Criteria.DUMMY,
                    plugin.getConfig().getString("main-scoreboard.titulo", "\uE900"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(NumberFormat.blank());
        }
        objective.setDisplayName(plugin.getConfig().getString("main-scoreboard.titulo", "\uE900"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Extraer rango
        String rankName = "Ninguno";
        String rankColor = ChatColor.GRAY.toString();
        Team playerTeam = mainBoard.getEntryTeam(player.getName());

        if (playerTeam != null) {
            TeamType type = TeamType.getById(playerTeam.getName());
            if (type != null) {
                rankName = type.getDisplayName();
                rankColor = type.getBungeeColor().toString();
            }
        }

        // Setear las líneas del scoreboard (de mayor a menor)
        setLine(scoreboard, objective, 12, ChatColor.GRAY + " ");
        setLine(scoreboard, objective, 11, ChatColor.of("#1180c4") + icon("usuario", "\uE901") + ChatColor.WHITE + "Usuario: " + ChatColor.of("#189adf") + player.getName());
        setLine(scoreboard, objective, 10, ChatColor.GRAY + "  ");
        setLine(scoreboard, objective, 9, ChatColor.of("#d99b2e") + icon("rango", "\uE902") + ChatColor.WHITE + "Rango: " + rankColor + rankName);
        setLine(scoreboard, objective, 8, ChatColor.GRAY + "   ");
        setLine(scoreboard, objective, 7, ChatColor.of("#dbc44f") + icon("misiones", "\uE903") + ChatColor.WHITE + "Misiones: " + missionsProgress(player));
        setLine(scoreboard, objective, 6, ChatColor.GRAY + "    ");
        setLine(scoreboard, objective, 5, ChatColor.of("#2fae60") + icon("dinocoins", "\uE904") + ChatColor.WHITE + "DinoCoins: " + ChatColor.of("#4ade80") + dinoCoinsManager.getCachedDinoCoins(player.getUniqueId()));
        setLine(scoreboard, objective, 4, ChatColor.GRAY + "     ");
        setLine(scoreboard, objective, 3, ChatColor.of("#a8505f") + icon("trabajo", "\uE905") + ChatColor.WHITE + "Trabajo: " + TrabajosManager.lineaScoreboard(player));
        setLine(scoreboard, objective, 2, ChatColor.GRAY + "      ");
        setLine(scoreboard, objective, 1, ChatColor.of("#fdfd96") + icon("ip", "\uE906") + "croissant.holy.gg");
    }

    private String icon(String name, String fallback) {
        return plugin.getConfig().getString("main-scoreboard.iconos." + name, fallback) + " ";
    }

    private void addConfigDefaults() {
        Map<String, String> defaults = Map.of(
                "main-scoreboard.titulo", "\uE900",
                "main-scoreboard.iconos.usuario", "\uE901",
                "main-scoreboard.iconos.rango", "\uE902",
                "main-scoreboard.iconos.misiones", "\uE903",
                "main-scoreboard.iconos.dinocoins", "\uE904",
                "main-scoreboard.iconos.trabajo", "\uE905",
                "main-scoreboard.iconos.ip", "\uE906");
        boolean changed = false;
        for (var entry : defaults.entrySet()) {
            if (!plugin.getConfig().contains(entry.getKey(), true)) {
                plugin.getConfig().set(entry.getKey(), plugin.getConfig().getString(entry.getKey(), entry.getValue()));
                changed = true;
            }
        }
        if (changed) plugin.saveConfig();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerBoards.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == playerBoards.get(player.getUniqueId())) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        playerBoards.clear();
    }

    private String missionsProgress(Player player) {
        if (missionHandler == null) {
            return ChatColor.of("#fdfd96") + "0" + ChatColor.of("#fff4b8") + "/0";
        }

        int completed = missionHandler.getCompletedMissionCount(player);
        int total = missionHandler.getTotalMissionCount();

        // Verde pastel al terminarlas todas, amarillo pastel mientras tanto.
        String countColor = (total > 0 && completed >= total)
                ? ChatColor.of("#4ade80").toString()
                : ChatColor.of("#fdfd96").toString();

        return countColor + completed + ChatColor.of("#fff4b8") + "/" + total;
    }

    private void setLine(Scoreboard board, Objective objective, int lineNumber, String text) {
        String entry = ChatColor.values()[lineNumber].toString() + ChatColor.RESET;

        Team team = board.getTeam("sb_line_" + lineNumber);
        if (team == null) {
            team = board.registerNewTeam("sb_line_" + lineNumber);
            team.addEntry(entry);
        }

        team.setPrefix(text);
        objective.getScore(entry).setScore(lineNumber);
    }
}
