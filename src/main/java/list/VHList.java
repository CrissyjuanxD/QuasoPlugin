package list;

import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private static final String HEADER = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " +
            ChatColor.BLUE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
            ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
            ChatColor.GRAY + " \n" +
            ChatColor.RED + "" + ChatColor.BOLD + "      \uD83E\uDD50" + ChatColor.GOLD + ChatColor.BOLD + " CROISSANTS " + ChatColor.RED + ChatColor.BOLD + "\uD83E\uDD50    " +
            ChatColor.GRAY + " \n" +
            ChatColor.GRAY + " \n";

    private static final String FOOTER_BOTTOM = " \n" +
            ChatColor.GRAY + " \n" +
            ChatColor.WHITE + "" + ChatColor.BOLD + "Organizado por: " + ChatColor.YELLOW + "Crosszy\n" +
            ChatColor.GRAY + " \n" +
            ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
            ChatColor.BLUE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
            ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " +
            ChatColor.DARK_GRAY + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " +
            ChatColor.BLUE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
            ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " +
            ChatColor.DARK_GRAY + ChatColor.BOLD + "●";

    public VHList(JavaPlugin plugin) {
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
            updateHealthScoreboard(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        // 2. Solo calculamos dinámicamente lo que realmente cambia (el ping)
        int ping = player.getPing();
        String pingColor;

        if (ping < 100) {
            pingColor = ChatColor.GREEN.toString();
        } else if (ping < 200) {
            pingColor = ChatColor.YELLOW.toString();
        } else {
            pingColor = ChatColor.RED.toString();
        }

        String pingText = ChatColor.GRAY + " \n" + ChatColor.WHITE + "" + ChatColor.BOLD + "Ping: " + pingColor + ping + "ms";
        String footer = pingText + FOOTER_BOTTOM;

        player.setPlayerListHeaderFooter(HEADER, footer);

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

        // 3. Optimización de Red: Solo se envía el paquete al jugador si su nombre/clan realmente ha cambiado
        String currentName = player.getPlayerListName();
        if (currentName == null || !currentName.equals(coloredName)) {
            player.setPlayerListName(coloredName);
        }
    }

    public void updateHealthScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective healthObjective = scoreboard.getObjective("Healthvct");

        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("Healthvct", "health",
                    ChatColor.DARK_PURPLE + "❤ Vida", RenderType.HEARTS);
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }
}