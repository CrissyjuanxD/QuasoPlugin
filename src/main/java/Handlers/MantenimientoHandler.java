package Handlers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MantenimientoHandler implements CommandExecutor, TabCompleter, Listener {

    private static final String CONFIG_KEY = "mantenimiento.activo";

    /** Mensaje de expulsion, en dos lineas y en rojo. */
    private static final String KICK_MESSAGE =
            ChatColor.RED + "El servidor está en mantenimiento,\n" +
                    ChatColor.RED + "revisa Discord para saber cuándo abre nuevamente.";

    private final JavaPlugin plugin;
    private boolean activo;

    public MantenimientoHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activo = plugin.getConfig().getBoolean(CONFIG_KEY, false);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (activo) {
            plugin.getLogger().warning("El servidor ha arrancado en MODO MANTENIMIENTO: solo entran operadores.");
        }
    }

    public void reload() {
        activo = plugin.getConfig().getBoolean(CONFIG_KEY, false);
        if (activo) expulsarNoOperadores();
    }

    public boolean isActivo() {
        return activo;
    }

    private void setActivo(boolean valor) {
        this.activo = valor;
        plugin.getConfig().set(CONFIG_KEY, valor);
        plugin.saveConfig();
    }

    // ------------------------------------------------------------------
    //  Entrada al servidor
    // ------------------------------------------------------------------

    /**
     * Se usa PlayerLoginEvent y no PlayerJoinEvent: aqui todavia se puede
     * rechazar la conexion, asi el jugador ni llega a cargar en el mundo.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!activo) return;
        if (event.getPlayer().isOp()) return;

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
    }

    /** A los operadores se les recuerda al entrar que el modo sigue puesto. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!activo) return;

        Player player = event.getPlayer();
        if (!player.isOp()) return;

        player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.RED + "Modo mantenimiento encendido"
                + ChatColor.GRAY + ".");
        player.sendMessage(ChatColor.GRAY + "Solo los operadores pueden entrar. Usa "
                + ChatColor.of("#FFCC99") + "/mantenimiento off" + ChatColor.GRAY + " para abrirlo.");
    }

    // ------------------------------------------------------------------
    //  /mantenimiento on|off
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.mantenimiento")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }
        if (args.length != 1) {
            enviarUso(sender);
            return true;
        }

        String opcion = args[0].toLowerCase();

        if (opcion.equals("on")) {
            if (activo) {
                sender.sendMessage(ChatColor.of("#FFA07A") + "El modo mantenimiento ya estaba encendido.");
                return true;
            }

            setActivo(true);
            int expulsados = expulsarNoOperadores();

            sender.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.RED + "Modo mantenimiento ENCENDIDO"
                    + ChatColor.GRAY + ". Solo entran operadores.");
            sender.sendMessage(ChatColor.GRAY + "Jugadores expulsados: "
                    + ChatColor.of("#FFCC99") + expulsados);

            plugin.getLogger().info("Modo mantenimiento ENCENDIDO por " + sender.getName()
                    + " (" + expulsados + " jugadores expulsados).");
            return true;
        }

        if (opcion.equals("off")) {
            if (!activo) {
                sender.sendMessage(ChatColor.of("#FFA07A") + "El modo mantenimiento ya estaba apagado.");
                return true;
            }

            setActivo(false);

            sender.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#98FB98") + "Modo mantenimiento APAGADO"
                    + ChatColor.GRAY + ". El servidor esta abierto.");

            plugin.getLogger().info("Modo mantenimiento APAGADO por " + sender.getName() + ".");
            return true;
        }

        enviarUso(sender);
        return true;
    }

    private void enviarUso(CommandSender sender) {
        String estado = activo
                ? ChatColor.RED + "encendido"
                : ChatColor.of("#98FB98") + "apagado";

        sender.sendMessage(ChatColor.GRAY + "Uso: " + ChatColor.of("#FFCC99") + "/mantenimiento <on|off>");
        sender.sendMessage(ChatColor.GRAY + "Estado actual: " + estado);
    }

    /**
     * Echa a todos los que no son operadores.
     *
     * Se itera sobre una copia de la lista porque expulsar modifica la
     * coleccion de jugadores conectados.
     */
    private int expulsarNoOperadores() {
        int expulsados = 0;

        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (online.isOp()) continue;

            online.kickPlayer(KICK_MESSAGE);
            expulsados++;
        }
        return expulsados;
    }

    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opciones = new ArrayList<>();
            for (String opcion : Arrays.asList("on", "off")) {
                if (opcion.startsWith(args[0].toLowerCase())) opciones.add(opcion);
            }
            return opciones;
        }
        return new ArrayList<>();
    }
}
