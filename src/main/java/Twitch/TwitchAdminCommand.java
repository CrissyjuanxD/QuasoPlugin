package Twitch;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// /twitchadmin: todo lo del sistema de Twitch en un comando. Solo la consola y los nicks de administradores en
// twitch.yml (ser operador no alcanza)
final class TwitchAdminCommand implements CommandExecutor, TabCompleter {

    private static final int PAGE = 10;
    private static final List<String> SUBCOMMANDS = List.of("estado", "canal", "info", "lista", "historial", "darkit",
            "vincular", "desvincular", "revisar", "fly", "simular", "kits", "recargar");

    private final TwitchManager manager;

    TwitchAdminCommand(TwitchManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!manager.config().isAdmin(sender)) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Solo los administradores de twitch.yml pueden usar este comando.");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "estado" -> status(sender);
            case "canal" -> channel(sender, args);
            case "info" -> info(sender, args);
            case "lista" -> list(sender, args);
            case "historial" -> history(sender, args);
            case "darkit" -> giveKit(sender, args);
            case "vincular" -> link(sender, args);
            case "desvincular" -> unlink(sender, args);
            case "revisar" -> check(sender, args);
            case "fly" -> fly(sender, args);
            case "simular" -> simulate(sender, args);
            case "kits" -> {
                if (sender instanceof Player player) manager.preview().open(player);
                else sender.sendMessage("Solo un jugador puede ver los kits.");
            }
            case "recargar" -> {
                manager.reload();
                sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "twitch.yml recargado. Admins: " + String.join(", ", manager.config().admins()));
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        String c = TwitchText.HIGHLIGHT + "/" + label + " ";
        sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Comandos de administración:");
        sender.sendMessage(c + "estado" + TwitchText.GRAY + " - conexión, revisiones y /fly");
        sender.sendMessage(c + "canal <conectar|desconectar>" + TwitchText.GRAY + " - conectar el canal de Crosszy");
        sender.sendMessage(c + "info <jugador|cuenta>" + TwitchText.GRAY + " - sub, kits y cuánto le falta");
        sender.sendMessage(c + "lista [subs|vips|todos] [página]" + TwitchText.GRAY + " - cuentas vinculadas");
        sender.sendMessage(c + "historial [jugador] [página]" + TwitchText.GRAY + " - quién recibió kits");
        sender.sendMessage(c + "darkit <jugador> <sub1|sub2|sub3|vip>" + TwitchText.GRAY + " - dar un kit (no gasta su mes)");
        sender.sendMessage(c + "vincular <jugador> <usuario_twitch>" + TwitchText.GRAY + " - vincular a mano");
        sender.sendMessage(c + "desvincular <jugador>" + TwitchText.GRAY + " - quitar la vinculación");
        sender.sendMessage(c + "revisar [jugador]" + TwitchText.GRAY + " - revisar en Twitch ahora");
        sender.sendMessage(c + "fly <on|off>" + TwitchText.GRAY + " - prender o apagar el /fly para eventos");
        sender.sendMessage(c + "simular <jugador> <sub|vip|ninguno|mes|quitar>" + TwitchText.GRAY + " - pruebas sin sub");
        sender.sendMessage(c + "kits" + TwitchText.GRAY + " - ver qué trae cada kit");
        sender.sendMessage(c + "recargar" + TwitchText.GRAY + " - volver a leer twitch.yml");
    }

    // ---------------------------------------------------------------- Estado y canal

    private void status(CommandSender sender) {
        TwitchChannel channel = manager.channel();
        sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Estado del sistema de Twitch");
        sender.sendMessage(TwitchText.GRAY + " App: " + (manager.api().configured() ? TwitchText.OK + "configurada" : TwitchText.ERROR + "falta el client_id en twitch.yml"));
        if (channel.connected()) {
            sender.sendMessage(TwitchText.GRAY + " Canal: " + TwitchText.OK + channel.login() + TwitchText.GRAY + " (conectado por "
                    + channel.connectedBy() + " el " + TwitchText.date(channel.connectedAt()) + ")");
        } else {
            sender.sendMessage(TwitchText.GRAY + " Canal: " + TwitchText.ERROR + "sin conectar" + TwitchText.GRAY
                    + (channel.lastError() != null ? " (" + channel.lastError() + ")" : "") + " · /twitchadmin canal conectar");
        }
        sender.sendMessage(TwitchText.GRAY + " Última revisión: " + TwitchText.TEXT + TwitchText.ago(manager.lastPoll())
                + TwitchText.GRAY + " · cada " + manager.config().checkMinutes + " minutos"
                + (manager.lastError() != null ? TwitchText.ERROR + " · error: " + manager.lastError() : ""));
        int subs = 0, vips = 0;
        for (TwitchAccount account : manager.store().linkedAccounts()) {
            if (account.sub) subs++;
            else if (account.vip) vips++;
        }
        sender.sendMessage(TwitchText.GRAY + " Vinculados: " + TwitchText.TEXT + manager.store().linkedAccounts().size()
                + TwitchText.GRAY + " (" + subs + " subs, " + vips + " VIPs) · pruebas: " + manager.store().simulatedAccounts().size());
        sender.sendMessage(TwitchText.GRAY + " /fly: " + (manager.store().flyEnabled ? TwitchText.OK + "prendido" : TwitchText.ERROR + "apagado (evento)")
                + TwitchText.GRAY + " · un mes = " + (manager.config().month / 86_400_000L) + " días de sub");
    }

    private void channel(CommandSender sender, String[] args) {
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "conectar" -> manager.connectChannel(sender);
            case "desconectar" -> {
                manager.disconnectChannel(sender.getName());
                sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Canal desconectado. Los kits no se pueden reclamar hasta volver a conectarlo.");
            }
            default -> sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin canal <conectar|desconectar>");
        }
    }

    // ---------------------------------------------------------------- Consultas

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin info <jugador|cuenta_twitch>");
            return;
        }
        TwitchAccount account = account(args[1]);
        if (account == null) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " no tiene una cuenta de Twitch vinculada.");
            return;
        }
        long month = manager.config().month;
        sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Twitch de " + TwitchText.HIGHLIGHT + account.playerName);
        sender.sendMessage(TwitchText.GRAY + " Cuenta: " + TwitchText.TEXT + account.login + TwitchText.GRAY
                + (account.isSimulated() ? " (prueba)" : " · id " + account.twitchId + " · vinculada el " + TwitchText.date(account.linkedAt)));
        sender.sendMessage(TwitchText.GRAY + " Estado: " + manager.describe(account));
        if (account.subRacha.activa()) {
            sender.sendMessage(TwitchText.GRAY + " Sub vista activa: " + TwitchText.TEXT + TwitchText.duration(account.subRacha.observado)
                    + TwitchText.GRAY + " seguidos desde el " + TwitchText.date(account.subRacha.inicio) + " · mes " + account.subRacha.mesActual(month)
                    + " (reclamado el " + account.subRacha.mesReclamado + ")");
        }
        if (account.vipRacha.activa()) {
            sender.sendMessage(TwitchText.GRAY + " VIP visto activo: " + TwitchText.TEXT + TwitchText.duration(account.vipRacha.observado)
                    + TwitchText.GRAY + " · mes " + account.vipRacha.mesActual(month) + " (reclamado el " + account.vipRacha.mesReclamado + ")");
        }
        sender.sendMessage(TwitchText.GRAY + " Kit: " + manager.kitState(account));
        sender.sendMessage(TwitchText.GRAY + " Kits de sub en total: " + TwitchText.TEXT + account.subKits
                + TwitchText.GRAY + " (el próximo es el " + TwitchKits.subKit(account.subKits).title + ")");
        List<TwitchStore.Entry> last = manager.store().history(account.playerName);
        if (!last.isEmpty()) {
            TwitchStore.Entry entry = last.get(0);
            sender.sendMessage(TwitchText.GRAY + " Último kit: " + TwitchText.TEXT + entry.kit() + TwitchText.GRAY + " el " + TwitchText.date(entry.time())
                    + (entry.by().isBlank() ? "" : " (" + entry.by() + ")"));
        }
        if (account.player != null) {
            Player online = Bukkit.getPlayer(account.player);
            sender.sendMessage(TwitchText.GRAY + " /fly: " + (online != null && TwitchFly.isActive(online) ? TwitchText.OK + "activado" : TwitchText.TEXT + "desactivado"));
        }
    }

    private void list(CommandSender sender, String[] args) {
        String filter = args.length > 1 && !isNumber(args[1]) ? args[1].toLowerCase(Locale.ROOT) : "todos";
        int page = page(args, args.length > 1 && !isNumber(args[1]) ? 2 : 1);
        List<TwitchAccount> accounts = new ArrayList<>();
        for (TwitchAccount account : manager.store().linkedAccounts()) {
            if (filter.startsWith("sub") && !account.sub) continue;
            if (filter.startsWith("vip") && !account.vip) continue;
            accounts.add(account);
        }
        accounts.addAll(filter.equals("todos") ? manager.store().simulatedAccounts() : List.of());
        accounts.sort(Comparator.comparing(account -> account.playerName == null ? "" : account.playerName.toLowerCase(Locale.ROOT)));
        int pages = Math.max(1, (accounts.size() + PAGE - 1) / PAGE);
        page = Math.min(page, pages);
        sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Vinculados (" + filter + "): " + accounts.size() + TwitchText.GRAY + " · página " + page + "/" + pages);
        for (int i = (page - 1) * PAGE; i < Math.min(accounts.size(), page * PAGE); i++) {
            TwitchAccount account = accounts.get(i);
            sender.sendMessage(TwitchText.GRAY + " • " + TwitchText.HIGHLIGHT + account.playerName + TwitchText.GRAY + " (" + account.login + ") "
                    + manager.describe(account) + TwitchText.GRAY + " · " + manager.kitState(account));
        }
    }

    private void history(CommandSender sender, String[] args) {
        String filter = args.length > 1 && !isNumber(args[1]) ? args[1] : null;
        int page = page(args, filter != null ? 2 : 1);
        List<TwitchStore.Entry> entries = manager.store().history(filter);
        int pages = Math.max(1, (entries.size() + PAGE - 1) / PAGE);
        page = Math.min(page, pages);
        sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Kits entregados" + (filter != null ? " a " + filter : "") + ": "
                + entries.size() + TwitchText.GRAY + " · página " + page + "/" + pages);
        for (int i = (page - 1) * PAGE; i < Math.min(entries.size(), page * PAGE); i++) {
            TwitchStore.Entry entry = entries.get(i);
            sender.sendMessage(TwitchText.GRAY + " " + TwitchText.date(entry.time()) + " " + TwitchText.HIGHLIGHT + entry.player()
                    + TwitchText.GRAY + " (" + entry.account() + ") " + TwitchText.TEXT + entry.kit()
                    + (entry.by().isBlank() ? "" : TwitchText.GRAY + " · " + entry.by()));
        }
    }

    // ---------------------------------------------------------------- Acciones

    // Para cuando a alguien se le bugeó el kit: se le da el cofre sin tocar su mes (queda en el historial)
    private void giveKit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin darkit <jugador> <sub1|sub2|sub3|vip>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " tiene que estar conectado.");
            return;
        }
        TwitchKits.Kit kit = TwitchKits.Kit.byId(args[2]);
        if (kit == null) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Kit inválido: usa sub1, sub2, sub3 o vip.");
            return;
        }
        TwitchKits.give(target, TwitchKits.chest(kit, target.getName()));
        TwitchAccount account = manager.store().byPlayer(target.getUniqueId());
        manager.store().log(target.getName(), account != null ? account.login : "-", kit.title, "dado por " + sender.getName());
        manager.saveAsync();
        target.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Un admin te dio el " + kit.colored() + TwitchText.OK + ".");
        sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Le diste el " + kit.colored() + TwitchText.OK + " a " + target.getName() + ". No cuenta como su kit del mes.");
    }

    private void link(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin vincular <jugador> <usuario_twitch>");
            return;
        }
        OfflinePlayer target = player(args[1]);
        if (target == null) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " nunca entró al server.");
            return;
        }
        if (!manager.channel().connected()) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Para buscar la cuenta en Twitch el canal tiene que estar conectado.");
            return;
        }
        String login = args[2];
        TwitchApi api = manager.api();
        manager.channel().call(token -> api.userByLogin(token, login)).whenComplete((user, error) -> manager.runSync(() -> {
                    if (error != null) {
                        sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Twitch no respondió: " + TwitchManager.message(error));
                        return;
                    }
                    if (user == null) {
                        sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "No existe el usuario de Twitch " + login + ".");
                        return;
                    }
                    TwitchAccount existing = manager.store().byTwitchId(user.id());
                    if (existing != null && existing.player != null && !existing.player.equals(target.getUniqueId())) {
                        sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + user.login() + " ya está vinculada a " + existing.playerName
                                + ". Primero desvincúlala.");
                        return;
                    }
                    TwitchAccount account = existing != null ? existing : new TwitchAccount(user.id());
                    account.login = user.login();
                    String name = target.getName() != null ? target.getName() : args[1];
                    manager.store().link(account, target.getUniqueId(), name);
                    manager.saveAsync();
                    manager.check(List.of(account));
                    sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + name + " quedó vinculado a " + user.login() + ".");
                }));
    }

    private void unlink(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin desvincular <jugador|cuenta_twitch>");
            return;
        }
        TwitchAccount account = account(args[1]);
        if (account == null || account.isSimulated()) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " no tiene una cuenta de Twitch vinculada.");
            return;
        }
        String name = account.playerName;
        manager.unlink(account);
        sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Se desvinculó " + account.login + " de " + name
                + ". Sus kits cobrados siguen anotados en esa cuenta de Twitch.");
    }

    private void check(CommandSender sender, String[] args) {
        if (!manager.channel().connected()) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "El canal no está conectado.");
            return;
        }
        if (args.length < 2) {
            manager.poll();
            sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Revisando a todos en Twitch...");
            return;
        }
        TwitchAccount account = account(args[1]);
        if (account == null || account.isSimulated()) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " no tiene una cuenta de Twitch vinculada.");
            return;
        }
        manager.check(List.of(account)).thenAccept(ok -> sender.sendMessage(ok
                ? TwitchText.PREFIX + TwitchText.TEXT + account.playerName + ": " + manager.describe(account) + TwitchText.GRAY + " · " + manager.kitState(account)
                : TwitchText.PREFIX + TwitchText.ERROR + "No se pudo revisar: " + manager.lastError()));
    }

    private void fly(CommandSender sender, String[] args) {
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (!action.equals("on") && !action.equals("off")) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "El /fly está " + (manager.store().flyEnabled ? "prendido" : "apagado")
                    + ". Uso: /twitchadmin fly <on|off>");
            return;
        }
        boolean enabled = action.equals("on");
        manager.fly().setEnabled(enabled);
        manager.saveAsync();
        sender.sendMessage(TwitchText.PREFIX + (enabled ? TwitchText.OK + "El /fly vuelve a estar disponible."
                : TwitchText.HIGHLIGHT + "El /fly quedó apagado para todos hasta que uses /twitchadmin fly on."));
    }

    // Cuentas de prueba: un jugador puede tener sub o VIP sin pagar para probar kits, roles y /fly. "mes" adelanta
    // un mes de sub para probar el kit siguiente y "quitar" vuelve a su cuenta de verdad
    private void simulate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Uso: /twitchadmin simular <jugador> <sub|vip|ninguno|mes|quitar>");
            return;
        }
        OfflinePlayer target = player(args[1]);
        if (target == null) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + args[1] + " nunca entró al server.");
            return;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : args[1];
        String action = args[2].toLowerCase(Locale.ROOT);
        TwitchStore store = manager.store();
        TwitchAccount account = store.simulatedFor(uuid);
        switch (action) {
            case "sub", "vip", "ninguno" -> {
                if (account == null) {
                    account = new TwitchAccount("sim-" + uuid);
                    account.login = "(prueba)";
                    account.player = uuid;
                    account.playerName = name;
                    account.linkedAt = System.currentTimeMillis();
                }
                account.simulated = action;
                store.putSimulated(account);
                manager.observeSimulated(account, System.currentTimeMillis());
                sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + name + " ahora tiene "
                        + (action.equals("ninguno") ? "la sub y el VIP vencidos" : action.equals("sub") ? "sub" : "VIP") + " de prueba.");
            }
            case "mes" -> {
                if (account == null || (!account.subRacha.activa() && !account.vipRacha.activa())) {
                    sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + name + " no tiene una sub o VIP de prueba activos.");
                    return;
                }
                Racha racha = account.subRacha.activa() ? account.subRacha : account.vipRacha;
                racha.observado += manager.config().month;
                store.markDirty();
                sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Pasó un mes de prueba: " + name + " va por el mes " + racha.mesActual(manager.config().month) + ".");
            }
            case "quitar" -> {
                if (account == null) {
                    sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + name + " no tiene una cuenta de prueba.");
                    return;
                }
                store.removeSimulated(uuid);
                manager.refreshRole(uuid);
                sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "Se quitó la cuenta de prueba de " + name + ".");
            }
            default -> {
                sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Usa sub, vip, ninguno, mes o quitar.");
                return;
            }
        }
        manager.saveAsync();
    }

    // ---------------------------------------------------------------- Ayudas

    private TwitchAccount account(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            TwitchAccount account = manager.store().byPlayer(online.getUniqueId());
            if (account != null) return account;
        }
        return manager.store().find(name);
    }

    private static OfflinePlayer player(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    private static boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int page(String[] args, int index) {
        if (args.length <= index) return 1;
        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!manager.config().isAdmin(sender)) return List.of();
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "canal" -> filter(List.of("conectar", "desconectar"), args[1]);
                case "fly" -> filter(List.of("on", "off"), args[1]);
                case "lista" -> filter(List.of("subs", "vips", "todos"), args[1]);
                case "info", "historial", "darkit", "vincular", "desvincular", "revisar", "simular" -> filter(onlineNames(), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (sub.equals("darkit")) return filter(List.of("sub1", "sub2", "sub3", "vip"), args[2]);
            if (sub.equals("simular")) return filter(List.of("sub", "vip", "ninguno", "mes", "quitar"), args[2]);
        }
        return List.of();
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return names;
    }

    private static List<String> filter(List<String> options, String typed) {
        List<String> result = new ArrayList<>();
        String lower = typed.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(option);
        }
        return result;
    }
}
