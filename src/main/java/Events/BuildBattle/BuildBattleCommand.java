package Events.BuildBattle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BuildBattleCommand implements CommandExecutor, TabCompleter {

    private final BuildBattleHandler handler;

    public BuildBattleCommand(BuildBattleHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) {
            p.sendMessage("§cUsa: /buildbattle <start|end|tpzona|forcetp|startbuild|skipbuild|reglas|...>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "addvotador":
                if (args.length < 2) return true;
                if (handler.isVotante(args[1])) {
                    p.sendMessage("§cEse jugador ya tiene el rol de votante.");
                    return true;
                }
                handler.addVotador(args[1]);
                p.sendMessage("§aVotante añadido: " + args[1]);
                break;

            case "removevotador":
                if (args.length < 2) return true;
                if (!handler.isVotante(args[1])) {
                    p.sendMessage("§cNo se puede remover: Ese jugador no es un votante activo.");
                    return true;
                }
                handler.removeVotador(args[1]);
                p.sendMessage("§cVotante removido: " + args[1]);
                break;

            case "addadmin":
                if (args.length < 2) return true;
                if (handler.isAdmin(args[1])) {
                    p.sendMessage("§cEse jugador ya tiene el rol de admin.");
                    return true;
                }
                handler.addAdmin(args[1]);
                p.sendMessage("§aAdmin añadido: " + args[1]);
                break;

            case "removeadmin":
                if (args.length < 2) return true;
                if (!handler.isAdmin(args[1])) {
                    p.sendMessage("§cNo se puede remover: Ese jugador no es un admin del evento.");
                    return true;
                }
                handler.removeAdmin(args[1]);
                p.sendMessage("§cAdmin removido: " + args[1]);
                break;

            case "start":
                if (handler.isEventoIniciado()) {
                    p.sendMessage("§cEl evento ya ha sido iniciado previamente.");
                    return true;
                }
                handler.iniciarEvento();
                p.sendMessage("§aEvento Build Battle iniciado correctamente.");
                break;

            case "reglas":
                handler.mostrarReglas();
                break;

            case "addplayer":
                if (args.length < 2) return true;
                if (handler.isParticipante(args[1])) {
                    p.sendMessage("§cEse jugador ya tiene el rol de participante.");
                    return true;
                }
                handler.addPlayerEnMedio(args[1]);
                p.sendMessage("§aJugador añadido como participante: " + args[1]);
                break;

            case "removeplayer":
                if (args.length < 2) return true;
                if (!handler.isParticipante(args[1])) {
                    p.sendMessage("§cNo se puede remover: Ese jugador no es un participante.");
                    return true;
                }
                handler.removePlayerEnMedio(args[1]);
                p.sendMessage("§cJugador participante removido: " + args[1]);
                break;

            case "tpzona":
                if (!handler.isEventoIniciado()) {
                    p.sendMessage("§cEl evento no ha sido iniciado (usa /buildbattle start primero).");
                    return true;
                }
                handler.tpZona();
                break;

            case "forcetp":
                handler.forceTp();
                break;

            case "stick":
                ItemStack stick = new ItemStack(Material.STICK);
                ItemMeta meta = stick.getItemMeta();
                meta.setDisplayName("§d§lVarita de Parcelas");
                meta.setLore(Arrays.asList("§7Click Izquierdo: §aSeleccionar Posición 1", "§7Click Derecho: §aSeleccionar Posición 2"));
                stick.setItemMeta(meta);
                p.getInventory().addItem(stick);
                p.sendMessage("§eVarita de selección entregada.");
                break;

            case "addparcela":
                if (args.length < 2) { p.sendMessage("§cUso: /buildbattle addparcela <id>"); return true; }
                if (handler.pos1Map.containsKey(p.getUniqueId()) && handler.pos2Map.containsKey(p.getUniqueId())) {
                    handler.saveParcel(Integer.parseInt(args[1]), handler.pos1Map.get(p.getUniqueId()), handler.pos2Map.get(p.getUniqueId()));
                    p.sendMessage("§aParcela " + args[1] + " guardada exitosamente.");
                } else {
                    p.sendMessage("§cDebes seleccionar Pos1 y Pos2 con la varita primero.");
                }
                break;

            case "removeparcela":
                if (args.length < 2) return true;
                handler.removeParcel(Integer.parseInt(args[1]));
                p.sendMessage("§cParcela " + args[1] + " eliminada.");
                break;

            case "center":
                handler.setCentro(p.getLocation());
                p.sendMessage("§aCentro del evento fijado en tu posición actual.");
                break;

            case "category":
                if (args.length < 2) return true;
                StringBuilder cat = new StringBuilder();
                for(int i=1; i<args.length; i++) cat.append(args[i]).append(" ");
                handler.setCategoria(cat.toString().trim());
                p.sendMessage("§aCategoría fijada a: " + cat.toString().trim());
                break;

            case "startbuild":
                if (!handler.isEventoIniciado()) {
                    p.sendMessage("§cEl evento no ha iniciado. Usa /buildbattle start.");
                    return true;
                }
                if (!handler.isTpRealizado()) {
                    p.sendMessage("§cDebes ejecutar /buildbattle tpzona y esperar a que el TP ocurra primero.");
                    return true;
                }
                if (!handler.getFase().equals("Lobby") && !handler.getFase().equals("Desempate_Lobby")) {
                    p.sendMessage("§cLa fase actual es '" + handler.getFase() + "'. No puedes usar startbuild ahora.");
                    return true;
                }
                handler.startBuildPhase();
                break;

            case "skipbuild":
                handler.skipBuildPhase();
                break;

            case "declararganador":
                handler.declararGanadores();
                break;

            case "forceganador":
                handler.forzarGanador();
                break;

            case "resetbuild":
                handler.resetAllBuilds();
                break;

            case "spawnfinal":
                handler.spawnFinal();
                break;

            case "puntos":
                int pagina = 1;
                if (args.length > 1) {
                    try {
                        pagina = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                handler.mostrarPuntosAdministrativos(p, pagina);
                break;

            case "end":
                handler.endEvent();
                break;

            case "reload":
                handler.loadFiles();
                p.sendMessage("§aArchivos de configuración recargados.");
                break;

            case "timeredit":
                if (args.length < 2) {
                    p.sendMessage("§cUso: /buildbattle timeredit <minutos (ej: 1 o -1)>");
                    return true;
                }
                try {
                    handler.modifyTimer(Integer.parseInt(args[1]));
                    p.sendMessage("§aTiempo del temporizador modificado con éxito.");
                } catch (NumberFormatException e) {
                    p.sendMessage("§cPor favor, ingresa un número válido.");
                }
                break;

            default:
                p.sendMessage("§cComando desconocido de BuildBattle.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                    "addvotador", "removevotador", "addadmin", "removeadmin", "start", "reglas", "addplayer",
                    "removeplayer", "tpzona", "forcetp", "stick", "addparcela", "removeparcela",
                    "center", "category", "startbuild", "skipbuild", "declararganador", "forceganador",
                    "resetbuild", "spawnfinal", "end", "reload", "puntos", "timeredit"
            );
            StringUtil.copyPartialMatches(args[0], subcommands, completions);
            Collections.sort(completions);
            return completions;

        } else if (args.length == 2 && args[0].equalsIgnoreCase("timeredit")) {
            completions.addAll(Arrays.asList("1", "-1", "2", "-2", "5", "-5"));
            return completions;

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (Arrays.asList("addvotador", "removevotador", "addadmin", "removeadmin", "addplayer", "removeplayer").contains(subCommand)) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (subCommand.equals("addparcela") || subCommand.equals("removeparcela")) {
                completions.add("<id>");
                return completions;
            } else if (subCommand.equals("category")) {
                completions.add("<nombre>");
                return completions;
            } else if (subCommand.equals("puntos")) {
                completions.add("1");
                completions.add("2");
                return completions;
            }
        }

        return Collections.emptyList();
    }
}