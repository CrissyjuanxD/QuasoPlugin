package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WardenCaveCommand implements CommandExecutor, TabCompleter {

    private final QuasoPlugin plugin;
    private final PortalManager portalManager;

    public WardenCaveCommand(QuasoPlugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    // portal crea o quita un portal donde estás; join y leave mandan jugadores a la dimensión o al spawn
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
        sender.sendMessage(ChatColor.RED + "Uso: /wardencave <portal [remove] | join <jugador/@a> [spawn] | leave <jugador/@a>>");
    }

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

    // join <jugador> manda a un lugar al azar como el portal; con "spawn" lo manda al centro (solo admins)
    private boolean handleJoinLeave(CommandSender sender, String action, String[] args) {
        if (!sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /wardencave " + action + " <jugador/@a>" + (action.equals("join") ? " [spawn]" : ""));
            return true;
        }

        String target = args[1];
        boolean center = args.length > 2 && args[2].equalsIgnoreCase("spawn");

        if (target.equalsIgnoreCase("@a")) {
            for (Player p : Bukkit.getOnlinePlayers()) executeJoinLeave(sender, p, action, center);
            return true;
        }

        Player pTarget = Bukkit.getPlayer(target);
        if (pTarget == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return true;
        }

        executeJoinLeave(sender, pTarget, action, center);
        return true;
    }

    private void executeJoinLeave(CommandSender sender, Player p, String action, boolean center) {
        if (action.equals("leave")) {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            p.sendMessage(ChatColor.GREEN + "Enviado al spawn del Overworld.");
            sender.sendMessage(ChatColor.GREEN + "Ejecutado para " + p.getName());
            return;
        }

        World world = Bukkit.getWorld(QuasoPlugin.WORLD_NAME);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "El mundo no está cargado.");
            return;
        }

        if (center) {
            Location location = portalManager.findCenterSpawn(world);
            portalManager.teleportToInfested(p, location);
            sender.sendMessage(ChatColor.GREEN + p.getName() + " enviado al centro de la Warden Cave (" + coords(location) + ").");
            return;
        }

        portalManager.findRandomSpawn(world).thenAccept(location -> {
            portalManager.teleportToInfested(p, location);
            sender.sendMessage(ChatColor.GREEN + p.getName() + " enviado a un lugar al azar de la Warden Cave (" + coords(location) + ").");
        });
    }

    private String coords(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
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
        } else if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
            completions.add("spawn");
        }

        return completions;
    }
}
