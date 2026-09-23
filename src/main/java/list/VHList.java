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

        String separator = ChatColor.DARK_GRAY + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.BLUE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " +
                ChatColor.GRAY + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n";

        String header = separator +
                ChatColor.GRAY + " \n" +
                ChatColor.RED + "" + ChatColor.BOLD + "      \uD83E\uDD50" + ChatColor.GOLD + ChatColor.BOLD + " CROISSANTS " + ChatColor.RED + ChatColor.BOLD + "\uD83E\uDD50     \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.of("#facc15") + "📊 ONLINE: " + ChatColor.WHITE + online + ChatColor.DARK_GRAY + "  |  " +
                ChatColor.of("#4ade80") + "📶 PING: " + ChatColor.WHITE + ping + " ms\n" +
                ChatColor.GRAY + " \n";

        String footer = " \n" + ChatColor.WHITE + "" + ChatColor.BOLD + "Organizado por: " + ChatColor.YELLOW + "Crosszy" + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + "Programado por @CrissyjuanxD\n" +
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