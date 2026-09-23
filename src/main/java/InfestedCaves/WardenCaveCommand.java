package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando único de la dimensión: /wardencave <subcomando> ...
 *
 * Subcomandos:
 *   /wardencave join <jugador/@a>
 *   /wardencave leave <jugador/@a>
 *   /wardencave portal [remove]
 */
public class WardenCaveCommand implements CommandExecutor, TabCompleter {

    private final QuasoPlugin plugin;
    private final PortalManager portalManager;

    public WardenCaveCommand(QuasoPlugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "portal":
                return handlePortal(sender, args);
            case "join":
            case "leave":
                return handleJoinLeave(sender, sub, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /wardencave <portal [remove] | join <jugador/@a> | leave <jugador/@a>>");
    }

    // --- /wardencave portal [remove] ---
    private boolean handlePortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo un jugador puede usar este subcomando.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
            portalManager.removePortalNearby(p.getLocation());
            p.sendMessage(ChatColor.RED + "Portal eliminado.");
        } else {
            portalManager.spawnPortal(p.getLocation());
            p.sendMessage(ChatColor.GREEN + "Portal creado. (Funciona en ambas direcciones)");
        }
        return true;
    }

    // --- /wardencave join|leave <jugador/@a> ---
    private boolean handleJoinLeave(CommandSender sender, String action, String[] args) {
        if (!sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /wardencave " + action + " <jugador/@a>");
            return true;
        }

        String target = args[1];

        if (target.equalsIgnoreCase("@a")) {
            for (Player p : Bukkit.getOnlinePlayers()) executeJoinLeave(p, action);
            sender.sendMessage(ChatColor.GREEN + "Ejecutado para todos.");
            return true;
        }

        Player pTarget = Bukkit.getPlayer(target);
        if (pTarget == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return true;
        }

        executeJoinLeave(pTarget, action);
        sender.sendMessage(ChatColor.GREEN + "Ejecutado para " + pTarget.getName());
        return true;
    }

    private void executeJoinLeave(Player p, String action) {
        if (action.equalsIgnoreCase("join")) {
            if (Bukkit.getWorld(QuasoPlugin.WORLD_NAME) == null) {
                p.sendMessage(ChatColor.RED + "El mundo no está cargado.");
                return;
            }
            Location loc = portalManager.findSafeSpawn(Bukkit.getWorld(QuasoPlugin.WORLD_NAME));
            p.teleport(loc);
            p.sendMessage(ChatColor.GREEN + "Teletransportado a WardenCave.");
        } else if (action.equalsIgnoreCase("leave")) {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            p.sendMessage(ChatColor.GREEN + "Enviado al spawn del Overworld.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("portal");
            completions.add("join");
            completions.add("leave");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("portal")) {
                completions.add("remove");
            } else if (sub.equals("join") || sub.equals("leave")) {
                completions.add("@a");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}