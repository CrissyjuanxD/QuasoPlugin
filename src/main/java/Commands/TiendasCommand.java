package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TiendasCommand implements CommandExecutor {

    private final QuasoPlugin plugin;

    public TiendasCommand(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("tiendas_loc.world") || !config.contains("tiendas_loc.x")) {
            player.sendMessage(ChatColor.RED + "La zona de Tiendas no ha sido establecida. Contacta a un administrador.");
            return true;
        }

        String worldName = config.getString("tiendas_loc.world");
        double x = config.getDouble("tiendas_loc.x");
        double y = config.getDouble("tiendas_loc.y");
        double z = config.getDouble("tiendas_loc.z");
        float yaw = (float) config.getDouble("tiendas_loc.yaw", 0);
        float pitch = (float) config.getDouble("tiendas_loc.pitch", 0);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "El mundo de las Tiendas no existe o no está cargado.");
            return true;
        }

        Location tiendasLocation = new Location(world, x, y, z, yaw, pitch);

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.6f);

        player.sendTitle("§a§lZona de §2§lTiendas", "§7Viajando...", 20, 40, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(tiendasLocation);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, 80L);

        return true;
    }
}