package Hologramas;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

// /holograma [crear|editar <nombre>|posicion <nombre>|guia|recargar]: sin nada abre el menú. Solo admins
public class HologramasCommand implements CommandExecutor, TabCompleter {

    private final HologramasManager manager;
    private final HologramasDialogos dialogos;

    public HologramasCommand(HologramasManager manager, HologramasDialogos dialogos) {
        this.manager = manager;
        this.dialogos = dialogos;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(HologramasManager.PERMISO)) {
            sender.sendMessage(ChatColor.RED + "Solo los admins pueden usar los hologramas.");
            return true;
        }
        String accion = args.length == 0 ? "" : args[0].toLowerCase();
        if (accion.equals("recargar")) {
            manager.cargar();
            sender.sendMessage(ChatColor.GREEN + "hologramas.yml recargado: " + manager.todos().size() + " hologramas.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Los hologramas se editan en un diálogo: usa /holograma dentro del juego.");
            return true;
        }
        Holograma holograma = args.length >= 2 ? manager.get(args[1]) : null;
        switch (accion) {
            case "crear" -> dialogos.crear(player, null);
            case "guia" -> dialogos.guia(player);
            case "editar", "posicion" -> {
                if (holograma == null) dialogos.principal(player, ChatColor.RED + "No hay un holograma con ese nombre.");
                else if (accion.equals("editar")) dialogos.menu(player, holograma, null);
                else dialogos.posicion(player, holograma, null);
            }
            default -> dialogos.principal(player, null);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> opciones = new ArrayList<>();
        if (!sender.hasPermission(HologramasManager.PERMISO)) return opciones;
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("crear", "editar", "posicion", "guia", "recargar"), opciones);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("editar") || args[0].equalsIgnoreCase("posicion"))) {
            List<String> ids = manager.todos().stream().map(Holograma::id).toList();
            StringUtil.copyPartialMatches(args[1], ids, opciones);
        }
        return opciones;
    }
}
