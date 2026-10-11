package Handlers;

import Trabajos.TrabajosManager;
import items.EconomyItems;
import org.bukkit.map.MinecraftFont;
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
                    title());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(NumberFormat.blank());
        }
        objective.setDisplayName(title());
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
        String lilac = ChatColor.of("#c6a5e8").toString();
        String blue = ChatColor.of("#a9c8ef").toString();
        java.util.List<String> content = java.util.List.of(
                icon("usuario", "\uE901") + lilac + "Usuario: " + ChatColor.of("#6fa2e7") + player.getName(),
                icon("rango", "\uE902") + blue + "Rango: " + rankColor + rankName,
                icon("misiones", "\uE903") + lilac + "Misiones: " + missionsProgress(player),
                icon("dinocoins", "\uE904") + blue + "DinoCoins: " + ChatColor.of(EconomyItems.DINOCOINS_COLOR)
                        + dinoCoinsManager.getCachedDinoCoins(player.getUniqueId()),
                icon("trabajo", "\uE905") + lilac + "Trabajo: " + TrabajosManager.lineaScoreboard(player));
        for (int i = 0; i < content.size(); i++) {
            setLine(scoreboard, objective, 11 - i * 2, content.get(i));
            setLine(scoreboard, objective, 10 - i * 2, ChatColor.GRAY + " ".repeat(i + 2));
        }
        String address = "croissant.holy.gg";
        int width = Math.max(textWidth(title()), content.stream().mapToInt(this::textWidth).max().orElse(0));
        // Cada espacio ocupa 4 píxeles: la mitad del margen va delante de la IP.
        int spaces = Math.max(0, (width - textWidth(address)) / 8);
        setLine(scoreboard, objective, 1, " ".repeat(spaces) + ChatColor.of("#7f3bd2") + address);
    }

    private String title() {
        return ChatColor.WHITE + ChatColor.stripColor(plugin.getConfig().getString("main-scoreboard.titulo", "\uE900"));
    }

    private String icon(String name, String fallback) {
        return ChatColor.WHITE + ChatColor.stripColor(plugin.getConfig().getString("main-scoreboard.iconos." + name, fallback)) + " ";
    }

    private int textWidth(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char format = Character.toLowerCase(text.charAt(++i));
                if (format == 'l') bold = true;
                else if ("0123456789abcdefrx".indexOf(format) >= 0) bold = false;
                continue;
            }
            var glyph = MinecraftFont.Font.getChar(c);
            int glyphWidth = glyph == null ? 8 : glyph.getWidth() + 1;
            width += glyphWidth + (bold && c != ' ' ? 1 : 0);
        }
        return Math.max(0, width - 1);
    }

    private void addConfigDefaults() {
        Map<String, String> defaults = Map.of(
                "main-scoreboard.titulo", "\uE900",
                "main-scoreboard.iconos.usuario", "\uE901",
                "main-scoreboard.iconos.rango", "\uE902",
                "main-scoreboard.iconos.misiones", "\uE903",
                "main-scoreboard.iconos.dinocoins", "\uE904",
                "main-scoreboard.iconos.trabajo", "\uE905");
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
