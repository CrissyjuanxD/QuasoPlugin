package SistemaTumbas;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GravesPublicCommand implements CommandExecutor, TabCompleter {
    private final GravesManager manager;

    public GravesPublicCommand(GravesManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        List<Grave> myGraves = new ArrayList<>();
        for (Grave g : manager.getGraves()) {
            if (g.getOwner().equals(player.getUniqueId())) {
                myGraves.add(g);
            }
        }

        if (myGraves.isEmpty()) {
            player.sendMessage("§cNo tienes ninguna tumba activa en este momento.");
            return true;
        }

        // Si mandaron el comando vacío -> Mostrar Lista
        if (args.length == 0) {
            player.sendMessage("§aTus tumbas activas (" + myGraves.size() + "):");
            for (Grave g : myGraves) {
                long remaining = g.getExpiryTime() - System.currentTimeMillis();
                long mins = (remaining / 1000) / 60;
                long secs = (remaining / 1000) % 60;
                String shortId = "#" + g.getId().toString().substring(0, 4);

                player.sendMessage(String.format("§7- %s §7[X:%d Y:%d Z:%d] §c⏱ %02d:%02d",
                        shortId, g.getLocation().getBlockX(), g.getLocation().getBlockY(), g.getLocation().getBlockZ(), mins, secs));
            }
            return true;
        }

        // Si mandaron un argumento autocompletado (Teletransporte)
        String input = args[0];
        if (input.startsWith("#")) {
            String shortId = input.split(",")[0].replace("#", "");

            for (Grave g : myGraves) {
                if (g.getId().toString().startsWith(shortId)) {
                    player.teleport(g.getLocation());
                    player.sendMessage("§aTeletransportado a tu tumba.");
                    return true;
                }
            }
            player.sendMessage("§cTumba no encontrada o ya expiró.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) return completions;

        if (args.length == 1) {
            for (Grave g : manager.getGraves()) {
                if (g.getOwner().equals(player.getUniqueId())) {
                    String shortId = "#" + g.getId().toString().substring(0, 4);
                    int x = g.getLocation().getBlockX();
                    int y = g.getLocation().getBlockY();
                    int z = g.getLocation().getBlockZ();

                    completions.add(shortId + ",X:" + x + ",Y:" + y + ",Z:" + z);
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(args[0].toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }
}