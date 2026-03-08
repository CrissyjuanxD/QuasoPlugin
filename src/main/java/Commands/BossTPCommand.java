package Commands;

import Events.MissionSystem.MissionHandler;
import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor; // Importante para colores HEX
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

public class BossTPCommand implements CommandExecutor {

    private final QuasoPlugin plugin;
    private final MissionHandler missionHandler; // Cambiamos DayHandler por MissionHandler

    // Actualizamos el constructor
    public BossTPCommand(QuasoPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!missionHandler.getActiveMissions().contains(10)) {
            String hexColor = "#DC9567";

            String message = ChatColor.RED + "" + ChatColor.BOLD + "\u06de " +
                    ChatColor.of(hexColor) + "" + ChatColor.BOLD + "Este comando aun no se puede ejecutar hasta que se habilite";

            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }
        // -----------------------------------

        FileConfiguration config = plugin.getConfig();

        if (!config.contains("boss_spawn.world") || !config.contains("boss_spawn.x")) {
            player.sendMessage(ChatColor.RED + "La zona del Boss no ha sido establecida. Contacta a un administrador.");
            return true;
        }

        String worldName = config.getString("boss_spawn.world");
        double x = config.getDouble("boss_spawn.x");
        double y = config.getDouble("boss_spawn.y");
        double z = config.getDouble("boss_spawn.z");
        float yaw = (float) config.getDouble("boss_spawn.yaw", 0);
        float pitch = (float) config.getDouble("boss_spawn.pitch", 0);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "El mundo del Boss no existe o no está cargado.");
            return true;
        }

        Location bossLocation = new Location(world, x, y, z, yaw, pitch);


        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 0.6f);

        player.sendTitle("§c§lBoss §4§lArena", "§7Viajando...", 20, 40, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(bossLocation);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, 80L);

        return true;
    }
}