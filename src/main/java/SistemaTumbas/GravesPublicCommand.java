package SistemaTumbas;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GravesPublicCommand implements CommandExecutor, TabCompleter {
    private final GravesManager manager;

    public GravesPublicCommand(GravesManager manager) {
        this.manager = manager;
    }

    // /muertes lista tus tumbas (mundo, coordenadas y cuánto hace que moriste). Con el #id te tepea si la config lo
    // permite; si no, te dice dónde está para ir caminando
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        List<Grave> myGraves = new ArrayList<>();
        for (Grave g : manager.getGraves()) {
            if (g.getOwner().equals(player.getUniqueId())) myGraves.add(g);
        }
        myGraves.sort(Comparator.comparingLong(Grave::getCreationTime).reversed());

        if (myGraves.isEmpty()) {
            player.sendMessage(TumbaMessages.info("No tienes ninguna tumba activa."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(TumbaMessages.info("Tus tumbas (" + myGraves.size() + "):"));
            long now = System.currentTimeMillis();
            for (Grave g : myGraves) {
                Location loc = g.getLocation();
                String shortId = "#" + g.getId().toString().substring(0, 4);
                TextComponent line = new TextComponent(ChatColor.of("#CDB4DB") + " ✦ " + ChatColor.of("#FDE2E4") + worldName(loc.getWorld())
                        + ChatColor.of("#B8C0FF") + " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ()
                        + ChatColor.GRAY + " · hace " + ChatColor.WHITE + ModoTumba.reloj(now - g.getCreationTime())
                        + ChatColor.GRAY + " · " + state(g, now));
                String hover = manager.teleportsToGrave() ? "Clic para ir a la tumba " + shortId : "Ve caminando a X:" + loc.getBlockX()
                        + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
                line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " " + shortId));
                player.spigot().sendMessage(line);
            }
            return true;
        }

        String shortId = args[0].split(",")[0].replace("#", "");
        for (Grave g : myGraves) {
            if (!g.getId().toString().startsWith(shortId)) continue;
            Location loc = g.getLocation();
            if (manager.teleportsToGrave() && loc.getWorld() != null) {
                player.teleport(loc);
                player.sendMessage(TumbaMessages.success("Teletransportado a tu tumba."));
            } else {
                player.sendMessage(TumbaMessages.info("Tu tumba está en " + worldName(loc.getWorld()) + " X:" + loc.getBlockX() + " Y:"
                        + loc.getBlockY() + " Z:" + loc.getBlockZ() + ". Hay que ir caminando."));
            }
            return true;
        }
        player.sendMessage(TumbaMessages.error("Tumba no encontrada o ya expiró."));
        return true;
    }

    // Privada (y cuánto falta para que se abra), abierta, y cuánto le queda antes de soltar las cosas
    private String state(Grave grave, long now) {
        long untilOpen = manager.millisUntilOpen(grave);
        String left = ModoTumba.reloj(grave.getExpiryTime() - now);
        if (untilOpen > 0) return ChatColor.of("#B8C0FF") + "privada " + ModoTumba.reloj(untilOpen);
        if (untilOpen < 0) return ChatColor.of("#B8C0FF") + "privada" + ChatColor.GRAY + " · quedan " + left;
        return ChatColor.of("#E3B778") + "abierta" + ChatColor.GRAY + " · quedan " + left;
    }

    private static String worldName(World world) {
        if (world == null) return "?";
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> world.getName().equalsIgnoreCase("wardencave") ? "Warden Cave" : world.getName().equals("world") ? "Overworld" : world.getName();
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player) || args.length != 1) return result;
        for (Grave g : manager.getGraves()) {
            if (!g.getOwner().equals(player.getUniqueId())) continue;
            Location loc = g.getLocation();
            String option = "#" + g.getId().toString().substring(0, 4) + ",X:" + loc.getBlockX() + ",Y:" + loc.getBlockY() + ",Z:" + loc.getBlockZ();
            if (option.toLowerCase().startsWith(args[0].toLowerCase())) result.add(option);
        }
        return result;
    }
}
