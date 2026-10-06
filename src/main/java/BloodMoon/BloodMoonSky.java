package BloodMoon;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Activa una capa visual del datapack sin cambiar el reloj del día ni los biomas. */
public final class BloodMoonSky {
    private static final String CLOCK = "quaso:bloodmoon";
    private final JavaPlugin plugin;
    private final Map<UUID, Boolean> states = new HashMap<>();
    private Boolean registered;
    private boolean sharedClocks;
    private CommandSender sender;
    private boolean sharedClockNoticeShown;

    public BloodMoonSky(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Los registros se cargan al arrancar. La consulta no ejecuta un comando inválido si falta el datapack. */
    public boolean isAvailable() {
        return isClockRegistered() && !sharedClocks;
    }

    private boolean isClockRegistered() {
        if (registered != null) return registered;
        registered = false;
        Server server = plugin.getServer();
        if (server == null) return false;
        try {
            Server.Spigot settings = server.spigot();
            YamlConfiguration paper = settings == null ? null : settings.getPaperConfig();
            sharedClocks = paper != null && paper.getBoolean("time.affects-all-worlds", false);
            if (sharedClocks) {
                if (!sharedClockNoticeShown) {
                    plugin.getLogger().warning("El cielo rojo de BloodMoon requiere time.affects-all-worlds: false en Paper; se mantiene el clima normal.");
                    sharedClockNoticeShown = true;
                }
            }
        } catch (RuntimeException ignored) {
            // Esta API de compatibilidad no existe en todos los servidores Bukkit.
        }
        CommandMap commands = server.getCommandMap();
        if (commands == null) return false;
        Command time = commands.getCommand("minecraft:time");
        if (time == null) return false;
        try {
            // VanillaCommandWrapper solo sobrescribe el overload con Location; el de tres argumentos lista jugadores.
            List<String> clocks = time.tabComplete(server.getConsoleSender(), "minecraft:time", new String[]{"of", ""}, null);
            registered = clocks.contains(CLOCK);
        } catch (RuntimeException ignored) {
            // Un servidor sin la sintaxis de relojes o sin el pack conserva la BloodMoon normal.
        }
        return registered;
    }

    public boolean syncWorld(World world, boolean active) {
        if (world == null || world.getEnvironment() != World.Environment.NORMAL || world.getKey() == null || !isClockRegistered()) return false;
        // Incluso con relojes globales hay que apagar y pausar el propio: uno recién registrado corre por defecto.
        if (active && sharedClocks) return false;
        Boolean previous = states.get(world.getUID());
        if (previous != null && previous == active) return true;
        Server server = plugin.getServer();
        if (sender == null) sender = server.createCommandSender(message -> {});
        if (sender == null) return false;
        String prefix = "minecraft:execute in " + world.getKey() + " run minecraft:time of " + CLOCK + " ";
        try {
            // El reloj propio queda fijo; el tiempo del mundo, las fases lunares y los spawns no cambian.
            if (!server.dispatchCommand(sender, prefix + "pause")
                    || !server.dispatchCommand(sender, prefix + "set " + (active ? 1 : 0))) return false;
            states.put(world.getUID(), active);
            return true;
        } catch (RuntimeException ignored) {
            registered = false;
            states.clear();
            return false;
        }
    }

    public boolean isActive(World world) {
        return world != null && Boolean.TRUE.equals(states.get(world.getUID()));
    }

    public void clearWorld(World world) {
        syncWorld(world, false);
        if (world != null) states.remove(world.getUID());
    }

    public void refreshAvailability() {
        registered = null;
        sharedClocks = false;
        states.clear();
    }
}
