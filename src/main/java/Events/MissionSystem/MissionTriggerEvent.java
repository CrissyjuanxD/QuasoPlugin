package Events.MissionSystem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Aviso genérico para que otros sistemas sumen progreso sin depender de las misiones.
// Acciones que se usan: "blackjack_ganada"
public class MissionTriggerEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String action;
    private final int amount;

    public MissionTriggerEvent(Player player, String action, int amount) {
        this.player = player;
        this.action = action;
        this.amount = amount;
    }

    public static void call(Player player, String action) {
        Bukkit.getPluginManager().callEvent(new MissionTriggerEvent(player, action, 1));
    }

    public Player getPlayer() { return player; }
    public String getAction() { return action; }
    public int getAmount() { return amount; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
