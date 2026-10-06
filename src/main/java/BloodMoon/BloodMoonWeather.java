package BloodMoon;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Conserva lluvia y truenos reales; suaviza únicamente el oscurecimiento enviado al cliente. */
final class BloodMoonWeather implements Listener {
    static final float STORM_VISUAL_STRENGTH = 0.60f;
    private static final String HANDLER = "quaso_bloodmoon_weather";
    private final BloodMoon manager;
    private final BloodMoonSky sky;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final AtomicBoolean warned = new AtomicBoolean();
    private Packets packets;
    private boolean unavailable;
    private boolean closed;
    private BukkitTask task;

    interface Packets {
        Channel channel(Player player) throws ReflectiveOperationException;
        Object scale(Object packet, float factor) throws ReflectiveOperationException;
        void refresh(Player player) throws ReflectiveOperationException;
    }

    static final class Filter extends ChannelOutboundHandlerAdapter {
        private final Packets packets;
        private final Consumer<Exception> failure;
        volatile float factor = 1;

        Filter(Packets packets, Consumer<Exception> failure) { this.packets = packets; this.failure = failure; }

        @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
            Object output = message;
            try { output = packets.scale(message, factor); }
            catch (ReflectiveOperationException | RuntimeException ex) { failure.accept(ex); }
            context.write(output, promise);
        }
    }

    private record Session(Player player, Channel channel, Filter filter) {}

    BloodMoonWeather(BloodMoon manager, BloodMoonSky sky) { this.manager = manager; this.sky = sky; }
    BloodMoonWeather(BloodMoon manager, BloodMoonSky sky, Packets packets) { this(manager, sky); this.packets = packets; }

    void enable() {
        manager.getPlugin().getServer().getPluginManager().registerEvents(this, manager.getPlugin());
        task = manager.GetScheduler().runTaskTimer(manager.getPlugin(), () -> {
            for (Player player : manager.getPlugin().getServer().getOnlinePlayers()) sync(player, false);
        }, 1, 2);
    }

    void sync(Player player, boolean force) {
        if (closed) return;
        ConfigReader config = manager.getConfigReader(player.getWorld());
        float strength = config != null && config.GetThunderingConfig() ? sky.strength(player.getWorld()) : 0;
        // Respeta el clima personal establecido por otros plugins.
        if (!player.isOnline() || strength <= 0 || player.getPlayerWeather() != null) {
            remove(player, player.isOnline() && player.getPlayerWeather() == null);
            return;
        }
        if (unavailable) return;
        try {
            if (packets == null) packets = new BloodMoonWeatherPackets();
            Session session = sessions.get(player.getUniqueId());
            if (session == null) {
                Channel channel = packets.channel(player);
                Filter filter = new Filter(packets, this::warn);
                channel.pipeline().addBefore("packet_handler", HANDLER, filter);
                session = new Session(player, channel, filter);
                sessions.put(player.getUniqueId(), session);
                force = true;
            }
            float factor = 1 - (1 - STORM_VISUAL_STRENGTH) * strength;
            if (force || session.filter.factor != factor) {
                session.filter.factor = factor;
                packets.refresh(player);
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            unavailable = true;
            warn(ex);
            for (Session session : List.copyOf(sessions.values())) remove(session.player, true);
        }
    }

    private void remove(Player player, boolean restore) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        session.filter.factor = 1;
        if (session.channel.pipeline().context(session.filter) != null) session.channel.pipeline().remove(session.filter);
        if (restore && player.isOnline()) {
            try { packets.refresh(player); }
            catch (ReflectiveOperationException | RuntimeException ex) { warn(ex); }
        }
    }

    private void warn(Exception ex) {
        if (warned.compareAndSet(false, true)) manager.getPlugin().getLogger().warning(
                "No se pudo suavizar el clima visual de BloodMoon; se conserva el clima normal: " + ex.getClass().getSimpleName());
    }

    @EventHandler(priority = EventPriority.MONITOR) public void onJoin(PlayerJoinEvent event) { sync(event.getPlayer(), true); }
    @EventHandler(priority = EventPriority.MONITOR) public void onWorldChange(PlayerChangedWorldEvent event) { sync(event.getPlayer(), true); }
    @EventHandler(priority = EventPriority.MONITOR) public void onRespawn(PlayerPostRespawnEvent event) { sync(event.getPlayer(), true); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { remove(event.getPlayer(), false); }

    void shutdown() {
        closed = true;
        if (task != null) task.cancel();
        for (Session session : List.copyOf(sessions.values())) remove(session.player, session.player.getPlayerWeather() == null);
        HandlerList.unregisterAll(this);
    }
}
