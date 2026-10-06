package BloodMoon;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/** Pausa solo el reloj diurno; advance_time también detendría la transición roja. */
final class BloodMoonDayClock {
    private final JavaPlugin plugin;
    private CommandSender sender;
    private boolean sharedClockNoticeShown;

    BloodMoonDayClock(JavaPlugin plugin) { this.plugin = plugin; }

    boolean setPaused(World world, boolean paused) {
        if (world.getKey() == null) return false;
        var server = plugin.getServer();
        var settings = server.spigot();
        var paper = settings == null ? null : settings.getPaperConfig();
        if (paper != null && paper.getBoolean("time.affects-all-worlds", false)) {
            if (paused && !sharedClockNoticeShown) {
                plugin.getLogger().warning("El amanecer fijo de BloodMoon requiere time.affects-all-worlds: false en Paper.");
                sharedClockNoticeShown = true;
            }
            return false;
        }
        if (sender == null) sender = server.createCommandSender(message -> {});
        if (sender == null) return false;
        return server.dispatchCommand(sender, "minecraft:execute in " + world.getKey()
                + " run minecraft:time of minecraft:overworld " + (paused ? "pause" : "resume"));
    }
}
