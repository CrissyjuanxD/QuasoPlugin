package Habilidades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HabilidadesCommand implements CommandExecutor, TabCompleter {

    private final HabilidadesManager manager;
    private final HabilidadesEffects effects; // Necesario para refrescar efectos

    // Asegúrate de pasar Effects en el constructor desde el Main
    public HabilidadesCommand(HabilidadesManager manager, HabilidadesEffects effects) {
        this.manager = manager;
        this.effects = effects;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Comando ver (default)
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                showSkills(sender, target);
                return true;
            }
        }

        // Comandos admin: add/remove <jugador> <tipo> <nivel>
        if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("viciont.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(ChatColor.RED + "Uso: /habilidades " + args[0] + " <jugador> <TIPO> <nivel>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }

            HabilidadesType type;
            try {
                type = HabilidadesType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Tipo inválido (VITALIDAD, RESISTENCIA, AGILIDAD).");
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[3]);
                if (level < 1 || level > 4) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Nivel debe ser 1-4.");
                return true;
            }

            if (args[0].equalsIgnoreCase("add")) {
                manager.unlockHabilidad(target.getUniqueId(), type, level);
                sender.sendMessage(ChatColor.GREEN + "Habilidad añadida a " + target.getName());
                effects.reapplyAllEffects(target, manager);
            } else {
                manager.removeHabilidad(target.getUniqueId(), type, level);
                sender.sendMessage(ChatColor.GREEN + "Habilidad removida de " + target.getName());
                // Resetear y re-aplicar
                effects.reapplyAllEffects(target, manager);
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /habilidades <jugador>");
        if (sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "Uso admin: /habilidades <add/remove> <jugador> <TIPO> <nivel>");
        }
    }

    private void showSkills(CommandSender sender, Player target) {
        Map<HabilidadesType, List<Integer>> habilidades = manager.getPlayerHabilidades(target.getUniqueId());
        // ... (Tu código de mostrar habilidades existente va aquí) ...
        sender.sendMessage(ChatColor.of("#C77DFF") + "Habilidades de " + target.getName() + "...");
        // ...
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for(Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            if (sender.hasPermission("viciont.admin")) {
                list.add("add");
                list.add("remove");
            }
            return list;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> types = new ArrayList<>();
            for (HabilidadesType t : HabilidadesType.values()) types.add(t.name());
            return types;
        }
        if (args.length == 4) {
            return List.of("1", "2", "3", "4");
        }
        return new ArrayList<>();
    }
}