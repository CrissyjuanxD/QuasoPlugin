package Commands;

import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpawnMobs implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CustomBoat customBoat;
    private final InfestedBeeHandler infestedBeeHandler;
    private final CorruptedBee corruptedBee;
    private final DayHandler dayHandler;

    public SpawnMobs(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin, dayHandler);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.dayHandler = dayHandler;
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.customBoat = new CustomBoat(plugin);
        this.infestedBeeHandler = new InfestedBeeHandler(plugin);
        this.corruptedBee = new CorruptedBee(plugin);
        plugin.getCommand("spawnqp").setExecutor(this);
        plugin.getCommand("spawnqp").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /spawnqp <mob> [jugador (opcional)] [x] [y] [z]");
            return true;
        }

        String mobType = args[0].toLowerCase();

        Location location = null;
        Player targetPlayer = null;

        if (args.length > 1 && Bukkit.getPlayer(args[1]) != null) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            location = targetPlayer.getLocation();
        } else if (args.length >= 4) {
            try {
                World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
                double x = Double.parseDouble(args[args.length - 3]);
                double y = Double.parseDouble(args[args.length - 2]);
                double z = Double.parseDouble(args[args.length - 1]);
                location = new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("Las coordenadas deben ser números válidos.");
                return true;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
            location = targetPlayer.getLocation();
        } else {
            sender.sendMessage("Debes especificar un jugador o coordenadas si no eres un jugador.");
            return true;
        }

        switch (mobType) {
            case "bombita":
                bombitaSpawner.spawnBombita(location);
                sender.sendMessage("¡Bombita ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "iceologer":
                iceologerSpawner.spawnIceologer(location);
                sender.sendMessage("¡Iceologer ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedzombie":
                corruptedZombieSpawner.spawnCorruptedZombie(location);
                sender.sendMessage("¡Corrupted Zombie ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedspider":
                corruptedSpider.spawnCorruptedSpider(location);
                sender.sendMessage("¡Corrupted Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "queenbee":
                QueenBeeHandler.spawn(plugin, location);
                sender.sendMessage("§d¡Corrupted Queen Bee ha sido invocada en " + locationToString(location) + "!");
                break;

            case "guardianblaze":
                guardianBlaze.spawnGuardianBlaze(location);
                sender.sendMessage("¡Guardian Blaze ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "guardiancorruptedskeleton":
                guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location);
                sender.sendMessage("¡Guardian Corrupted Skeleton ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedinfernalspider":
                corruptedInfernalSpider.spawnCorruptedInfernalSpider(location);
                sender.sendMessage("¡Corrupted Infernal Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "infestedbee":
                infestedBeeHandler.spawnInfestedBee(location);
                sender.sendMessage("¡Infested Bee ha sido spawneada en " + locationToString(location) + "!");
                break;

            case "estatuarecompensa":
                Estatua_Reward.spawn(location);
                sender.sendMessage("¡Estatua de Zorro ha sido spawneada en " + locationToString(location) + "!");
                break;

            case "corruptedbee":
                corruptedBee.spawnCorruptedBee(location);
                sender.sendMessage("¡Corrupted Bee ha sido spawneada en " + locationToString(location) + "!");
                break;

            default:
                sender.sendMessage("Mob no reconocido. Usa /spawnvct <bombita|iceologer|corruptedzombie|corruptedspider|queenbee>");
                break;
        }

        return true;
    }

    private String locationToString(Location location) {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("bombita");
            suggestions.add("iceologer");
            suggestions.add("corruptedzombie");
            suggestions.add("corruptedspider");
            suggestions.add("queenbee");
            suggestions.add("guardianblaze");
            suggestions.add("guardiancorruptedskeleton");
            suggestions.add("corruptedinfernalspider");
            suggestions.add("infestedbee");
            suggestions.add("estatuarecompensa");
            suggestions.add("corruptedbee");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 3 || args.length == 4 || args.length == 5) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                suggestions.add(String.valueOf(player.getLocation().getBlockX()));
                suggestions.add(String.valueOf(player.getLocation().getBlockY()));
                suggestions.add(String.valueOf(player.getLocation().getBlockZ()));
            }
        }

        return suggestions;
    }
}
