package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBossSpawnCommand implements CommandExecutor {

    private final QuasoPlugin plugin;

    public SetBossSpawnCommand(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp() && !player.hasPermission("viciont.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) return true;

        plugin.getConfig().set("boss_spawn.world", world.getName());
        plugin.getConfig().set("boss_spawn.x", loc.getX());
        plugin.getConfig().set("boss_spawn.y", loc.getY());
        plugin.getConfig().set("boss_spawn.z", loc.getZ());
        plugin.getConfig().set("boss_spawn.yaw", loc.getYaw());
        plugin.getConfig().set("boss_spawn.pitch", loc.getPitch());

        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "La zona del Boss ha sido establecida correctamente.");
        player.sendMessage(ChatColor.GRAY + "Coordenadas: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        return true;
    }
}