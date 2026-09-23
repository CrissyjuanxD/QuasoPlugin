package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final QuasoPlugin plugin;

    public SetSpawnCommand(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Verificar permisos
        if (!player.isOp() && !player.hasPermission("viciont.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) return true; // Seguridad por si el mundo es nulo

        // 1. Guardar en la config de TU plugin (QuasoPlugin)
        // Guardamos decimales exactos para el TP suave del plugin
        plugin.getConfig().set("spawn.world", world.getName());
        plugin.getConfig().set("spawn.x", loc.getX());
        plugin.getConfig().set("spawn.y", loc.getY());
        plugin.getConfig().set("spawn.z", loc.getZ());
        plugin.getConfig().set("spawn.yaw", loc.getYaw());
        plugin.getConfig().set("spawn.pitch", loc.getPitch());

        plugin.saveConfig();

        // 2. Establecer el World Spawn Vanilla (Igual que /setworldspawn)
        // Esto usa coordenadas de bloque (enteros), por eso usamos getBlockX/Y/Z
        world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // Mensajes de confirmación
        player.sendMessage(ChatColor.GREEN + "El Spawn del servidor ha sido establecido.");
        player.sendMessage(ChatColor.AQUA + "También se ha actualizado el 'World Spawn' vanilla.");
        player.sendMessage(ChatColor.GRAY + "Coordenadas: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        return true;
    }
}