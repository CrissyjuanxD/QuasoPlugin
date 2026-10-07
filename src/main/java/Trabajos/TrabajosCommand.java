package Trabajos;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static Trabajos.TrabajosTexto.*;

// /trabajos (menú), /trabajos info (detalles en el chat) y los de admin: ver, nivel add/remove, xp, espera y reset
final class TrabajosCommand implements CommandExecutor, TabCompleter {

    private final TrabajosManager manager;
    private final TrabajosGUI gui;

    TrabajosCommand(TrabajosManager manager, TrabajosGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && admin(sender, args)) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIJO + "Solo los jugadores pueden usar este comando.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) info(player);
        else if (args.length > 0 && args[0].equalsIgnoreCase("guia")) TrabajosGuia.abrir(player);
        else gui.abrir(player);
        return true;
    }

    private void info(Player player) {
        DatosTrabajo datos = manager.datos(player);
        if (datos == null) {
            player.sendMessage(PREFIJO + ROSA + "Tus trabajos todavía se están cargando, prueba en unos segundos.");
            return;
        }
        String linea = CAFE_OSCURO + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        player.sendMessage(linea);
        player.sendMessage(CAFE + "" + net.md_5.bungee.api.ChatColor.BOLD + "  ☕ Tu trabajo");
        if (datos.activo == null) {
            player.sendMessage(CREMA + "  Todavía no tienes trabajo. Usa " + BLANCO + "/trabajos" + CREMA + " para elegir uno.");
        } else {
            Trabajo trabajo = datos.activo;
            int nivel = datos.nivel(trabajo);
            player.sendMessage(CAFE + "  Trabajo: " + nombre(trabajo));
            player.sendMessage(CAFE + "  Nivel: " + BLANCO + nivel + GRIS + "/" + Trabajo.NIVEL_MAXIMO);
            if (nivel >= Trabajo.NIVEL_MAXIMO) {
                player.sendMessage(DORADO + "  ¡Llegaste al nivel máximo!");
            } else {
                int necesita = TrabajoNiveles.xpParaNivel(nivel + 1);
                player.sendMessage(CAFE + "  Progreso: " + barra(datos.xp(trabajo), necesita) + GRIS + " (" + numero(datos.xp(trabajo)) + "/" + numero(necesita) + " XP)");
                player.sendMessage(CAFE + "  Te faltan: " + BLANCO + numero(Math.ceil(necesita - datos.xp(trabajo))) + " XP" + GRIS + " para el nivel " + (nivel + 1));
                player.sendMessage(CAFE + "  Recompensa: " + DORADO + TrabajoNiveles.monedasTotales(nivel + 1) + " DinoCoins" + GRIS + " + "
                        + TrabajoNiveles.experiencia(nivel + 1) + " de experiencia");
                int bonus = TrabajoNiveles.proximoBonus(nivel);
                if (bonus > 0) player.sendMessage(CAFE + "  Próximo bonus: " + BLANCO + "nivel " + bonus + GRIS + " (+" + TrabajoNiveles.bonus(bonus) + " DinoCoins)");
            }
            double hora = System.currentTimeMillis() - datos.inicioHora >= 60 * 60 * 1000L ? 0 : datos.xpEnLaHora;
            player.sendMessage(CAFE + "  XP esta hora: " + BLANCO + numero(hora) + GRIS + "/" + numero(TrabajoNiveles.TOPE_POR_HORA)
                    + (hora >= TrabajoNiveles.TOPE_POR_HORA ? ROSA + " (rinde 25%)" : ""));
            long espera = manager.esperaRestante(datos);
            player.sendMessage(CAFE + "  Cambio de trabajo: " + (espera > 0 ? ROSA + "en " + tiempo(espera) : SALVIA + "disponible"));
        }
        StringBuilder otros = new StringBuilder();
        for (Trabajo trabajo : Trabajo.values()) {
            if (trabajo == datos.activo) continue;
            if (!otros.isEmpty()) otros.append(GRIS).append(", ");
            otros.append(color(trabajo)).append(trabajo.nombre()).append(" ").append(BLANCO).append(datos.nivel(trabajo));
        }
        player.sendMessage(CAFE + "  Otros niveles: " + otros);
        player.sendMessage(linea);
    }

    // /trabajos ver <jugador>: todas sus estadísticas, aunque esté desconectado
    private void ver(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIJO + "Uso: /trabajos ver <jugador>");
            return;
        }
        OfflinePlayer jugador = Bukkit.getPlayerExact(args[1]);
        if (jugador == null) jugador = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (jugador == null) {
            sender.sendMessage(PREFIJO + ROSA + "Ese jugador nunca entró al server.");
            return;
        }
        String nombreJugador = jugador.getName() != null ? jugador.getName() : args[1];
        manager.datosDe(jugador.getUniqueId(), datos -> estadisticas(sender, nombreJugador, datos),
                () -> sender.sendMessage(PREFIJO + ROSA + "No se pudieron leer sus trabajos de la base de datos."));
    }

    private void estadisticas(CommandSender sender, String nombreJugador, DatosTrabajo datos) {
        String linea = CAFE_OSCURO + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        sender.sendMessage(linea);
        sender.sendMessage(CAFE + "" + net.md_5.bungee.api.ChatColor.BOLD + "  ☕ Trabajos de " + BLANCO + nombreJugador);
        if (datos.activo == null) {
            sender.sendMessage(CAFE + "  Trabajo actual: " + GRIS + "ninguno");
        } else {
            sender.sendMessage(CAFE + "  Trabajo actual: " + nombre(datos.activo) + GRIS + " (desde hace "
                    + tiempo(System.currentTimeMillis() - datos.desde) + ")");
            long espera = manager.esperaRestante(datos);
            sender.sendMessage(CAFE + "  Cambio de trabajo: " + (espera > 0 ? ROSA + "en " + tiempo(espera) : SALVIA + "disponible"));
            double hora = System.currentTimeMillis() - datos.inicioHora >= 60 * 60 * 1000L ? 0 : datos.xpEnLaHora;
            sender.sendMessage(CAFE + "  XP esta hora: " + BLANCO + numero(hora) + GRIS + "/" + numero(TrabajoNiveles.TOPE_POR_HORA));
        }
        int monedasTotal = 0;
        int nivelesTotal = 0;
        for (Trabajo trabajo : Trabajo.values()) {
            int nivel = datos.nivel(trabajo);
            int monedas = 0;
            for (int n = 1; n <= nivel; n++) monedas += TrabajoNiveles.monedasTotales(n);
            monedasTotal += monedas;
            nivelesTotal += nivel;
            String progreso = nivel >= Trabajo.NIVEL_MAXIMO ? DORADO + "máximo"
                    : barra(datos.xp(trabajo), TrabajoNiveles.xpParaNivel(nivel + 1)) + GRIS + " (" + numero(datos.xp(trabajo)) + "/"
                    + numero(TrabajoNiveles.xpParaNivel(nivel + 1)) + ")";
            sender.sendMessage("  " + nombre(trabajo) + GRIS + " · " + BLANCO + "Nv " + nivel + GRIS + " · " + progreso
                    + GRIS + " · " + DORADO + monedas + " DC");
        }
        sender.sendMessage(CAFE + "  Niveles en total: " + BLANCO + nivelesTotal + GRIS + "/" + Trabajo.values().length * Trabajo.NIVEL_MAXIMO);
        sender.sendMessage(CAFE + "  DinoCoins ganadas por niveles: " + DORADO + numero(monedasTotal));
        sender.sendMessage(linea);
    }

    private static boolean esAdmin(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("quasoplugin.trabajos.admin");
    }

    // /trabajos nivel add|remove <jugador> <n> · xp <jugador> <n> · espera <jugador> · reset
    private boolean admin(CommandSender sender, String[] args) {
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("nivel", "xp", "espera", "reset", "ver").contains(sub)) return false;
        if (!esAdmin(sender)) {
            sender.sendMessage(PREFIJO + ROSA + "No tienes permiso.");
            return true;
        }
        if (sub.equals("ver")) {
            ver(sender, args);
            return true;
        }
        if (sub.equals("reset")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirmar")) {
                sender.sendMessage(PREFIJO + ROSA + "Esto borra los trabajos y niveles de TODOS los jugadores y empiezan de cero.");
                sender.sendMessage(PREFIJO + "Para hacerlo escribe " + BLANCO + "/trabajos reset confirmar");
                return true;
            }
            manager.resetTodo(() -> {
                sender.sendMessage(PREFIJO + SALVIA + "Trabajos reiniciados: todos empiezan desde cero.");
                Bukkit.broadcastMessage(PREFIJO + "Los trabajos se reiniciaron: todos empiezan desde cero.");
            });
            return true;
        }

        Player objetivo = null;
        int indiceJugador = sub.equals("nivel") ? 2 : 1;
        if (args.length > indiceJugador) objetivo = Bukkit.getPlayerExact(args[indiceJugador]);
        boolean hecho;
        switch (sub) {
            case "nivel" -> {
                Integer cantidad = args.length > 3 ? entero(args[3]) : null;
                boolean sube = args.length > 1 && args[1].equalsIgnoreCase("add");
                boolean baja = args.length > 1 && args[1].equalsIgnoreCase("remove");
                if ((!sube && !baja) || cantidad == null || cantidad <= 0) {
                    sender.sendMessage(PREFIJO + "Uso: /trabajos nivel <add|remove> <jugador> <niveles>");
                    return true;
                }
                if (objetivo == null) {
                    sender.sendMessage(PREFIJO + ROSA + "Ese jugador no está conectado.");
                    return true;
                }
                hecho = manager.cambiarNivel(objetivo, sube ? cantidad : -cantidad);
                if (hecho) {
                    DatosTrabajo datos = manager.datos(objetivo);
                    sender.sendMessage(PREFIJO + SALVIA + objetivo.getName() + " ahora es " + nombre(datos.activo) + SALVIA + " nivel " + datos.nivel(datos.activo) + ".");
                    return true;
                }
            }
            case "xp" -> {
                Integer cantidad = args.length > 2 ? entero(args[2]) : null;
                if (objetivo == null || cantidad == null || cantidad <= 0) {
                    sender.sendMessage(PREFIJO + "Uso: /trabajos xp <jugador> <cantidad>");
                    return true;
                }
                hecho = manager.darXpAdmin(objetivo, cantidad);
            }
            default -> {
                if (objetivo == null) {
                    sender.sendMessage(PREFIJO + "Uso: /trabajos espera <jugador>");
                    return true;
                }
                hecho = manager.quitarEspera(objetivo);
            }
        }
        sender.sendMessage(PREFIJO + (hecho ? SALVIA + "Listo." : ROSA + "No se pudo: sus datos no cargaron o no tiene trabajo."));
        return true;
    }

    private static Integer entero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> opciones = new ArrayList<>();
        boolean admin = esAdmin(sender);
        if (args.length == 1) {
            opciones.addAll(List.of("info", "guia"));
            if (admin) opciones.addAll(List.of("ver", "nivel", "xp", "espera", "reset"));
        } else if (admin) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("nivel") && args.length == 2) opciones.addAll(List.of("add", "remove"));
            else if ((sub.equals("nivel") && args.length == 3) || ((sub.equals("xp") || sub.equals("espera") || sub.equals("ver")) && args.length == 2)) {
                Bukkit.getOnlinePlayers().forEach(p -> opciones.add(p.getName()));
            } else if (sub.equals("reset") && args.length == 2) opciones.add("confirmar");
        }
        String ultimo = args[args.length - 1].toLowerCase(Locale.ROOT);
        opciones.removeIf(opcion -> !opcion.toLowerCase(Locale.ROOT).startsWith(ultimo));
        return opciones;
    }
}
