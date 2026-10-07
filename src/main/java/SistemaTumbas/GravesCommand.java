package SistemaTumbas;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravesCommand implements CommandExecutor, TabCompleter {
    private final GravesManager manager;

    public GravesCommand(GravesManager manager) {
        this.manager = manager;
    }

    // Admin: list, reload, remove (por id o mirando la tumba) y place para poner una tumba de prueba
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tumbas.admin")) {
            sender.sendMessage("§cNo tienes permisos para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6=== Tumbas Admin ===");
            sender.sendMessage("§e/tumbas list");
            sender.sendMessage("§e/tumbas reload");
            sender.sendMessage("§e/tumbas remove [ID o mirando la tumba]");
            sender.sendMessage("§e/tumbas place <nombre>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("§aTumbas activas en el servidor: " + manager.getGraves().size() + " §7(modo " + manager.getModo().name().toLowerCase() + ")");
                for (Grave g : manager.getGraves()) {
                    String shortId = "#" + g.getId().toString().substring(0, 4);
                    sender.sendMessage(String.format("§7- %s §8(%s) §7[X:%d Y:%d Z:%d] §c⏱ %s",
                            g.getOwnerName(), shortId,
                            g.getLocation().getBlockX(), g.getLocation().getBlockY(), g.getLocation().getBlockZ(),
                            ModoTumba.reloj(g.getExpiryTime() - System.currentTimeMillis())));
                }
                break;

            case "reload":
                manager.loadConfig();
                sender.sendMessage("§aConfiguración recargada: modo " + manager.getModo().name().toLowerCase()
                        + (manager.teleportsToGrave() ? ", /muertes tepea." : ", /muertes no tepea."));
                break;

            case "remove":
                UUID toRemove = null;

                if (args.length == 1 && sender instanceof Player player) {
                    Block target = player.getTargetBlockExact(5);
                    // La tumba está en el bloque de aire de los pies: vale mirarlo o mirar el suelo de abajo
                    if (target != null) {
                        for (Grave g : manager.getGraves()) {
                            if (g.getLocation().getBlockX() == target.getX() &&
                                    (g.getLocation().getBlockY() == target.getY() || g.getLocation().getBlockY() == target.getY() + 1) &&
                                    g.getLocation().getBlockZ() == target.getZ()) {
                                toRemove = g.getId();
                                break;
                            }
                        }
                    }
                    if (toRemove == null) {
                        player.sendMessage("§cNo estás mirando ninguna tumba. Usa: /tumbas remove <#ID>");
                        return true;
                    }
                }
                else if (args.length > 1) {
                    String searchId = args[1].replace("#", "");
                    for (Grave g : manager.getGraves()) {
                        if (g.getId().toString().startsWith(searchId)) {
                            toRemove = g.getId();
                            break;
                        }
                    }
                }

                if (toRemove != null) {
                    manager.removeGrave(toRemove);
                    sender.sendMessage("§aTumba eliminada exitosamente.");
                } else {
                    sender.sendMessage("§cNo se encontró la tumba especificada.");
                }
                break;

            case "place":
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cUso: /tumbas place <nombre>");
                    return true;
                }
                manager.createFakeGrave(args[1], player.getLocation());
                player.sendMessage("§aTumba de prueba colocada exitosamente.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("tumbas.admin")) return completions;

        if (args.length == 1) {
            completions.add("list");
            completions.add("reload");
            completions.add("remove");
            completions.add("place");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            for (Grave g : manager.getGraves()) {
                completions.add("#" + g.getId().toString().substring(0, 4));
            }
        }

        List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }
}