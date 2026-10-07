package Trabajos;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Se lanza cada vez que un jugador sube de nivel en un trabajo (lo escuchan las misiones de trabajo)
public class TrabajoSubeNivelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Trabajo trabajo;
    private final int nivel;

    public TrabajoSubeNivelEvent(Player player, Trabajo trabajo, int nivel) {
        this.player = player;
        this.trabajo = trabajo;
        this.nivel = nivel;
    }

    public Player getPlayer() { return player; }
    public Trabajo getTrabajo() { return trabajo; }
    public int getNivel() { return nivel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
