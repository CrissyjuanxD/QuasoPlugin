package Events.Skybattle;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LavaClashCommand implements CommandExecutor, TabCompleter {

    private final EventoHandler eventoHandler;

    public LavaClashCommand(EventoHandler eventoHandler) {
        this.eventoHandler = eventoHandler;
    }

    // Subcomandos de admin del LavaClash
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.command.lavaclash")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                if (eventoHandler.isEventoActivo()) {
                    sender.sendMessage(ChatColor.RED + "El evento ya está iniciado. Usa /lavaclash end si quieres detenerlo.");
                } else {
                    eventoHandler.iniciarEvento(false);
                    sender.sendMessage(ChatColor.GREEN + "LavaClash: Fase de recolección iniciada.");
                }
                break;
            case "silencestart":
                if (eventoHandler.isEventoActivo()) {
                    sender.sendMessage(ChatColor.RED + "El evento ya está iniciado.");
                } else {
                    eventoHandler.iniciarEvento(true);
                    sender.sendMessage(ChatColor.GREEN + "LavaClash: Fase de recolección iniciada (Modo Silencioso).");
                }
                break;
            case "battle":
                if (!eventoHandler.isEventoActivo()) {
                    sender.sendMessage(ChatColor.RED + "No puedes iniciar la batalla, el evento no está activo.");
                    return true;
                }
                if (eventoHandler.isSecuenciaBatallaIniciada()) {
                    sender.sendMessage(ChatColor.RED + "La secuencia de batalla ya ha sido iniciada. No spamees el comando.");
                    return true;
                }
                eventoHandler.iniciarSecuenciaInicioSkyBattle();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Procesando secuencia de batalla...");
                break;
            case "force":
                if (!eventoHandler.isEventoActivo()) {
                    sender.sendMessage(ChatColor.RED + "El evento no está activo.");
                    return true;
                }
                eventoHandler.forzarEvento();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Teletransporte forzado.");
                break;
            case "reglas":
                eventoHandler.espera1();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Explicando reglas...");
                break;
            case "resetpurple":
                eventoHandler.eliminarPurpleConcrete();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Bloques morados eliminados.");
                break;
            case "spawnfinal":
                eventoHandler.spawnFinal();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Todos los jugadores en la zona han sido devueltos al spawn.");
                break;
            case "end":
                if (!eventoHandler.isEventoActivo()) {
                    sender.sendMessage(ChatColor.RED + "El evento ya está apagado.");
                    return true;
                }
                eventoHandler.terminarEvento();
                sender.sendMessage(ChatColor.RED + "LavaClash finalizado forzosamente.");
                break;
            case "list":
                eventoHandler.gestionarParticipantes(sender, new String[]{"list"});
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash add <jugador>");
                    return true;
                }
                eventoHandler.gestionarParticipantes(sender, new String[]{"add", args[1]});
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash remove <jugador>");
                    return true;
                }
                eventoHandler.gestionarParticipantes(sender, new String[]{"remove", args[1]});
                break;
            case "exclude":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lavaclash exclude <jugador>");
                    return true;
                }
                eventoHandler.excludePlayer(sender, args[1]);
                break;
            case "reload":
                eventoHandler.crearYcargarConfig();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Configuración (lavaclashconfig.yml) recargada con éxito.");
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /lavaclash <start|silencestart|battle|force|reglas|resetpurple|spawnfinal|end|list|add|remove|exclude|reload>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("start", "silencestart", "battle", "force", "reglas", "resetpurple", "spawnfinal", "end", "list", "add", "remove", "exclude", "reload");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("exclude")) {
                return null;
            }
        }
        return Collections.emptyList();
    }
}