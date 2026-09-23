package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnCommand implements CommandExecutor {

    private final QuasoPlugin plugin;

    public SpawnCommand(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    // Tepea al spawn guardado en el config después de 4 segundos
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("spawn.world") || !config.contains("spawn.x")) {
            player.sendMessage(ChatColor.RED + "El Spawn no ha sido establecido. Contacta a un administrador.");
            return true;
        }

        String worldName = config.getString("spawn.world");
        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw", 0);
        float pitch = (float) config.getDouble("spawn.pitch", 0);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "El mundo del spawn no existe o no está cargado.");
            return true;
        }

        Location spawnLocation = new Location(world, x, y, z, yaw, pitch);

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.6f);

        player.sendTitle("§b§lTepea§3§lndote§r§l...", "", 20, 40, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(spawnLocation);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, 80L);

        return true;
    }
}