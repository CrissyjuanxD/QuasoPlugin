package Handlers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Una cola por jugador compartida entre las misiones y los indicadores continuos. */
public final class ActionBarHandler implements Listener {
    private static final int REFRESH_TICKS = 10;
    private static final int MESSAGE_TICKS = 100;
    // El cliente deja cada action bar 3 segundos y la desvanece en el último
    private static final int CLIENT_TICKS = 60;
    private static final int CLIENT_FADE_TICKS = 20;
    private static final int MAX_PENDING = 64;
    private static final Map<JavaPlugin, Coordinator> COORDINATORS = new HashMap<>();

    private final JavaPlugin plugin;

    public ActionBarHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ActionBarHandler get(JavaPlugin plugin) {
        return new ActionBarHandler(plugin);
    }

    /** Mantiene la API anterior; los avisos repetidos se agrupan mientras sigan visibles. */
    public void sendActionBar(Player player, String message) {
        sendNotification(player, "notice:" + message, message);
    }

    /** Los objetivos completados conservan su orden y cinco segundos de lectura. */
    public void sendNotification(Player player, String key, String message) {
        coordinator().enqueue(player, key, message, false);
    }

    /** Agrupa los cambios del mismo objetivo sin prolongar su turno en pantalla. */
    public void sendProgress(Player player, String key, String message) {
        coordinator().enqueue(player, key, message, true);
    }

    /** El indicador de fondo solo aparece cuando no hay un aviso de misión en pantalla. */
    public void setBackground(Player player, String source, String message) {
        if (!player.isOnline()) return;
        Coordinator coordinator = coordinator();
        PlayerBars bars = coordinator.players.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerBars(player));
        bars.backgroundSource = source;
        bars.background = message;
        if (bars.active == null) display(player, message);
    }

    public void clearBackground(Player player, String source) {
        Coordinator coordinator = COORDINATORS.get(plugin);
        if (coordinator == null) return;
        PlayerBars bars = coordinator.players.get(player.getUniqueId());
        if (bars == null || !Objects.equals(source, bars.backgroundSource)) return;
        bars.background = null;
        bars.backgroundSource = null;
        if (bars.active == null) {
            display(player, "");
            if (bars.pending.isEmpty()) coordinator.players.remove(player.getUniqueId());
        }
    }

    public void clear(Player player) {
        Coordinator coordinator = COORDINATORS.get(plugin);
        if (coordinator != null) coordinator.players.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    /** Se llama al apagar el plugin para soltar tareas, jugadores y la instancia compartida. */
    public static void shutdown(JavaPlugin plugin) {
        Coordinator coordinator = COORDINATORS.remove(plugin);
        if (coordinator == null) return;
        coordinator.task.cancel();
        HandlerList.unregisterAll(coordinator.listener);
        for (PlayerBars bars : coordinator.players.values()) {
            if (bars.player.isOnline()) display(bars.player, "");
        }
        coordinator.players.clear();
    }

    private Coordinator coordinator() {
        return COORDINATORS.computeIfAbsent(plugin, Coordinator::new);
    }

    private static void display(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private static final class Coordinator {
        private final Map<UUID, PlayerBars> players = new HashMap<>();
        private final BukkitTask task;
        private final ActionBarHandler listener;
        private long tick;

        private Coordinator(JavaPlugin plugin) {
            listener = new ActionBarHandler(plugin);
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, REFRESH_TICKS, REFRESH_TICKS);
        }

        private void enqueue(Player player, String key, String message, boolean progress) {
            if (!player.isOnline() || message == null || message.isEmpty()) return;
            PlayerBars bars = players.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerBars(player));
            if (bars.active != null && Objects.equals(key, bars.active.key)) {
                if (progress && bars.active.progress) {
                    bars.active.message = message;
                    // Si nadie espera turno sigue en pantalla 5 segundos desde el último cambio
                    if (bars.pending.isEmpty()) bars.until = tick + MESSAGE_TICKS;
                    display(player, message);
                    return;
                }
                if (!bars.active.progress) return;
            }
            for (Message pending : bars.pending) {
                if (!Objects.equals(key, pending.key)) continue;
                if (progress && pending.progress) pending.message = message;
                if (progress || !pending.progress) return;
            }
            if (!progress) {
                // La confirmación sustituye al progreso pendiente de ese mismo objetivo.
                bars.pending.removeIf(pending -> pending.progress && Objects.equals(key, pending.key));
            }
            if (bars.pending.size() >= MAX_PENDING) {
                if (progress) return;
                Message oldProgress = bars.pending.stream().filter(pending -> pending.progress).findFirst().orElse(null);
                if (oldProgress != null) bars.pending.remove(oldProgress);
                else bars.pending.removeFirst();
            }
            bars.pending.addLast(new Message(key, message, progress));
            if (bars.active == null) advance(bars);
        }

        // Sin nada más que mostrar no se borra: el último aviso ya se desvaneció solo
        private void advance(PlayerBars bars) {
            bars.active = bars.pending.pollFirst();
            if (bars.active != null) {
                bars.until = tick + MESSAGE_TICKS;
                display(bars.player, bars.active.message);
            } else if (bars.background != null) {
                display(bars.player, bars.background);
            }
        }

        // El último aviso deja de reenviarse 3 segundos antes de terminar para que el cliente lo desvanezca justo a
        // los 5 segundos; si viene otro se sigue reenviando para que no se apague antes de cambiar
        private boolean resend(PlayerBars bars) {
            long left = bars.until - tick;
            return bars.pending.isEmpty() && bars.background == null ? left >= CLIENT_TICKS : left > CLIENT_FADE_TICKS;
        }

        private void refresh() {
            tick += REFRESH_TICKS;
            Iterator<PlayerBars> iterator = players.values().iterator();
            while (iterator.hasNext()) {
                PlayerBars bars = iterator.next();
                if (!bars.player.isOnline()) {
                    iterator.remove();
                    continue;
                }
                if (bars.active != null) {
                    if (tick >= bars.until) advance(bars);
                    else if (resend(bars)) display(bars.player, bars.active.message);
                } else if (bars.background != null) {
                    display(bars.player, bars.background);
                }
                if (bars.active == null && bars.background == null) iterator.remove();
            }
        }

    }

    private static final class PlayerBars {
        private final Player player;
        private final LinkedList<Message> pending = new LinkedList<>();
        private Message active;
        private long until;
        private String backgroundSource;
        private String background;

        private PlayerBars(Player player) { this.player = player; }
    }

    private static final class Message {
        private final String key;
        private final boolean progress;
        private String message;

        private Message(String key, String message, boolean progress) {
            this.key = key;
            this.message = message;
            this.progress = progress;
        }
    }
}
