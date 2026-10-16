package Commands;

import Handlers.Dimensiones;
import Twitch.TwitchFly;
import imp.crissyjuanxd.QuasoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// /tpa <jugador>: le pide al jugador ir a donde está; él acepta o rechaza desde el chat (/tpaccept, /tpdeny).
// Al aceptar, quien la pidió espera 5 segundos quieto y sin recibir daño, igual que en /home
public class Tpa implements CommandExecutor, TabCompleter, Listener {

    private static final long EXPIRA_MS = 60_000;
    // Morado, azul y celeste pastel
    private static final TextColor MORADO = TextColor.color(0xB79CFF);
    private static final TextColor AZUL = TextColor.color(0x7DA7FF);
    private static final TextColor CELESTE = TextColor.color(0xA8E4FF);

    private final QuasoPlugin plugin;
    // Quien recibe → (quien pide → cuándo la pidió)
    private final Map<UUID, LinkedHashMap<UUID, Long>> peticiones = new HashMap<>();
    private final Map<UUID, Espera> esperando = new HashMap<>();

    private static final class Espera {
        private final Location origen;
        private final UUID destino;
        private BukkitTask task;

        private Espera(Location origen, UUID destino) {
            this.origen = origen.clone();
            this.destino = destino;
        }

        private void cancel() {
            if (task != null) task.cancel();
        }
    }

    public Tpa(QuasoPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "tpa" -> pedir(player, args);
            case "tpaccept" -> responder(player, args, true);
            case "tpdeny" -> responder(player, args, false);
        }
        return true;
    }

    private void pedir(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(texto("Uso: /tpa <jugador>"));
            return;
        }
        Player objetivo = Bukkit.getPlayerExact(args[0]);
        if (objetivo == null) {
            player.sendMessage(texto("Ese jugador no está conectado."));
            return;
        }
        if (objetivo.equals(player)) {
            player.sendMessage(texto("No puedes enviarte una petición a ti mismo."));
            return;
        }
        if (TwitchFly.inEvent(player) || TwitchFly.inEvent(objetivo)) {
            player.sendMessage(texto("No se puede usar /tpa durante un evento."));
            return;
        }
        LinkedHashMap<UUID, Long> recibidas = peticiones.computeIfAbsent(objetivo.getUniqueId(), id -> new LinkedHashMap<>());
        limpiar(recibidas);
        if (recibidas.containsKey(player.getUniqueId())) {
            player.sendMessage(texto("Ya le enviaste una petición a ").append(nombre(objetivo))
                    .append(Component.text(", espera a que la responda.", CELESTE)));
            return;
        }
        recibidas.put(player.getUniqueId(), System.currentTimeMillis());

        Component aceptar = Component.text("[Aceptar]", AZUL).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept " + player.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Dejar que " + player.getName() + " venga a donde estás", CELESTE)));
        Component rechazar = Component.text("[Rechazar]", MORADO).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny " + player.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Rechazar la petición", CELESTE)));
        objetivo.sendMessage(texto("El jugador ").append(nombre(player))
                .append(Component.text(" quiere teletransportarse a donde tú estás ", CELESTE))
                .append(aceptar).append(Component.text(" ")).append(rechazar));
        objetivo.playSound(objetivo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        player.sendMessage(texto("Has enviado una petición de teletransporte a ").append(nombre(objetivo))
                .append(Component.text(", espera a que la acepte.", CELESTE)));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    // Sin nombre responde a la última petición que le llegó
    private void responder(Player player, String[] args, boolean acepta) {
        LinkedHashMap<UUID, Long> recibidas = peticiones.get(player.getUniqueId());
        if (recibidas != null) limpiar(recibidas);
        if (recibidas == null || recibidas.isEmpty()) {
            player.sendMessage(texto("No tienes peticiones de teletransporte pendientes."));
            return;
        }
        UUID id;
        if (args.length >= 1) {
            Player quien = Bukkit.getPlayerExact(args[0]);
            id = quien == null ? null : quien.getUniqueId();
            if (id == null || !recibidas.containsKey(id)) {
                player.sendMessage(texto("No tienes una petición de teletransporte de " + args[0] + "."));
                return;
            }
        } else {
            id = null;
            for (UUID uuid : recibidas.keySet()) id = uuid;
        }
        recibidas.remove(id);
        Player solicitante = Bukkit.getPlayer(id);
        if (solicitante == null) {
            player.sendMessage(texto("Ese jugador ya no está conectado."));
            return;
        }

        if (!acepta) {
            player.sendMessage(texto("Has rechazado la petición de teletransporte de ").append(nombre(solicitante)));
            solicitante.sendMessage(texto("").append(nombre(player))
                    .append(Component.text(" te ha rechazado la petición de teletransporte", CELESTE)));
            solicitante.playSound(solicitante.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (Dimensiones.bloquea(solicitante, player.getWorld())) {
            player.sendMessage(texto("La dimensión donde estás está cerrada, ").append(nombre(solicitante))
                    .append(Component.text(" no puede venir.", CELESTE)));
            solicitante.sendMessage(texto("La dimensión de ").append(nombre(player))
                    .append(Component.text(" está cerrada temporalmente.", CELESTE)));
            return;
        }
        player.sendMessage(texto("Has aceptado la petición de teletransporte de ").append(nombre(solicitante)));
        solicitante.sendMessage(texto("").append(nombre(player))
                .append(Component.text(" ha aceptado tu petición de teletransporte", CELESTE)));
        empezar(solicitante, player);
    }

    private void empezar(Player player, Player destino) {
        Espera previa = esperando.remove(player.getUniqueId());
        if (previa != null) previa.cancel();
        if (player.isOp()) {
            teletransportar(player, destino);
            return;
        }
        Espera espera = new Espera(player.getLocation(), destino.getUniqueId());
        esperando.put(player.getUniqueId(), espera);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
        espera.task = new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (esperando.get(player.getUniqueId()) != espera || !player.isOnline()) {
                    if (esperando.get(player.getUniqueId()) == espera) esperando.remove(player.getUniqueId());
                    espera.cancel();
                    return;
                }
                Player objetivo = Bukkit.getPlayer(espera.destino);
                if (objetivo == null) {
                    cancelar(player, "¡Teletransporte cancelado, el jugador se desconectó!");
                    return;
                }
                if (count > 0) {
                    player.sendActionBar(Component.text("Teletransportando en ", CELESTE)
                            .append(Component.text(count, count <= 2 ? MORADO : AZUL).decorate(TextDecoration.BOLD))
                            .append(Component.text("...", CELESTE)));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    count--;
                } else {
                    esperando.remove(player.getUniqueId());
                    espera.cancel();
                    if (Dimensiones.bloquea(player, objetivo.getWorld())) return;
                    teletransportar(player, objetivo);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teletransportar(Player player, Player destino) {
        player.setFallDistance(0);
        player.teleport(destino.getLocation());
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.sendActionBar(Component.text("¡Teletransportado a ", CELESTE).decorate(TextDecoration.BOLD)
                .append(Component.text(destino.getName(), AZUL)).append(Component.text("!", CELESTE)));
    }

    private void cancelar(Player player, String mensaje) {
        Espera espera = esperando.remove(player.getUniqueId());
        if (espera == null) return;
        espera.cancel();
        player.sendMessage(texto("").append(Component.text(mensaje, MORADO)));
        player.sendActionBar(Component.text("¡Teletransporte cancelado!", MORADO).decorate(TextDecoration.BOLD));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getFinalDamage() > 0 && event.getEntity() instanceof Player player) {
            cancelar(player, "¡Teletransporte cancelado por recibir daño!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Espera espera = esperando.get(event.getPlayer().getUniqueId());
        if (espera != null && movido(espera.origen, event.getTo())) {
            cancelar(event.getPlayer(), "¡Teletransporte cancelado por moverte!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        onMove(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Espera espera = esperando.remove(id);
        if (espera != null) espera.cancel();
        peticiones.remove(id);
        for (LinkedHashMap<UUID, Long> recibidas : peticiones.values()) recibidas.remove(id);
    }

    private boolean movido(Location from, Location to) {
        return to == null || !Objects.equals(from.getWorld(), to.getWorld())
                || from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
    }

    private void limpiar(LinkedHashMap<UUID, Long> recibidas) {
        long ahora = System.currentTimeMillis();
        recibidas.values().removeIf(cuando -> ahora - cuando > EXPIRA_MS);
    }

    private Component texto(String mensaje) {
        return Component.text("⚙ ", MORADO).decorate(TextDecoration.BOLD)
                .append(Component.text(mensaje, CELESTE).decoration(TextDecoration.BOLD, false));
    }

    private Component nombre(Player player) {
        return Component.text(player.getName(), AZUL).decoration(TextDecoration.BOLD, false);
    }

    public void shutdown() {
        esperando.values().forEach(Espera::cancel);
        esperando.clear();
        peticiones.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) return List.of();
        List<String> nombres = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("tpa")) {
            for (Player online : Bukkit.getOnlinePlayers()) if (!online.equals(player)) nombres.add(online.getName());
        } else {
            LinkedHashMap<UUID, Long> recibidas = peticiones.get(player.getUniqueId());
            if (recibidas != null) {
                for (UUID id : recibidas.keySet()) {
                    Player quien = Bukkit.getPlayer(id);
                    if (quien != null) nombres.add(quien.getName());
                }
            }
        }
        List<String> opciones = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], nombres, opciones);
        return opciones;
    }
}
