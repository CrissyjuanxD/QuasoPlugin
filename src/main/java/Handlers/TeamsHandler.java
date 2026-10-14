package Handlers;

import Handlers.Teams.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;

public class TeamsHandler {

    private final Scoreboard scoreboard;

    public TeamsHandler() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    // Crea o actualiza todos los teams del server con su prefijo y color. Arriba de la cabeza sale el prefijo del chat
    // (el unicode del rango) y el nombre en el color del team, que en los rangos es blanco
    public void loadTeams() {
        quitarTeamViejo("TSurvivor");
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

    // Teams que ya no existen: sus jugadores pasan a DinoNugget y el team se borra
    private void quitarTeamViejo(String id) {
        Team viejo = scoreboard.getTeam(id);
        if (viejo == null) return;
        Team nugget = scoreboard.getTeam(TeamType.Z_MIEMBRO.getId());
        if (nugget == null) nugget = scoreboard.registerNewTeam(TeamType.Z_MIEMBRO.getId());
        for (String entry : new HashSet<>(viejo.getEntries())) nugget.addEntry(entry);
        viejo.unregister();
    }

    public void addPlayerToTeam(Player player, TeamType type) {
        Team team = scoreboard.getTeam(type.getId());
        if (team != null) {
            team.addEntry(player.getName());
        }
    }
}
