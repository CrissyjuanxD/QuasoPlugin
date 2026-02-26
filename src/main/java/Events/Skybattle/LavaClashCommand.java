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
                eventoHandler.iniciarEvento();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Fase de recolección iniciada.");
                break;
            case "battle":
                eventoHandler.iniciarSecuenciaInicioSkyBattle();
                sender.sendMessage(ChatColor.GREEN + "LavaClash: Secuencia de batalla iniciada.");
                break;
            case "force":
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
            case "end":
                eventoHandler.terminarEvento();
                sender.sendMessage(ChatColor.RED + "LavaClash finalizado forzosamente.");
                break;
            case "list":
                // Reutilizamos la lógica que ya tenías en gestionarParticipantes
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
            default:
                sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /lavaclash <start|battle|force|reglas|resetpurple|end|list|add|remove>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("start", "battle", "force", "reglas", "resetpurple", "end", "list", "add", "remove");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                return null; // Devuelve lista de jugadores online
            }
        }
        return Collections.emptyList();
    }
}