package Gui.dinocoins;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DinoCoinsCommand implements CommandExecutor, TabCompleter {

    private final DinoCoinsManager dinocoinsManager;

    public DinoCoinsCommand(DinoCoinsManager dinocoinsManager) {
        this.dinocoinsManager = dinocoinsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dinocoins.admin")) {
            sender.sendMessage("§cNo tienes permiso.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUso: /dinocoins <add|remove|get> [jugador] [cantidad]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /dinocoins " + subCommand + " <jugador> <cantidad>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador debe estar online para modificar su monedero físico.");
                    return true;
                }
                int amount;
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage("§cCantidad inválida.");
                    return true;
                }

                if (amount <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor que cero.");
                    return true;
                }

                if (subCommand.equals("add")) {
                    boolean success = dinocoinsManager.addPhysicalDinoCoins(target, amount);
                    if (success) sender.sendMessage("§aSe añadieron " + amount + " DinoCoins a los monederos de " + target.getName());
                    else sender.sendMessage("§eSe añadieron algunos DinoCoins, pero el/los monedero(s) se llenaron antes de terminar.");
                } else {
                    boolean success = dinocoinsManager.removePhysicalDinoCoins(target, amount);
                    if (success) sender.sendMessage("§aSe removieron " + amount + " DinoCoins de los monederos de " + target.getName());
                    else sender.sendMessage("§eSe retiraron las DinoCoins disponibles, pero el jugador no tenía suficientes para completar la cantidad.");
                }
                break;

            case "get":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /dinocoins get <jugador>");
                    return true;
                }
                Player getTarget = Bukkit.getPlayer(args[1]);
                if (getTarget != null) {
                    int total = dinocoinsManager.calculatePhysicalDinoCoins(getTarget);
                    int tokens = dinocoinsManager.calculatePhysicalDinoFichas(getTarget);
                    sender.sendMessage("§a" + getTarget.getName() + " tiene " + total + " DinoCoins y " + tokens + " DinoFichas en sus monederos.");
                } else {
                    sender.sendMessage("§cJugador no encontrado u offline.");
                }
                break;

            default:
                sender.sendMessage("§cComando desconocido.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (!sender.hasPermission("dinocoins.admin")) {
            return completions;
        }

        if (args.length == 1) {
            commands.addAll(Arrays.asList("add", "remove", "get"));
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            // Sugerir jugadores para estos subcomandos
            if (Arrays.asList("add", "remove", "get").contains(subCommand)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commands.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }
        else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            // Sugerir cantidades para add y remove
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                commands.addAll(Arrays.asList("1", "10", "32", "64"));
                StringUtil.copyPartialMatches(args[2], commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}