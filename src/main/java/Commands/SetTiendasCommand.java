package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetTiendasCommand implements CommandExecutor {

    private final QuasoPlugin plugin;

    public SetTiendasCommand(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Verificar permisos (Mismos que tu setspawn)
        if (!player.isOp() && !player.hasPermission("viciont.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) return true;

        // Guardar en la config bajo la sección "tiendas_loc"
        plugin.getConfig().set("tiendas_loc.world", world.getName());
        plugin.getConfig().set("tiendas_loc.x", loc.getX());
        plugin.getConfig().set("tiendas_loc.y", loc.getY());
        plugin.getConfig().set("tiendas_loc.z", loc.getZ());
        plugin.getConfig().set("tiendas_loc.yaw", loc.getYaw());
        plugin.getConfig().set("tiendas_loc.pitch", loc.getPitch());

        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "La zona de las Tiendas ha sido establecida correctamente.");
        player.sendMessage(ChatColor.GRAY + "Coordenadas: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        return true;
    }
}