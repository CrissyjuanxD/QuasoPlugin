package Twitch;

import Handlers.Teams.TeamType;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// /fly para los subs del canal y los operadores. No se puede en la Warden Cave, en el End, en los mundos de
// twitch.yml ni durante un evento, y los admins lo apagan para todos con /twitchadmin fly off.
// Solo maneja el vuelo que da él: el doble salto de Habilidades y el vuelo de BuildBattle no se tocan
public final class TwitchFly implements CommandExecutor, Listener {

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final long FALL_PROTECTION = 10_000L;

    private final JavaPlugin plugin;
    private final TwitchManager manager;
    private final Map<UUID, Long> noFallUntil = new HashMap<>();
    private BukkitTask task;

    TwitchFly(JavaPlugin plugin, TwitchManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        ACTIVE.clear();
        ACTIVE.addAll(manager.store().flyActive);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
    }

    // Habilidades pregunta esto para no cancelar el vuelo de /fly con el doble salto
    public static boolean isActive(Player player) {
        return ACTIVE.contains(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar /fly.");
            return true;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.GRAY + "En este modo de juego ya puedes volar.");
            return true;
        }
        if (ACTIVE.contains(player.getUniqueId())) {
            disable(player, TwitchText.TEXT + "Fly desactivado.");
            return true;
        }
        String reason = denial(player);
        if (reason != null) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + reason);
            return true;
        }
        enable(player);
        player.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Fly activado: toca dos veces el espacio para volar.");
        return true;
    }

    // Por qué no puede volar ahora (null = puede)
    String denial(Player player) {
        if (!manager.store().flyEnabled) return "El /fly está apagado por un evento.";
        if (blocked(player.getWorld())) return "No se puede volar en la Warden Cave ni en el End.";
        if (inEvent(player)) return "No se puede volar durante un evento.";
        if (!player.isOp() && !manager.isSub(player)) return "El /fly es para los subs del canal de Crosszy.";
        return null;
    }

    boolean blocked(World world) {
        return world.getEnvironment() == World.Environment.THE_END
                || world.getName().equals(QuasoPlugin.WORLD_NAME)
                || manager.config().worldBlocked(world.getName());
    }

    static boolean inEvent(Player player) {
        String team = teamOf(player);
        return team != null && (team.equals(TeamType.LAVACLASH.getId()) || team.equals(TeamType.ITEMPARTY.getId())
                || team.equals(TeamType.HOTPOTATO.getId()) || team.equals(TeamType.BUILDBATTLE.getId()));
    }

    private static String teamOf(Player player) {
        var team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        return team == null ? null : team.getName();
    }

    private void enable(Player player) {
        ACTIVE.add(player.getUniqueId());
        manager.store().flyActive.add(player.getUniqueId());
        manager.store().markDirty();
        player.setFlyingFallDamage(net.kyori.adventure.util.TriState.NOT_SET);
        player.setAllowFlight(true);
    }

    // Apaga el vuelo que dio /fly; si estaba en el aire no recibe daño de caída al aterrizar
    void disable(Player player, String message) {
        forget(player.getUniqueId());
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            if (player.isFlying() || !player.isOnGround()) noFallUntil.put(player.getUniqueId(), System.currentTimeMillis() + FALL_PROTECTION);
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (message != null) player.sendMessage(TwitchText.PREFIX + message);
    }

    private void forget(UUID uuid) {
        ACTIVE.remove(uuid);
        if (manager.store().flyActive.remove(uuid)) manager.store().markDirty();
    }

    // /twitchadmin fly off lo saca a todos; con on se puede volver a usar
    void setEnabled(boolean enabled) {
        manager.store().flyEnabled = enabled;
        manager.store().markDirty();
        if (enabled) return;
        for (UUID uuid : new ArrayList<>(ACTIVE)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) disable(player, TwitchText.HIGHLIGHT + "El /fly se apagó por un evento.");
            else forget(uuid);
        }
    }

    // Cada segundo: se le quita a quien ya no puede (fin de la sub, cambio de mundo, evento) y se le devuelve el
    // permiso de volar si otro sistema se lo sacó (por ejemplo al salir de creativo)
    private void tick() {
        long now = System.currentTimeMillis();
        noFallUntil.values().removeIf(until -> until < now);
        for (UUID uuid : new ArrayList<>(ACTIVE)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            check(player);
        }
    }

    private void check(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        String reason = denial(player);
        if (reason != null) {
            // En BuildBattle el evento maneja el vuelo: solo se olvida el /fly sin tocar lo que puso el evento
            if (TeamType.BUILDBATTLE.getId().equals(teamOf(player))) {
                forget(player.getUniqueId());
                return;
            }
            disable(player, TwitchText.HIGHLIGHT + "Se te quitó el fly: " + reason);
            return;
        }
        if (!player.getAllowFlight()) player.setAllowFlight(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!ACTIVE.contains(player.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && ACTIVE.contains(player.getUniqueId())) check(player);
        }, 20L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (ACTIVE.contains(event.getPlayer().getUniqueId())) check(event.getPlayer());
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!ACTIVE.contains(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && ACTIVE.contains(player.getUniqueId())) check(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        Long until = noFallUntil.remove(player.getUniqueId());
        if (until != null && until >= System.currentTimeMillis()) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        noFallUntil.remove(event.getPlayer().getUniqueId());
    }

    void shutdown() {
        if (task != null) task.cancel();
        task = null;
    }
}
