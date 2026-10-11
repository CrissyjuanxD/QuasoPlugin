package list;

import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

public class VHList extends BukkitRunnable {

    private static final ChatColor DECORATION_PURPLE = ChatColor.of("#572a8c");
    private static final ChatColor DECORATION_BLUE = ChatColor.of("#3b69aa");
    private static final ChatColor DECORATION_LIGHT_BLUE = ChatColor.of("#4d8be0");
    private static final ChatColor STUDIO_COLOR = ChatColor.of("#7f3bd2");

    private final JavaPlugin plugin;
    private boolean showCreator = true;

    public VHList(JavaPlugin plugin) {
        this.plugin = plugin;
        this.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                showCreator = !showCreator;
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeOldScoreboards(player);
            updateTablistForPlayer(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        int ping = player.getPing();

        String separator = DECORATION_PURPLE + "●" + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
                DECORATION_BLUE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
                DECORATION_LIGHT_BLUE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + DECORATION_LIGHT_BLUE + "●\n";

        String header = separator +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "\uE906\n" +
                ChatColor.GRAY + " \n" +
                DECORATION_BLUE + "📊 ONLINE: " + ChatColor.WHITE + online + DECORATION_PURPLE + "  |  " +
                DECORATION_LIGHT_BLUE + "📶 PING: " + ChatColor.WHITE + ping + " ms\n" +
                ChatColor.GRAY + " \n";

        String footer = " \n" + ChatColor.WHITE + "" + ChatColor.BOLD + "Organizado por: " + DECORATION_LIGHT_BLUE + "Crosszy" + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + "Programado por " + STUDIO_COLOR + "@Viciont Studios\n" +
                ChatColor.GRAY + " \n" +
                separator.replace("\n", "");

        player.setPlayerListHeaderFooter(header, footer);

        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainScoreboard.getEntryTeam(player.getName());

        String tabPrefix = "";
        String colorHex = ChatColor.GRAY.toString();
        String suffix = "";

        if (team != null) {
            suffix = team.getSuffix();
            TeamType type = TeamType.getById(team.getName());

            if (type != null) {
                tabPrefix = type.getTabPrefix();
                colorHex = type.getBungeeColor().toString();
            }
        }

        String coloredName = ChatColor.WHITE + tabPrefix + colorHex + player.getName() + suffix;

        String currentName = player.getPlayerListName();
        if (currentName == null || !currentName.equals(coloredName)) {
            player.setPlayerListName(coloredName);
        }

        Scoreboard viewerScoreboard = player.getScoreboard();
        Objective healthObjective = viewerScoreboard.getObjective("tabHealth");

        if (healthObjective == null) {
            healthObjective = viewerScoreboard.registerNewObjective("tabHealth", "dummy", ChatColor.RED + "❤");
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            int healthInt = (int) Math.ceil(target.getHealth());
            healthObjective.getScore(target.getName()).setScore(healthInt);
        }
    }

    public void removeOldScoreboards(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != null) {
            Objective oldHealth = scoreboard.getObjective("Healthvct");
            if (oldHealth != null) oldHealth.unregister();

            Objective oldPing = scoreboard.getObjective("tabPing");
            if (oldPing != null) oldPing.unregister();
        }
    }
}
