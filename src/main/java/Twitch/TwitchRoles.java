package Twitch;

import Events.MissionSystem.MissionHandler;
import Handlers.Teams.TeamType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Set;

// DinoSub y DinoVip reemplazan solo a DinoNugget y DinoNugget+. A los del staff, DinoLeyenda, los equipos de
// eventos y los fantasmas no se los toca. Cuando la sub o el VIP terminan vuelve al rango que le toca por misiones
final class TwitchRoles {

    private static final Set<String> MANAGED = Set.of(TeamType.Z_MIEMBRO.getId(), TeamType.Y_MIEMBRO.getId(),
            TeamType.U_SUB.getId(), TeamType.V_VIP.getId());

    private final MissionHandler missions;

    TwitchRoles(MissionHandler missions) {
        this.missions = missions;
    }

    void apply(Player player, TwitchAccount account) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team current = board.getEntryTeam(player.getName());
        String currentId = current == null ? null : current.getName();
        if (currentId != null && !MANAGED.contains(currentId)) return;

        TeamType target;
        if (account != null && account.sub) target = TeamType.U_SUB;
        else if (account != null && account.vip) target = TeamType.V_VIP;
        else if (TeamType.U_SUB.getId().equals(currentId) || TeamType.V_VIP.getId().equals(currentId)) target = base(player, account);
        else return;

        if (target.getId().equals(currentId)) return;
        if (account != null && (target == TeamType.U_SUB || target == TeamType.V_VIP)
                && (currentId == null || currentId.equals(TeamType.Z_MIEMBRO.getId()) || currentId.equals(TeamType.Y_MIEMBRO.getId()))) {
            account.previousRole = currentId;
        }
        Team team = board.getTeam(target.getId());
        if (team == null) return;
        team.addEntry(player.getName());

        if (target == TeamType.U_SUB) {
            player.sendMessage(TwitchText.PREFIX + ChatColor.of("#E6D6FF") + "¡Ahora eres " + target.getChatPrefix().trim()
                    + ChatColor.of("#E6D6FF") + "! Gracias por apoyar el canal de Crosszy.");
        } else if (target == TeamType.V_VIP) {
            player.sendMessage(TwitchText.PREFIX + ChatColor.of("#E6D6FF") + "¡Ahora eres " + target.getChatPrefix().trim()
                    + ChatColor.of("#E6D6FF") + "!");
        } else {
            player.sendMessage(TwitchText.PREFIX + ChatColor.of("#D3D3D3") + "Tu sub o VIP del canal terminó: vuelves a ser "
                    + target.getChatPrefix().trim() + ChatColor.of("#D3D3D3") + ".");
        }
    }

    // DinoNugget+ si ya lo era antes de la sub o si llegó a las 30 misiones mientras tanto
    private TeamType base(Player player, TwitchAccount account) {
        if (account != null && TeamType.Y_MIEMBRO.getId().equals(account.previousRole)) return TeamType.Y_MIEMBRO;
        if (missions != null && missions.getCompletedCount(player) >= MissionHandler.MISSIONS_FOR_PLUS) return TeamType.Y_MIEMBRO;
        return TeamType.Z_MIEMBRO;
    }
}
