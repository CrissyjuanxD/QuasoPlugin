package BloodMoon;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Activa una capa visual del datapack sin cambiar el reloj del día ni los biomas. */
public final class BloodMoonSky {
    private static final String CLOCK = "quaso:bloodmoon";
    private final JavaPlugin plugin;
    // La timeline sube de 0 a 200 y baja de 200 a 400; no toca el reloj del día.
    public static final int FADE_TICKS = 200;
    private final Map<UUID, Transition> states = new HashMap<>();

    private static final class Transition {
        final World world;
        final boolean active;
        final int from;
        final long started;
        BukkitTask task;

        Transition(World world, boolean active, int from) {
            this.world = world;
            this.active = active;
            this.from = from;
            started = world.getGameTime();
        }

        int strength() {
            long elapsed = Math.max(0, world.getGameTime() - started);
            return (int) Math.clamp(active ? from + elapsed : from - elapsed, 0, FADE_TICKS);
        }

        void cancel() { if (task != null) task.cancel(); }
    }
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
        if (!supported(world)) return false;
        // Los relojes globales no permiten aplicar el efecto solo al mundo elegido.
        if (sharedClocks) { clearWorld(world); return !active; }
        Transition previous = states.get(world.getUID());
        if (previous != null && previous.active == active) return true;
        int strength = previous == null ? 0 : previous.strength();
        if (previous != null) previous.cancel();
        states.remove(world.getUID());
        long duration = active ? FADE_TICKS - strength : strength;
        int position = duration == 0 ? (active ? FADE_TICKS : 0)
                : (active ? strength : 2 * FADE_TICKS - strength);
        if (!command(world, "pause") || !command(world, "set " + position)
                || (duration > 0 && !command(world, "resume"))) {
            command(world, "pause");
            command(world, "set 0");
            return false;
        }
        Transition transition = new Transition(world, active, strength);
        states.put(world.getUID(), transition);
        if (duration > 0) {
            transition.task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (states.get(world.getUID()) != transition) return;
                // Fija exactamente el extremo, aunque el scheduler y el reloj difieran un tick.
                if (!command(world, "pause") || !command(world, "set " + (active ? FADE_TICKS : 0))) {
                    clearWorld(world);
                    return;
                }
                states.put(world.getUID(), new Transition(world, active, active ? FADE_TICKS : 0));
            }, duration);
        }
        return true;
    }

    private boolean supported(World world) {
        return world != null && world.getEnvironment() == World.Environment.NORMAL
                && world.getKey() != null && isClockRegistered();
    }

    private boolean command(World world, String argument) {
        Server server = plugin.getServer();
        if (sender == null) sender = server.createCommandSender(message -> {});
        if (sender == null) return false;
        try {
            return server.dispatchCommand(sender, "minecraft:execute in " + world.getKey()
                    + " run minecraft:time of " + CLOCK + " " + argument);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public boolean isActive(World world) {
        Transition state = world == null ? null : states.get(world.getUID());
        return state != null && state.active;
    }

    float strength(World world) {
        Transition state = states.get(world.getUID());
        return state == null ? 0 : state.strength() / (float) FADE_TICKS;
    }

    /** Al cargar, descargar o apagar, no puede quedar un fade ni un reloj rojo huérfano. */
    public void clearWorld(World world) {
        if (world == null) return;
        Transition previous = states.remove(world.getUID());
        if (previous != null) previous.cancel();
        if (supported(world)) {
            command(world, "pause");
            command(world, "set 0");
        }
    }

    public void shutdown() {
        for (Transition transition : List.copyOf(states.values())) clearWorld(transition.world);
    }

    public void refreshAvailability() {
        registered = null;
        sharedClocks = false;
        // Conserva el progreso: recargar o invertir la transición no debe dar un salto de color.
    }
}
