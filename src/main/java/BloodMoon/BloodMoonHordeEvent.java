package BloodMoon;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/** Una sola decisión antes de crear cualquiera de los mobs de la horda. */
public final class BloodMoonHordeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player target;
    private final Location origin;
    private final List<Location> locations;
    private boolean cancelled;

    public BloodMoonHordeEvent(Player target, List<Location> locations) {
        this.target = target;
        this.origin = target.getLocation().clone();
        this.locations = locations.stream().map(Location::clone).toList();
    }

    public Player getTarget() { return target; }
    public Location getOrigin() { return origin.clone(); }
    public List<Location> getLocations() { return locations.stream().map(Location::clone).toList(); }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
