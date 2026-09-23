package Events.MissionSystem;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

// Se lanza cuando muere un jefe, con todos los jugadores que pelearon contra él.
// bossId: abeja_reina, ender_dragon, wither, elder_guardian o el valor de la PDC "boss_id" del jefe
// (infested_warden_boss, ultra_warden, rey_ender cuando estén hechos)
public class BossDefeatedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String bossId;
    private final LivingEntity boss;
    private final Set<Player> players;

    public BossDefeatedEvent(String bossId, LivingEntity boss, Set<Player> players) {
        this.bossId = bossId;
        this.boss = boss;
        this.players = players;
    }

    public String getBossId() { return bossId; }
    public LivingEntity getBoss() { return boss; }
    public Set<Player> getPlayers() { return players; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
