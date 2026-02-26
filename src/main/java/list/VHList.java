package list;

import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;
    private boolean showCreator = true;
    private int counter = 0;

    public VHList(JavaPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                showCreator = !showCreator;
            }
        }.runTaskTimer(plugin, 0L, 200L); // 200 ticks = 10 segundos
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
            updateHealthScoreboard(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        String header = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.BLUE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.RED + "" + ChatColor.BOLD + "      \uD83E\uDD50" + ChatColor.GOLD + ChatColor.BOLD + " CROISSANTS " + ChatColor.RED + ChatColor.BOLD + "\uD83E\uDD50    " +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n";

        String alternatingText;
            alternatingText = ChatColor.WHITE + "" + ChatColor.BOLD + "Organizado por: " + ChatColor.YELLOW + "Crosszy";

        String PingText;
        int ping = player.getPing();
        if (ping < 100) {
            PingText = ChatColor.WHITE + "" + ChatColor.BOLD + "Ping: " + ChatColor.GREEN + ping + "ms";
        } else if (ping < 200) {
            PingText = ChatColor.WHITE + "" + ChatColor.BOLD + "Ping: " + ChatColor.YELLOW + ping + "ms";
        } else {
            PingText = ChatColor.WHITE + "" + ChatColor.BOLD + "Ping: " + ChatColor.RED + ping + "ms";
        }


        String footer = ChatColor.GRAY + " \n" +
                PingText + " \n" +
                ChatColor.GRAY + " \n" +
                alternatingText + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.BLUE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
                ChatColor.DARK_GRAY + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " +
                ChatColor.BLUE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
                ChatColor.DARK_GRAY + ChatColor.BOLD + "●";
        player.setPlayerListHeaderFooter(header, footer);

        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        String tabPrefix = "";
        String colorHex = "";
        String suffix = "";

        if (team != null) {
            suffix = team.getSuffix();
            TeamType type = TeamType.getById(team.getName());

            if (type != null) {
                tabPrefix = type.getTabPrefix();
                colorHex = type.getBungeeColor().toString();
            }
        }

        String coloredName = ChatColor.WHITE + tabPrefix + colorHex + player.getName() + suffix + " ";
        player.setPlayerListName(coloredName);
    }

    public void updateHealthScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        // Crear o obtener el objetivo de salud
        Objective healthObjective = scoreboard.getObjective("Healthvct");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("Healthvct", "health",
                    ChatColor.DARK_PURPLE + "❤ Vida", RenderType.HEARTS);
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }


    // Método para obtener el Unicode según el equipo
    private String getUnicodeForTeam(Team team) {
        if (team != null) {
            String teamName = team.getName();
            switch (teamName) {
                case "Admin":
                    return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#ff935f") + ChatColor.BOLD + "HOK" + ChatColor.GRAY + ChatColor.BOLD + "]";
                case "Mod":
                    return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#00BFFF") + ChatColor.BOLD + "ANB" + ChatColor.GRAY + ChatColor.BOLD + "]";
                case "Helper":
                    return "\uEB92";
                case "TSurvivor":
                    return "\uEB8F";
                case "ZMiembro":
                    return ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#ffa39d") + ChatColor.BOLD + "ALD" + ChatColor.GRAY + ChatColor.BOLD + "]";
                case "ZFantasma":
                    return "\uEB91";
                default:
                    return "";
            }
        }
        return "";
    }

    private String getColorForTeam(Team team) {
        if (team != null) {
            String teamName = team.getName();
            switch (teamName) {
                case "Admin":
                    return ChatColor.of("#ff935f").toString();
                case "Mod":
                    return ChatColor.of("#00BFFF").toString();
                case "Helper":
                    return ChatColor.of("#67E590").toString();
                case "TSurvivor":
                    return ChatColor.of("#9455ED").toString();
                case "ZMiembro":
                    return ChatColor.of("#ffa39d").toString();
                case "ZFantasma":
                    return ChatColor.of("#555555").toString();
                default:
                    return ChatColor.WHITE.toString();
            }
        }
        return ChatColor.GRAY.toString();
    }

}
