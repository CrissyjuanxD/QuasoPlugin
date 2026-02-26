package Casino;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class CasinoCommands implements CommandExecutor {
    private final CasinoManager manager;

    public CasinoCommands(CasinoManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ismanu.admin")) return true;

        if (args.length == 0) return false;

        // /casino set <type>
        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Uso: /casino set <blackjack/slot>");
                return true;
            }

            Block target = player.getTargetBlockExact(5);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Mira a un bloque.");
                return true;
            }

            String type = args[1].toLowerCase();
            if (!type.equals("blackjack") && !type.equals("slot")) {
                player.sendMessage(ChatColor.RED + "Tipos válidos: blackjack, slot");
                return true;
            }

            manager.saveTable(target.getLocation(), type);
            player.sendMessage(ChatColor.GREEN + "Mesa de " + type + " creada.");
            return true;
        }

        // /casino remove
        if (args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player player)) return true;

            Block target = player.getTargetBlockExact(5);
            if (target != null && manager.isTable(target.getLocation())) {
                manager.removeTable(target.getLocation());
                player.sendMessage(ChatColor.GREEN + "Mesa eliminada.");
            } else {
                player.sendMessage(ChatColor.RED + "No estás mirando una mesa registrada.");
            }
            return true;
        }

        // /casino reload
        if (args[0].equalsIgnoreCase("reload")) {
            manager.reload();
            sender.sendMessage(ChatColor.GREEN + "Casino recargado.");
            return true;
        }

        return false;
    }
}