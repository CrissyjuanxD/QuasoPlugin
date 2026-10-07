package Twitch;

import Events.MissionSystem.MissionHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// El sistema de Twitch del canal de Crosszy: vincula cuentas, revisa cada tantos minutos quién tiene sub o VIP,
// pone los roles, entrega los kits del mes y maneja el /fly
public final class TwitchManager implements Listener {

    // El jugador solo autoriza que se sepa quién es; el token se descarta apenas se vincula
    private static final String VIEWER_SCOPES = "user:read:subscriptions";
    // Para el kit y el /fly la última revisión tiene que ser de hace menos de un día
    private static final long STALE = 24L * 60 * 60 * 1000;

    private final JavaPlugin plugin;
    private final File folder;
    private final TwitchStore store;
    private final TwitchRoles roles;
    private final TwitchFly fly;
    private final TwitchKitPreview preview;
    private TwitchConfig config;
    private TwitchApi api;
    private TwitchChannel channel;

    private final Map<UUID, CompletableFuture<?>> pendingLinks = new HashMap<>();
    private CompletableFuture<?> pendingChannel;
    private final Set<UUID> claiming = new HashSet<>();
    private BukkitTask pollTask;
    private BukkitTask saveTask;
    private long lastPoll;
    private String lastError;
    private long saveSequence;
    private long writtenSequence;
    private boolean shutdown;

    public TwitchManager(JavaPlugin plugin, MissionHandler missions) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "twitch");
        this.config = TwitchConfig.load(plugin);
        this.api = new TwitchApi(config.clientId, config.clientSecret);
        this.channel = new TwitchChannel(api, new File(folder, "canal.yml"), plugin.getLogger());
        this.store = new TwitchStore(new File(folder, "datos.yml"), plugin.getLogger());
        this.roles = new TwitchRoles(missions);
        this.fly = new TwitchFly(plugin, this);
        this.preview = new TwitchKitPreview(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(fly, plugin);
        plugin.getServer().getPluginManager().registerEvents(preview, plugin);
        TwitchCommand twitch = new TwitchCommand(this);
        PluginCommand twitchCommand = Objects.requireNonNull(plugin.getCommand("twitch"));
        twitchCommand.setExecutor(twitch);
        twitchCommand.setTabCompleter(twitch);
        TwitchAdminCommand admin = new TwitchAdminCommand(this);
        PluginCommand adminCommand = Objects.requireNonNull(plugin.getCommand("twitchadmin"));
        adminCommand.setExecutor(admin);
        adminCommand.setTabCompleter(admin);
        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(fly);

        schedulePoll();
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (store.dirty()) saveAsync();
        }, 20L * 60, 20L * 60);

        if (!api.configured()) {
            plugin.getLogger().warning("[Twitch] Falta el client_id en twitch.yml: los kits y el /fly de subs no funcionan hasta configurarlo.");
        } else if (!channel.connected()) {
            plugin.getLogger().warning("[Twitch] El canal no está conectado: usa /twitchadmin canal conectar con Crosszy.");
        }
    }

    TwitchStore store() {
        return store;
    }

    TwitchConfig config() {
        return config;
    }

    TwitchChannel channel() {
        return channel;
    }

    TwitchApi api() {
        return api;
    }

    TwitchFly fly() {
        return fly;
    }

    TwitchKitPreview preview() {
        return preview;
    }

    long lastPoll() {
        return lastPoll;
    }

    String lastError() {
        return lastError;
    }

    // ---------------------------------------------------------------- Estado

    boolean fresh(TwitchAccount account) {
        return account.isSimulated() || System.currentTimeMillis() - account.checkedAt < STALE;
    }

    // Sub activa según la última revisión (o la simulada)
    boolean isSub(Player player) {
        TwitchAccount account = store.byPlayer(player.getUniqueId());
        return account != null && account.sub && fresh(account);
    }

    // ---------------------------------------------------------------- Revisiones

    private void schedulePoll() {
        if (pollTask != null) pollTask.cancel();
        long period = config.checkMinutes * 60L * 20L;
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L * 30, period);
    }

    // Revisa a todos los vinculados de una vez (de a 20 por llamada) y avanza las cuentas de prueba
    void poll() {
        long now = System.currentTimeMillis();
        for (TwitchAccount account : new ArrayList<>(store.simulatedAccounts())) observeSimulated(account, now);
        if (!channel.connected()) return;
        TwitchChannel current = channel;
        current.validateIfNeeded().exceptionally(error -> {
            sync(() -> lastError = message(error));
            return null;
        });
        check(new ArrayList<>(store.linkedAccounts()));
    }

    void observeSimulated(TwitchAccount account, long now) {
        account.sub = "sub".equals(account.simulated);
        account.vip = "vip".equals(account.simulated);
        account.tier = account.sub ? "1000" : "";
        account.checkedAt = now;
        account.subRacha.observar(account.sub, now, config.maxGap());
        account.vipRacha.observar(account.vip, now, config.maxGap());
        store.markDirty();
        Player player = account.player == null ? null : Bukkit.getPlayer(account.player);
        if (player != null) roles.apply(player, store.byPlayer(player.getUniqueId()));
    }

    // Pregunta a Twitch por esas cuentas. Termina en el hilo del server con true si Twitch respondió
    CompletableFuture<Boolean> check(List<TwitchAccount> accounts) {
        List<String> ids = new ArrayList<>();
        for (TwitchAccount account : accounts) {
            if (!account.isSimulated() && account.player != null) ids.add(account.twitchId);
        }
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (ids.isEmpty()) {
            result.complete(true);
            return result;
        }
        if (!channel.connected()) {
            result.complete(false);
            return result;
        }
        TwitchChannel current = channel;
        TwitchApi currentApi = api;
        String broadcaster = current.broadcasterId();
        CompletableFuture<Map<String, TwitchApi.Sub>> subs = current.call(token -> currentApi.subscriptions(token, broadcaster, ids));
        CompletableFuture<Set<String>> vips = current.call(token -> currentApi.vips(token, broadcaster, ids));
        subs.thenCombine(vips, (s, v) -> Map.entry(s, v)).whenComplete((both, error) -> {
            if (!sync(() -> {
                if (error != null) {
                    lastError = message(error);
                    plugin.getLogger().warning("[Twitch] No se pudieron revisar las subs: " + lastError);
                    result.complete(false);
                    return;
                }
                long now = System.currentTimeMillis();
                for (String id : ids) {
                    TwitchAccount account = store.byTwitchId(id);
                    if (account == null || account.player == null) continue;
                    apply(account, both.getKey().get(id), both.getValue().contains(id), now);
                }
                lastPoll = now;
                lastError = null;
                result.complete(true);
            })) result.complete(false);
        });
        return result;
    }

    private void apply(TwitchAccount account, TwitchApi.Sub sub, boolean vip, long now) {
        account.sub = sub != null;
        account.tier = sub != null ? sub.tier() : "";
        account.gift = sub != null && sub.gift();
        account.gifter = sub != null ? sub.gifter() : "";
        account.vip = vip;
        account.checkedAt = now;
        account.subRacha.observar(account.sub, now, config.maxGap());
        account.vipRacha.observar(account.vip, now, config.maxGap());
        store.markDirty();
        Player player = Bukkit.getPlayer(account.player);
        if (player != null) roles.apply(player, store.byPlayer(player.getUniqueId()));
    }

    // ---------------------------------------------------------------- Vincular

    void startLink(Player player) {
        UUID uuid = player.getUniqueId();
        if (!api.configured()) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "El sistema de Twitch todavía no está configurado. Avísale a un admin.");
            return;
        }
        TwitchAccount linked = store.linkedTo(uuid);
        if (linked != null) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Ya tienes vinculada la cuenta " + TwitchText.HIGHLIGHT + linked.login
                    + TwitchText.TEXT + ". Para cambiarla usa /twitch desvincular.");
            return;
        }
        cancelLink(uuid);
        player.sendMessage(TwitchText.PREFIX + TwitchText.GRAY + "Pidiendo un código a Twitch...");
        TwitchApi currentApi = api;
        CompletableFuture<TwitchApi.TokenInfo> flow = new CompletableFuture<>();
        pendingLinks.put(uuid, flow);
        currentApi.deviceCode(VIEWER_SCOPES).whenComplete((code, error) -> sync(() -> {
            if (flow.isDone()) return;
            if (error != null) {
                pendingLinks.remove(uuid, flow);
                flow.cancel(false);
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Twitch no respondió (" + message(error) + "). Prueba en un rato.");
                return;
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online == null) {
                pendingLinks.remove(uuid, flow);
                flow.cancel(false);
                return;
            }
            sendCode(online, code, "Vincular tu cuenta de Twitch");
            CompletableFuture<TwitchApi.Token> waiting = currentApi.waitForToken(code);
            flow.whenComplete((ignored, cancelled) -> waiting.cancel(false));
            waiting.thenCompose(token -> currentApi.validate(token.access()).whenComplete((info, ignored) ->
                            currentApi.revoke(token.access()).exceptionally(e -> null)))
                    .whenComplete((info, failure) -> sync(() -> {
                        if (!pendingLinks.remove(uuid, flow)) return;
                        flow.complete(info);
                        Player now = Bukkit.getPlayer(uuid);
                        if (failure != null) {
                            if (now != null) now.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "No se pudo vincular: " + message(failure) + ". Vuelve a usar /twitch vincular.");
                            return;
                        }
                        finishLink(uuid, now, info);
                    }));
        }));
    }

    private void finishLink(UUID uuid, Player player, TwitchApi.TokenInfo info) {
        TwitchAccount existing = store.byTwitchId(info.userId());
        if (existing != null && existing.player != null && !existing.player.equals(uuid)) {
            if (player != null) player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "La cuenta " + info.login() + " ya está vinculada a "
                    + existing.playerName + ". Si es tuya, pide a un admin que la desvincule.");
            return;
        }
        if (store.linkedTo(uuid) != null && store.linkedTo(uuid) != existing) {
            if (player != null) player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Ya tienes otra cuenta vinculada.");
            return;
        }
        TwitchAccount account = existing != null ? existing : new TwitchAccount(info.userId());
        account.login = info.login();
        String name = player != null ? player.getName() : Objects.requireNonNullElse(Bukkit.getOfflinePlayer(uuid).getName(), "?");
        store.link(account, uuid, name);
        plugin.getLogger().info("[Twitch] " + name + " vinculó la cuenta " + info.login() + " (" + info.userId() + ")");
        if (player != null) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.OK + "¡Cuenta vinculada: " + TwitchText.HIGHLIGHT + info.login() + TwitchText.OK + "!");
        }
        saveAsync();
        check(List.of(account)).thenAccept(ok -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) sendStatus(online);
        });
    }

    void cancelLink(UUID uuid) {
        CompletableFuture<?> flow = pendingLinks.remove(uuid);
        if (flow != null) flow.cancel(false);
    }

    void unlink(TwitchAccount account) {
        UUID uuid = account.player;
        store.unlink(account);
        saveAsync();
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        if (player != null) roles.apply(player, store.byPlayer(uuid));
    }

    // Conecta el canal: el link lo tiene que abrir Crosszy con su cuenta de Twitch
    void connectChannel(CommandSender sender) {
        if (!api.configured()) {
            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Primero pon el client_id de la app de Twitch en twitch.yml y usa /twitchadmin recargar.");
            return;
        }
        if (pendingChannel != null) pendingChannel.cancel(false);
        TwitchApi currentApi = api;
        CompletableFuture<Void> flow = new CompletableFuture<>();
        pendingChannel = flow;
        String by = sender.getName();
        sender.sendMessage(TwitchText.PREFIX + TwitchText.GRAY + "Pidiendo un código a Twitch...");
        currentApi.deviceCode(TwitchChannel.SCOPES).whenComplete((code, error) -> sync(() -> {
            if (flow.isDone()) return;
            if (error != null) {
                flow.cancel(false);
                sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Twitch no respondió (" + message(error) + ").");
                return;
            }
            if (sender instanceof Player player) sendCode(player, code, "Conectar el canal de " + config.channel);
            else sender.sendMessage("Abre " + code.verificationUri() + " con la cuenta de " + config.channel + " (código " + code.userCode() + ")");
            CompletableFuture<TwitchApi.Token> waiting = currentApi.waitForToken(code);
            flow.whenComplete((ignored, cancelled) -> waiting.cancel(false));
            waiting.thenCompose(token -> currentApi.validate(token.access()).thenApply(info -> Map.entry(token, info)))
                    .whenComplete((pair, failure) -> sync(() -> {
                        if (flow.isDone()) return;
                        flow.complete(null);
                        if (failure != null) {
                            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "No se pudo conectar el canal: " + message(failure));
                            return;
                        }
                        TwitchApi.TokenInfo info = pair.getValue();
                        if (!info.login().equalsIgnoreCase(config.channel)) {
                            currentApi.revoke(pair.getKey().access()).exceptionally(e -> null);
                            sender.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "Se autorizó con la cuenta " + info.login()
                                    + ", pero el canal es " + config.channel + ". Tiene que abrir el link con la cuenta del canal.");
                            return;
                        }
                        channel.connect(pair.getKey(), info, by);
                        lastError = null;
                        plugin.getLogger().info("[Twitch] Canal " + info.login() + " conectado por " + by);
                        sender.sendMessage(TwitchText.PREFIX + TwitchText.OK + "¡Canal " + info.login() + " conectado! Revisando las subs...");
                        poll();
                    }));
        }));
    }

    void disconnectChannel(String by) {
        if (pendingChannel != null) pendingChannel.cancel(false);
        channel.disconnect("desconectado por " + by);
        plugin.getLogger().info("[Twitch] Canal desconectado por " + by);
    }

    private void sendCode(Player player, TwitchApi.DeviceCode code, String title) {
        TextColor purple = TextColor.color(0x9146FF);
        TextColor text = TextColor.color(0xE6D6FF);
        player.sendMessage(Component.text("━━━━━━ " + title + " ━━━━━━", purple, TextDecoration.BOLD));
        player.sendMessage(Component.text("1. ", text).append(Component.text("[Abrir Twitch]", purple, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(code.verificationUri()))
                .hoverEvent(HoverEvent.showText(Component.text(code.verificationUri(), NamedTextColor.GRAY)))));
        player.sendMessage(Component.text("2. Revisa que el código sea ", text).append(Component.text(code.userCode(), TextColor.color(0xFFD166), TextDecoration.BOLD)
                .clickEvent(ClickEvent.copyToClipboard(code.userCode()))
                .hoverEvent(HoverEvent.showText(Component.text("Clic para copiarlo", NamedTextColor.GRAY)))));
        player.sendMessage(Component.text("3. Toca Autorizar. El código dura " + TwitchText.duration(code.expiresAt() - System.currentTimeMillis()) + ".", text));
    }

    // ---------------------------------------------------------------- Kits

    void claim(Player player) {
        UUID uuid = player.getUniqueId();
        if (claiming.contains(uuid)) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.GRAY + "Espera, todavía estoy revisando tu sub...");
            return;
        }
        if (TwitchFly.inEvent(player)) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "No puedes reclamar el kit durante un evento.");
            return;
        }
        TwitchAccount account = store.byPlayer(uuid);
        if (account == null) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Primero vincula tu cuenta de Twitch con " + TwitchText.HIGHLIGHT + "/twitch vincular" + TwitchText.TEXT + ".");
            return;
        }
        if (account.isSimulated()) {
            observeSimulated(account, System.currentTimeMillis());
            give(player, account);
            return;
        }
        if (!channel.connected()) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "El canal de Twitch no está conectado ahora mismo. Avísale a un admin.");
            return;
        }
        claiming.add(uuid);
        player.sendMessage(TwitchText.PREFIX + TwitchText.GRAY + "Revisando tu sub en Twitch...");
        check(List.of(account)).thenAccept(ok -> {
            claiming.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || store.byPlayer(uuid) != account) return;
            if (!ok) {
                online.sendMessage(TwitchText.PREFIX + TwitchText.ERROR + "No se pudo revisar tu sub en Twitch. Prueba en unos minutos.");
                return;
            }
            give(online, account);
        });
    }

    // Un kit por mes de sub: si es sub recibe el de sub (y ese mes de VIP queda usado), si no el VIP
    private void give(Player player, TwitchAccount account) {
        long month = config.month;
        String test = account.isSimulated() ? " (prueba)" : "";
        if (account.sub) {
            if (!account.subRacha.puedeReclamar(month)) {
                player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Ya reclamaste el kit de sub de este mes. El siguiente se destraba en "
                        + TwitchText.HIGHLIGHT + TwitchText.duration(account.subRacha.falta(month)) + TwitchText.TEXT + " si tu sub se renueva.");
                return;
            }
            TwitchKits.Kit kit = TwitchKits.subKit(account.subKits);
            TwitchKits.give(player, TwitchKits.chest(kit, player.getName()));
            account.subRacha.reclamar(month);
            account.subKits++;
            if (account.vipRacha.activa()) account.vipRacha.reclamar(month);
            store.log(player.getName(), account.login, kit.title + test, "");
            player.sendMessage(TwitchText.PREFIX + TwitchText.OK + "¡Recibiste el " + kit.colored() + TwitchText.OK + "! El próximo se destraba en "
                    + TwitchText.HIGHLIGHT + TwitchText.duration(account.subRacha.falta(month)) + TwitchText.OK + " si tu sub se renueva.");
        } else if (account.vip) {
            if (!account.vipRacha.puedeReclamar(month)) {
                player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Ya reclamaste el kit VIP de este mes. El siguiente estará en "
                        + TwitchText.HIGHLIGHT + TwitchText.duration(account.vipRacha.falta(month)) + TwitchText.TEXT + " si sigues siendo VIP.");
                return;
            }
            TwitchKits.give(player, TwitchKits.chest(TwitchKits.Kit.VIP, player.getName()));
            account.vipRacha.reclamar(month);
            store.log(player.getName(), account.login, TwitchKits.Kit.VIP.title + test, "");
            player.sendMessage(TwitchText.PREFIX + TwitchText.OK + "¡Recibiste el " + TwitchKits.Kit.VIP.colored() + TwitchText.OK + "!");
        } else {
            player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "No tienes sub ni VIP activos en el canal de Crosszy. Si te acabas de suscribir, espera un minuto y vuelve a intentar.");
            return;
        }
        plugin.getLogger().info("[Twitch] " + player.getName() + " (" + account.login + ") reclamó su kit" + test);
        saveAsync();
    }

    // Lo que ve el jugador con /twitch
    void sendStatus(Player player) {
        TwitchAccount account = store.byPlayer(player.getUniqueId());
        player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Canal de " + TwitchText.HIGHLIGHT + config.channel);
        if (account == null) {
            player.sendMessage(TwitchText.GRAY + " No tienes una cuenta de Twitch vinculada. Usa " + TwitchText.HIGHLIGHT + "/twitch vincular" + TwitchText.GRAY + ".");
            return;
        }
        player.sendMessage(TwitchText.GRAY + " Cuenta: " + TwitchText.TEXT + account.login);
        player.sendMessage(TwitchText.GRAY + " Estado: " + describe(account));
        player.sendMessage(TwitchText.GRAY + " Kit: " + kitState(account));
        if (account.sub || player.isOp()) {
            player.sendMessage(TwitchText.GRAY + " Fly: " + (TwitchFly.isActive(player) ? TwitchText.OK + "activado" : TwitchText.TEXT + "desactivado")
                    + TwitchText.GRAY + " (/fly)");
        }
    }

    String describe(TwitchAccount account) {
        String state;
        if (account.sub) {
            state = TwitchText.OK + "Sub " + account.tierName() + (account.gift ? TwitchText.GRAY + " (regalo" + (account.gifter.isBlank() ? "" : " de " + account.gifter) + ")" : "");
            if (account.vip) state += TwitchText.GRAY + " · " + TwitchText.OK + "VIP";
        } else if (account.vip) {
            state = TwitchText.OK + "VIP";
        } else {
            state = TwitchText.TEXT + "sin sub ni VIP";
        }
        if (account.isSimulated()) state += TwitchText.HIGHLIGHT + " (prueba)";
        else state += TwitchText.GRAY + " · revisado " + TwitchText.ago(account.checkedAt);
        return state;
    }

    String kitState(TwitchAccount account) {
        long month = config.month;
        if (account.sub) {
            if (account.subRacha.puedeReclamar(month)) return TwitchText.OK + "¡disponible! Usa /twitch kit (" + TwitchKits.subKit(account.subKits).title + ")";
            return TwitchText.TEXT + "el próximo en " + TwitchText.HIGHLIGHT + TwitchText.duration(account.subRacha.falta(month)) + TwitchText.TEXT + " de sub activa";
        }
        if (account.vip) {
            if (account.vipRacha.puedeReclamar(month)) return TwitchText.OK + "¡disponible! Usa /twitch kit (Kit VIP)";
            return TwitchText.TEXT + "el próximo en " + TwitchText.HIGHLIGHT + TwitchText.duration(account.vipRacha.falta(month)) + TwitchText.TEXT + " de VIP";
        }
        return TwitchText.GRAY + "necesitas sub o VIP en el canal";
    }

    // ---------------------------------------------------------------- Jugadores

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TwitchAccount account = store.byPlayer(uuid);
        if (account != null && !player.getName().equals(account.playerName)) {
            account.playerName = player.getName();
            store.markDirty();
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            TwitchAccount current = store.byPlayer(uuid);
            // Con una revisión vieja (el canal sin conectar) no se le cambia el rango hasta saber de nuevo
            if (current == null || fresh(current)) roles.apply(player, current);
            if (current != null && !current.isSimulated() && channel.connected()
                    && System.currentTimeMillis() - current.checkedAt > 2 * 60 * 1000L) {
                check(List.of(current));
            }
            if (config.isAdmin(player)) {
                if (!api.configured()) player.sendMessage(TwitchText.PREFIX + TwitchText.HIGHLIGHT + "Falta el client_id de Twitch en twitch.yml.");
                else if (!channel.connected()) player.sendMessage(TwitchText.PREFIX + TwitchText.HIGHLIGHT + "El canal no está conectado: /twitchadmin canal conectar");
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelLink(event.getPlayer().getUniqueId());
        claiming.remove(event.getPlayer().getUniqueId());
    }

    // ---------------------------------------------------------------- Recargar y apagar

    void reload() {
        config = TwitchConfig.load(plugin);
        api = new TwitchApi(config.clientId, config.clientSecret);
        channel.useApi(api);
        schedulePoll();
    }

    void saveAsync() {
        String data = store.serialize();
        long sequence = ++saveSequence;
        if (shutdown || !plugin.isEnabled()) {
            write(sequence, data);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(sequence, data));
    }

    // Si dos guardados se cruzan, el más viejo no pisa al más nuevo
    private synchronized void write(long sequence, String data) {
        if (sequence < writtenSequence) return;
        writtenSequence = sequence;
        store.write(data);
    }

    public void shutdown() {
        shutdown = true;
        if (pollTask != null) pollTask.cancel();
        if (saveTask != null) saveTask.cancel();
        for (CompletableFuture<?> flow : pendingLinks.values()) flow.cancel(false);
        pendingLinks.clear();
        if (pendingChannel != null) pendingChannel.cancel(false);
        fly.shutdown();
        write(++saveSequence, store.serialize());
    }

    void runSync(Runnable task) {
        sync(task);
    }

    void refreshRole(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) roles.apply(player, store.byPlayer(uuid));
    }

    // Vuelve al hilo del server; false si el plugin ya se apagó
    private boolean sync(Runnable task) {
        if (shutdown || !plugin.isEnabled()) return false;
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return true;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    static String message(Throwable error) {
        Throwable cause = TwitchApi.unwrap(error);
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
