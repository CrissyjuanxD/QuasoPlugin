package Pesca;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FishingCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "[Pesca] " + ChatColor.RESET;

    private final QuasoPlugin plugin;
    private final FishingZoneManager zoneManager;
    private final FishingWandListener wandListener;

    public FishingCommand(QuasoPlugin plugin, FishingZoneManager zoneManager, FishingWandListener wandListener) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
        this.wandListener = wandListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /pesca give ──────────────────────────────────────────────────
            case "give" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores pueden usar este comando.");
                    return true;
                }
                if (!player.hasPermission("pesca.admin")) {
                    player.sendMessage(PREFIX + ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                player.getInventory().addItem(FishingWandListener.createWand());
                player.sendMessage(PREFIX + ChatColor.GRAY + "Recibiste la " + ChatColor.of("#61B1F2") + "Vara de Pesca" + ChatColor.GRAY + ".");
                player.sendMessage(ChatColor.GRAY + "Click Izquierdo → POS1 | Click Derecho → POS2");
            }

            // ── /pesca set <nombre> ──────────────────────────────────────────
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores pueden usar este comando.");
                    return true;
                }
                if (!player.hasPermission("pesca.admin")) {
                    player.sendMessage(PREFIX + ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Uso: /pesca set <nombre>");
                    return true;
                }
                String name = args[1];
                if (!wandListener.hasBothPositions(player.getUniqueId())) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Necesitas seleccionar POS1 y POS2 primero con la Vara de Pesca.");
                    player.sendMessage(ChatColor.GRAY + "Usa /pesca give para obtenerla.");
                    return true;
                }

                boolean saved = zoneManager.registerZone(
                        name,
                        wandListener.getPos1(player.getUniqueId()),
                        wandListener.getPos2(player.getUniqueId())
                );

                if (saved) {
                    wandListener.clearPositions(player.getUniqueId());
                    player.sendMessage(PREFIX + ChatColor.of("#4BA3DD") + "Zona " + ChatColor.WHITE + ChatColor.BOLD + name
                            + ChatColor.of("#4BA3DD") + " guardada correctamente en pesca.yml.");
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "Error al guardar la zona. ¿Las posiciones son del mismo mundo?");
                }
            }

            // ── /pesca removezone <nombre> ───────────────────────────────────
            case "removezone" -> {
                if (!sender.hasPermission("pesca.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Uso: /pesca removezone <nombre>");
                    return true;
                }
                String name = args[1];
                if (zoneManager.removeZone(name)) {
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "La zona " + ChatColor.WHITE + name + ChatColor.GREEN + " ha sido eliminada.");
                } else {
                    sender.sendMessage(PREFIX + ChatColor.RED + "La zona '" + name + "' no existe.");
                }
            }

            // ── /pesca reload ────────────────────────────────────────────────
            case "reload" -> {
                if (!sender.hasPermission("pesca.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                zoneManager.load();
                sender.sendMessage(PREFIX + ChatColor.of("#4BA3DD") + "pesca.yml recargado. Zonas activas: "
                        + ChatColor.WHITE + zoneManager.getZones().size());
            }

            // ── /pesca list ──────────────────────────────────────────────────
            case "list" -> {
                if (!sender.hasPermission("pesca.admin")) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                if (zoneManager.getZones().isEmpty()) {
                    sender.sendMessage(PREFIX + ChatColor.GRAY + "No hay zonas configuradas.");
                } else {
                    sender.sendMessage(PREFIX + ChatColor.of("#4BA3DD") + "Zonas de pesca:");
                    zoneManager.getZonesMap().forEach((k, v) ->
                            sender.sendMessage(ChatColor.GRAY + "  · " + ChatColor.WHITE + v.getName()
                                    + ChatColor.GRAY + " (" + v.getPos1().getWorld().getName() + ")")
                    );
                }
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "set", "removezone", "reload", "list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("removezone")) {
            return new ArrayList<>(zoneManager.getZonesMap().keySet());
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.of("#4BA3DD") + "Comandos disponibles:");
        sender.sendMessage(ChatColor.GRAY + "  /pesca give            " + ChatColor.WHITE + "→ Obtener la vara de selección");
        sender.sendMessage(ChatColor.GRAY + "  /pesca set <nombre>    " + ChatColor.WHITE + "→ Guardar zona");
        sender.sendMessage(ChatColor.GRAY + "  /pesca removezone <nombre>" + ChatColor.WHITE + "→ Eliminar zona");
        sender.sendMessage(ChatColor.GRAY + "  /pesca reload          " + ChatColor.WHITE + "→ Recargar pesca.yml");
        sender.sendMessage(ChatColor.GRAY + "  /pesca list            " + ChatColor.WHITE + "→ Ver zonas activas");
        sender.sendMessage("");
    }
}