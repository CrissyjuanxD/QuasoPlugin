package Events.ItemParty;

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

public class ItemPartyCommand implements CommandExecutor, TabCompleter {

    private final ItemPartyHandler itemPartyHandler;

    public ItemPartyCommand(ItemPartyHandler itemPartyHandler) {
        this.itemPartyHandler = itemPartyHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.command.itemparty")) {
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
                itemPartyHandler.iniciarEvento();
                sender.sendMessage(ChatColor.GREEN + "ItemParty iniciado.");
                break;
            case "end":
                itemPartyHandler.terminarEvento();
                sender.sendMessage(ChatColor.RED + "ItemParty terminado.");
                break;
            case "reset":
                itemPartyHandler.resetPlayersFile();
                sender.sendMessage(ChatColor.GREEN + "ItemParty: Lista de jugadores/castigos reseteada.");
                break;
            case "reload":
                itemPartyHandler.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ItemParty: Configuración recargada.");
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /itemparty <start|end|reset|reload>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("start", "end", "reset", "reload");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, completions);
            Collections.sort(completions);
            return completions;
        }
        return Collections.emptyList();
    }
}
