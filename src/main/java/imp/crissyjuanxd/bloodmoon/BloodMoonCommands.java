package imp.crissyjuanxd.bloodmoon;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Mantiene los comandos del ciclo y de las hordas y completa sus argumentos según el remitente. */
public final class BloodMoonCommands implements TabExecutor {
    private static final List<String> COMMANDS = List.of("show", "start", "stop", "reload", "spawnhorde");
    private final BloodMoon manager;
    public BloodMoonCommands(BloodMoon manager) { this.manager = manager; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!COMMANDS.contains(action)) {
            LocaleReader.MessageLocale("CommandNotFound", new String[]{"$d"}, new String[]{args[0]}, sender);
            return true;
        }
        if (!sender.hasPermission("bloodmoon.bloodmoon") || !sender.hasPermission("bloodmoon." + action)) {
            LocaleReader.MessageLocale("NoPermission", null, null, sender);
            return true;
        }
        if (action.equals("reload")) {
            manager.reload();
            LocaleReader.MessageLocale("PluginReloaded", null, null, sender);
            return true;
        }
        int parameter = sender instanceof Player ? 1 : 2;
        World world;
        if (sender instanceof Player player) world = player.getWorld();
        else {
            if (args.length < 2) { reply(sender, "Añade el nombre del mundo: /bloodmoon " + action + " <mundo>"); return true; }
            world = Bukkit.getWorld(args[1]);
        }
        BloodMoonActuator actuator = manager.getActuator(world);
        if (actuator == null) { LocaleReader.MessageLocale("NoBloodMoonInWorld", null, null, sender); return true; }
        ConfigReader config = manager.getConfigReader(world);
        switch (action) {
            case "show" -> {
                if (config.GetPermanentBloodMoonConfig()) LocaleReader.MessageLocale("WorldIsPermanentBloodMoon", null, null, sender);
                else if (actuator.isInProgress()) LocaleReader.MessageLocale("BloodMoonRightNow", null, null, sender);
                else LocaleReader.MessageLocale("DaysBeforeBloodMoon", new String[]{"$d"}, new String[]{String.valueOf(actuator.getCycle().remaining(world.getFullTime() / 24000))}, sender);
            }
            case "start" -> {
                if (actuator.isInProgress()) LocaleReader.MessageLocale("BloodMoonRightNow", null, null, sender);
                else { world.setTime(12001); actuator.StartBloodMoon(); reply(sender, "BloodMoon iniciada en " + world.getName() + "."); }
            }
            case "stop" -> {
                if (config.GetPermanentBloodMoonConfig()) LocaleReader.MessageLocale("CannotStopBloodMoon", null, null, sender);
                else {
                    actuator.StopBloodMoon(); world.setTime(0);
                    actuator.getCycle().stopped(world.getFullTime() / 24000, config.GetIntervalConfig());
                    manager.remember(actuator);
                    reply(sender, "BloodMoon detenida en " + world.getName() + ".");
                }
            }
            case "spawnhorde" -> {
                BloodMoonActuator.HordeResult result;
                if (args.length > parameter) {
                    Player target = Bukkit.getPlayerExact(args[parameter]);
                    if (target == null || !target.getWorld().equals(world)) {
                        LocaleReader.MessageLocale("NoPlayerOfName", new String[]{"$p", "$w"}, new String[]{args[parameter], world.getName()}, sender);
                        return true;
                    }
                    result = actuator.SpawnHorde(target);
                } else result = actuator.SpawnHorde();
                reply(sender, switch (result) {
                    case SPAWNED -> "Horda creada.";
                    case BLOCKED -> "La horda ha sido bloqueada por una protección.";
                    case NO_SAFE_LOCATION -> "No hay posiciones seguras para la horda.";
                    case DISABLED -> "Las hordas están deshabilitadas o no hay jugadores disponibles.";
                });
            }
            default -> help(sender);
        }
        return true;
    }
    private void reply(CommandSender sender, String message) { sender.sendMessage(LocaleReader.ORANGE + "Bloodmoon > " + LocaleReader.RED + message); }
    private void help(CommandSender sender) {
        String world = sender instanceof Player ? "" : " <mundo>";
        LocaleReader.MessageLocale("AllowedCommands", new String[]{"$d"}, new String[]{
                "show" + world + ", start" + world + ", stop" + world + ", reload, spawnhorde" + world + " [jugador]"}, sender);
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || !sender.hasPermission("bloodmoon.bloodmoon")) return List.of();
        if (args.length == 1) return matching(COMMANDS.stream().filter(value -> sender.hasPermission("bloodmoon." + value)).toList(), args[0]);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!COMMANDS.contains(action) || action.equals("reload") || !sender.hasPermission("bloodmoon." + action)) return List.of();
        boolean playerSender = sender instanceof Player;
        if (!playerSender && args.length == 2) return matching(manager.getWorlds().stream().map(World::getName).toList(), args[1]);
        int parameter = playerSender ? 1 : 2;
        if (args.length != parameter + 1) return List.of();
        if (action.equals("spawnhorde")) {
            World world = playerSender ? ((Player) sender).getWorld() : Bukkit.getWorld(args[1]);
            return world == null ? List.of() : matching(world.getPlayers().stream().map(Player::getName).toList(), args[parameter]);
        }
        return List.of();
    }
    private List<String> matching(List<String> values, String prefix) {
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
