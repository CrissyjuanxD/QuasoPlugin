package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MissionCommands implements CommandExecutor, TabCompleter {
    private static final long RESET_WINDOW = 30_000;

    private final MissionHandler missionHandler;
    private final MissionGUI missionGUI;
    // Quién pidió el reset y hasta cuándo puede confirmarlo
    private final Map<String, Long> pendingReset = new HashMap<>();

    public MissionCommands(MissionHandler missionHandler, MissionGUI missionGUI) {
        this.missionHandler = missionHandler;
        this.missionGUI = missionGUI;
    }

    // /misiones abre el menú; /missions es el de admin para activar, desactivar, dar, quitar y resetear misiones
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("misiones")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
                return true;
            }
            missionGUI.openMissionGUI(player);
            return true;
        }

        if (label.equalsIgnoreCase("missions") || label.equalsIgnoreCase("mission")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length == 0) {
                sendHelpMenu(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "activar" -> {
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions activar <número|todas>");
                    } else if (args[1].equalsIgnoreCase("todas")) {
                        missionHandler.activateAll(sender);
                    } else {
                        Integer number = parseNumber(sender, args[1]);
                        if (number != null) missionHandler.activateMission(sender, number);
                    }
                }
                case "desactivar" -> {
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions desactivar <número|todas>");
                    } else if (args[1].equalsIgnoreCase("todas")) {
                        missionHandler.deactivateAll(sender);
                    } else {
                        Integer number = parseNumber(sender, args[1]);
                        if (number != null) missionHandler.deactivateMission(sender, number);
                    }
                }
                case "complete" -> {
                    if (args.length != 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions complete <jugador> <número>");
                    } else {
                        Integer number = parseNumber(sender, args[2]);
                        if (number != null) missionHandler.addMissionToPlayer(sender, args[1], number);
                    }
                }
                case "remove" -> {
                    if (args.length != 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions remove <jugador> <número>");
                    } else {
                        Integer number = parseNumber(sender, args[2]);
                        if (number != null) missionHandler.removeMissionFromPlayer(sender, args[1], number);
                    }
                }
                case "reset" -> handleReset(sender, args);
                case "savedata" -> {
                    sender.sendMessage(ChatColor.YELLOW + "Forzando guardado de datos de misiones...");
                    missionHandler.autoSaveAll();
                    sender.sendMessage(ChatColor.GREEN + "Datos guardados exitosamente en la base de datos.");
                }
                default -> sendHelpMenu(sender);
            }
            return true;
        }

        return false;
    }

    // Acepta 1 (normal), 1ex (la extra de la misión 1) y 170tra (de trabajo); también el número de siempre
    private Integer parseNumber(CommandSender sender, String text) {
        int number = missionHandler.parse(text);
        if (number < 0) {
            sender.sendMessage(ChatColor.RED + "Esa misión no existe. Usa el número (1), la extra con ex (1ex) o la de trabajo con tra (170tra).");
            return null;
        }
        return number;
    }

    // Primero avisa lo que borra; recién con /missions reset confirmar (dentro de 30 segundos) lo hace
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirmar")) {
            Long until = pendingReset.remove(sender.getName());
            if (until == null || System.currentTimeMillis() > until) {
                sender.sendMessage(ChatColor.RED + "No hay ningún reset pendiente o ya pasaron los 30 segundos. Usa /missions reset primero.");
                return;
            }
            missionHandler.resetAll(sender);
            return;
        }

        pendingReset.put(sender.getName(), System.currentTimeMillis() + RESET_WINDOW);
        sender.sendMessage(ChatColor.RED + "⚠ " + ChatColor.of("#FFA07A") + "Esto desactiva todas las misiones y borra "
                + ChatColor.RED + "todos" + ChatColor.of("#FFA07A") + " los datos de misiones de la base de datos (progreso, completadas y recompensas).");

        if (sender instanceof Player player) {
            TextComponent confirm = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[CONFIRMAR RESET]");
            confirm.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/missions reset confirmar"));
            confirm.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Borra todo, no tiene vuelta atrás")));
            player.spigot().sendMessage(new ComponentBuilder(ChatColor.GRAY + "Tienes 30 segundos: ").append(confirm).create());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Escribe /missions reset confirmar en los próximos 30 segundos para continuar.");
        }
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Menú de Administración de Misiones ===");
        sender.sendMessage(ChatColor.GRAY + "Las misiones se escriben 1 (normal), 1ex (la extra de la 1) o 170tra (de trabajo).");
        sender.sendMessage(ChatColor.YELLOW + "/missions activar <número|todas> " + ChatColor.GRAY + "- Activa una misión (con sus extras) o todas.");
        sender.sendMessage(ChatColor.YELLOW + "/missions desactivar <número|todas> " + ChatColor.GRAY + "- Desactiva una misión (con sus extras) o todas.");
        sender.sendMessage(ChatColor.YELLOW + "/missions complete <jugador> <número> " + ChatColor.GRAY + "- Completa forzosamente una misión a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/missions remove <jugador> <número> " + ChatColor.GRAY + "- Reinicia la misión a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/missions reset " + ChatColor.GRAY + "- Desactiva todo y borra los datos de misiones (pide confirmación).");
        sender.sendMessage(ChatColor.YELLOW + "/missions savedata " + ChatColor.GRAY + "- Guarda los datos de todos a la Base de Datos.");
    }

    // Las normales (1), sus extras (1ex) y las de trabajo (170tra); las de trabajo no se activan porque siempre lo están
    private List<String> tokens(boolean withJobs) {
        List<String> normal = new ArrayList<>();
        List<String> extras = new ArrayList<>();
        List<String> jobs = new ArrayList<>();
        for (int number : missionHandler.getMissions().keySet()) {
            switch (missionHandler.tipo(number)) {
                case NORMAL -> normal.add(missionHandler.token(number));
                case EXTRA -> extras.add(missionHandler.token(number));
                case TRABAJO -> {
                    if (withJobs) jobs.add(missionHandler.token(number));
                }
            }
        }
        extras.sort(java.util.Comparator.comparingInt(token -> Integer.parseInt(token.replace(TipoMision.EXTRA.sufijo, ""))));
        normal.addAll(extras);
        normal.addAll(jobs);
        return normal;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("missions") || command.getName().equalsIgnoreCase("mission")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("activar", "desactivar", "complete", "remove", "reset", "savedata"));
            } else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("activar") || sub.equals("desactivar")) {
                    completions.add("todas");
                    completions.addAll(tokens(false));
                } else if (sub.equals("complete") || sub.equals("remove")) {
                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                } else if (sub.equals("reset")) {
                    completions.add("confirmar");
                }
            } else if (args.length == 3) {
                String sub = args[0].toLowerCase();
                if (sub.equals("complete") || sub.equals("remove")) completions.addAll(tokens(true));
            }
        }

        if (!args[args.length - 1].isEmpty()) {
            List<String> filtered = new ArrayList<>();
            StringUtil.copyPartialMatches(args[args.length - 1], completions, filtered);
            return filtered;
        }

        return completions;
    }
}
