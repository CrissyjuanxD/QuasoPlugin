package Commands;

import Dificultades.Change;
import Handlers.ChangesHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChangesCommand implements CommandExecutor, TabCompleter {

    private final ChangesHandler handler;

    public ChangesCommand(ChangesHandler handler) {
        this.handler = handler;
    }

    // /changes list muestra las etapas; activar y desactivar prenden o apagan una (por nombre o número)
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("quasoplugin.changes")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            list(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (!action.equals("activar") && !action.equals("desactivar")) {
            sender.sendMessage(ChatColor.RED + "Uso: /changes <activar|desactivar|list> [cambio]");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /changes " + action + " <cambio>");
            return true;
        }

        Change change = handler.find(args[1]);
        if (change == null) {
            sender.sendMessage(ChatColor.RED + "No existe el cambio '" + args[1] + "'. Mira la lista con /changes list.");
            return true;
        }

        if (action.equals("activar")) {
            if (handler.activate(change)) {
                sender.sendMessage(ChatColor.GREEN + "Cambio " + change.id() + " activado.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "El cambio " + change.id() + " ya estaba activo.");
            }
        } else {
            if (handler.deactivate(change)) {
                sender.sendMessage(ChatColor.GREEN + "Cambio " + change.id() + " desactivado.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "El cambio " + change.id() + " no estaba activo.");
            }
        }
        return true;
    }

    private void list(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Cambios del server ===");
        for (Change change : handler.getChanges()) {
            String state = change.isApplied() ? ChatColor.GREEN + "ACTIVO" : ChatColor.RED + "INACTIVO";
            sender.sendMessage(ChatColor.YELLOW + change.id() + ChatColor.GRAY + " [" + state + ChatColor.GRAY + "] "
                    + ChatColor.WHITE + change.description());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("quasoplugin.changes")) return List.of();
        if (args.length == 1) return filter(List.of("activar", "desactivar", "list"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("activar") || args[0].equalsIgnoreCase("desactivar"))) {
            List<String> ids = new ArrayList<>();
            for (Change change : handler.getChanges()) ids.add(change.id());
            return filter(ids, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String typed) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(typed.toLowerCase(Locale.ROOT))) result.add(option);
        }
        return result;
    }
}
