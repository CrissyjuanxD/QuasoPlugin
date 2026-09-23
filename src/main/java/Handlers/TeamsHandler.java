package Handlers;

import Handlers.Teams.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamsHandler {

    private final Scoreboard scoreboard;

    public TeamsHandler() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void loadTeams() {
        for (TeamType type : TeamType.values()) {
            Team team = scoreboard.getTeam(type.getId());

            if (team == null) {
                team = scoreboard.registerNewTeam(type.getId());
            }

            team.setPrefix(type.getChatPrefix());
            team.setColor(type.getBukkitColor());

            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
        }
        Bukkit.getLogger().info("§acargados y prefijos actualizados.");
    }

    public void addPlayerToTeam(Player player, TeamType type) {
        Team team = scoreboard.getTeam(type.getId());
        if (team != null) {
            team.addEntry(player.getName());
        }
    }
}