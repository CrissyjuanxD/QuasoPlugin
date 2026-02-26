package TitleListener;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MagicTP implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public MagicTP(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Lógica de Viciont: Aceptamos 2, 4 o 5 argumentos
        if (args.length != 4 && args.length != 5 && args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso correcto:");
            sender.sendMessage(ChatColor.RED + "/magictp <jugador|@a> <x> <y> <z> [mundo]");
            sender.sendMessage(ChatColor.RED + "/magictp <jugador|@a> spawn");
            return true;
        }

        String targetName = args[0];
        boolean useSpawn = args.length == 2 && args[1].equalsIgnoreCase("spawn");

        // Procesar coordenadas y dimensión (Lógica Viciont mejorada)
        final Location targetLocation;
        if (!useSpawn && (args.length == 4 || args.length == 5)) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);

                World targetWorld;

                if (args.length == 5) {
                    // Si especifican mundo, lo buscamos
                    String worldName = args[4];
                    targetWorld = Bukkit.getWorld(worldName);

                    if (targetWorld == null) {
                        sender.sendMessage(ChatColor.RED + "El mundo '" + worldName + "' no existe.");
                        return true;
                    }
                } else {
                    // Si NO especifican mundo, usamos el del sender (si es jugador) o el default
                    if (sender instanceof Player) {
                        targetWorld = ((Player) sender).getWorld();
                    } else {
                        targetWorld = Bukkit.getWorlds().get(0);
                    }
                }

                targetLocation = new Location(targetWorld, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                return true;
            }
        } else {
            targetLocation = null;
        }

        // Obtener jugadores objetivo
        final List<Player> players = new ArrayList<>();
        if (targetName.equalsIgnoreCase("@a")) {
            players.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player player = Bukkit.getPlayer(targetName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "El jugador especificado no está en línea.");
                return true;
            }
            players.add(player);
        }

        // --- ESTÉTICA DE ISMANUSMP (Vanilla Friendly) ---
        for (Player player : players) {
            // Sonidos de amatista (Vanilla 1.21 compatible)
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.6f);

            // Título sin unicodes, solo colores estándar
            player.sendTitle("§b§lTepea§3§lndote§r§l...", "", 20, 40, 20);
        }

        // Tarea diferida (Delay de IsManuSMP: 80 ticks = 4 segundos)
        new BukkitRunnable() {
            @Override
            public void run() {
                World mainWorld = Bukkit.getWorlds().get(0);

                for (Player player : players) {
                    if (useSpawn) {
                        Location bedSpawn = player.getBedSpawnLocation();
                        if (bedSpawn != null) {
                            player.teleport(bedSpawn);
                        } else {
                            player.teleport(mainWorld.getSpawnLocation());
                        }
                    } else if (targetLocation != null) {
                        player.teleport(targetLocation);
                    }
                }
            }
        }.runTaskLater(plugin, 80); // Mantenemos los 80 ticks de IsManuSMP

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // TabCompleter de Viciont (Más completo)
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            completions.add("spawn");
            if (sender instanceof Player) {
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockX()));
            }
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("spawn")) {
            if (sender instanceof Player) {
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockY()));
            }
        } else if (args.length == 4 && !args[1].equalsIgnoreCase("spawn")) {
            if (sender instanceof Player) {
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockZ()));
            }
        } else if (args.length == 5 && !args[1].equalsIgnoreCase("spawn")) {
            // Sugerencia de mundos (Feature de Viciont)
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
        }

        return completions;
    }
}