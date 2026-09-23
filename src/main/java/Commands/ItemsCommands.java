package Commands;

import Managers.ItemManager;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommands implements CommandExecutor, TabCompleter {

    private final QuasoPlugin plugin;
    private final ItemManager itemManager;

    public ItemsCommands(QuasoPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        plugin.getCommand("giveqp").setExecutor(this);
        plugin.getCommand("giveqp").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /giveqp <item> [cantidad] [jugador]");
            return true;
        }

        String itemName = args[0].toLowerCase();
        int cantidad = 1;
        Player target = null;

        if (args.length > 1) {
            try {
                cantidad = Integer.parseInt(args[1]);
                if (cantidad <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador '" + args[1] + "' no está en línea.");
                    return true;
                }
            }
        }

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cEl jugador '" + args[2] + "' no está en línea.");
                return true;
            }
        }

        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cDebes especificar un jugador si ejecutas el comando desde la consola.");
                return true;
            }
        }

        ItemStack item = itemManager.getItem(itemName, cantidad, target);

        if (item == null) {
            sender.sendMessage("§cEse item no existe: " + itemName);
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a " + target.getName() + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return itemManager.getRegisteredItems().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }

            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");

            return completions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}