package Commands;

import imp.crissyjuanxd.QuasoPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Homes implements CommandExecutor, TabCompleter, Listener {

    private static final int MAX_HOMES = 10;
    private static final int DIALOG_PROTOCOL = 771; // Java 1.21.6 introduce los diálogos.
    private static final TextColor GOLD = TextColor.color(0xE28B20);
    private static final TextColor CREAM = TextColor.color(0xF4D990);
    private static final TextColor GREEN = TextColor.color(0xC9DC8A);
    private static final TextColor ORANGE = TextColor.color(0xDD6110);
    private static final TextColor MUTED = TextColor.color(0xAAA391);

    private final QuasoPlugin plugin;
    private File homesFile;
    private FileConfiguration homesConfig;

    private final Map<UUID, PendingTeleport> teleportingPlayers = new HashMap<>();

    private static final class PendingTeleport {
        private final Location origin;
        private BukkitTask task;

        private PendingTeleport(Location origin) { this.origin = origin.clone(); }
        private void cancel() { if (task != null) task.cancel(); }
    }

    public Homes(QuasoPlugin plugin) {
        this.plugin = plugin;
        createHomesConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // /sethome, /home y /delhome; máximo 10 homes por jugador y el tp tarda 5 segundos
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }

        Player player = (Player) sender;
        String homeName = (args.length > 0) ? args[0].toLowerCase(Locale.ROOT) : "base";

        if (command.getName().equalsIgnoreCase("sethome")) {
            if (!isUsableHomeName(homeName)) {
                player.sendMessage(ChatColor.of("#F4D990") + "Usa un nombre de hasta 32 letras, números, guiones o guiones bajos. 'list' está reservado para /home list.");
                return true;
            }
            Set<String> playerHomes = getPlayerHomes(player);

            if (playerHomes.size() >= MAX_HOMES && !playerHomes.contains(homeName)) {
                player.sendMessage(ChatColor.RED + "Has alcanzado el límite máximo de 10 homes. Usa /delhome para borrar alguna.");
                return true;
            }

            Location loc = player.getLocation();
            String worldName = loc.getWorld().getName();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            String saveFormat = worldName + ", " + x + ", " + y + ", " + z;

            homesConfig.set("Homes." + player.getName() + "." + homeName, saveFormat);
            saveHomesConfig();

            String simbolo = ChatColor.of("#E28B20") + "" + ChatColor.BOLD + "\u06de";
            String texto1 = ChatColor.of("#F4D990") + " Has establecido el home " + ChatColor.of("#C9DC8A") + homeName + ChatColor.of("#F4D990") + " en";
            String coords = ChatColor.of("#DD6110") + "" + ChatColor.BOLD + " " + x + " " + y + " " + z;
            String saltoLinea = "\n";
            String texto2 = ChatColor.of("#F4D990") + "Para borrar este home, usa";
            String comandoDelHome = ChatColor.of("#C9DC8A") + "" + ChatColor.BOLD + " /delhome " + homeName;

            player.sendMessage(simbolo + texto1 + coords + saltoLinea + texto2 + comandoDelHome);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

            return true;
        }

        if (command.getName().equalsIgnoreCase("delhome")) {
            Set<String> playerHomes = getPlayerHomes(player);

            if (!playerHomes.contains(homeName)) {
                player.sendMessage(ChatColor.RED + "No tienes un home llamado '" + homeName + "'.");
                return true;
            }

            homesConfig.set("Homes." + player.getName() + "." + homeName, null);
            saveHomesConfig();
            player.sendMessage(ChatColor.GREEN + "El home '" + homeName + "' ha sido eliminado.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);

            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            if (homeName.equals("list")) {
                if (args.length > 1) {
                    player.sendMessage(ChatColor.of("#F4D990") + "Usa /home list para ver tus homes.");
                } else {
                    showHomeList(player);
                }
                return true;
            }
            Set<String> playerHomes = getPlayerHomes(player);

            if (playerHomes.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No has establecido ningún punto de casa. Usa /sethome.");
                return true;
            }

            if (!playerHomes.contains(homeName)) {
                player.sendMessage(ChatColor.RED + "No tienes un home llamado '" + homeName + "'. Tus homes: " + ChatColor.YELLOW + String.join(", ", playerHomes));
                return true;
            }

            String data = homesConfig.getString("Homes." + player.getName() + "." + homeName);
            if (data == null) return true;

            String[] parts = data.split(", ");
            if (parts.length < 4) {
                player.sendMessage(ChatColor.RED + "Error en los datos de la casa. Vuelve a establecerla con /sethome.");
                return true;
            }

            String worldName = parts[0];
            double x, y, z;
            try {
                x = Double.parseDouble(parts[1]) + 0.5;
                y = Double.parseDouble(parts[2]);
                z = Double.parseDouble(parts[3]) + 0.5;
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                    throw new NumberFormatException("Coordenadas no finitas");
                }
            } catch (NumberFormatException malformed) {
                player.sendMessage(ChatColor.RED + "Error en los datos de la casa. Vuelve a establecerla con /sethome.");
                return true;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(ChatColor.RED + "El mundo de tu casa no existe o no está cargado.");
                return true;
            }

            Location homeLoc = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

            PendingTeleport previous = teleportingPlayers.remove(player.getUniqueId());
            if (previous != null) previous.cancel();

            if (player.isOp()) {
                teleportHome(player, homeLoc, homeName);
                return true;
            }

            PendingTeleport pending = new PendingTeleport(player.getLocation());
            teleportingPlayers.put(player.getUniqueId(), pending);

            player.sendMessage(ChatColor.of("#F4D990") + "Teletransportándote a " + ChatColor.of("#C9DC8A") + homeName + ChatColor.of("#F4D990") + " en 5 segundos. ¡No te muevas y no recibas daño!");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);

            pending.task = new BukkitRunnable() {
                int count = 5;

                @Override
                public void run() {
                    // Un callback antiguo no puede ejecutar un TP ya cancelado o reemplazado.
                    if (teleportingPlayers.get(player.getUniqueId()) != pending) {
                        pending.cancel();
                        return;
                    }
                    if (!player.isOnline()) {
                        teleportingPlayers.remove(player.getUniqueId());
                        pending.cancel();
                        return;
                    }
                    if (hasMoved(pending.origin, player.getLocation())) {
                        cancelTeleport(player, "¡Teletransporte cancelado por moverte!");
                        return;
                    }

                    if (count > 0) {
                        String color = count <= 2 ? "§c" : "§e";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bTeletransportando en " + color + count + "§b..."));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                        count--;
                    } else {
                        teleportingPlayers.remove(player.getUniqueId());
                        pending.cancel();
                        teleportHome(player, homeLoc, homeName);
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            Set<String> homes = new HashSet<>(getPlayerHomes(player));
            if (command.getName().equalsIgnoreCase("home")) homes.add("list");
            List<String> completions = new ArrayList<>();

            StringUtil.copyPartialMatches(args[0], homes, completions);
            Collections.sort(completions);
            return completions;
        }
        return Collections.emptyList();
    }

    private boolean isUsableHomeName(String name) {
        // Los botones solo ejecutan un argumento literal; nunca comandos guardados en el YAML.
        return !name.equals("list") && name.matches("[\\p{L}\\p{N}_-]{1,32}");
    }

    private void showHomeList(Player player) {
        List<String> names = getPlayerHomes(player).stream().sorted().limit(MAX_HOMES).toList();
        int protocol = clientProtocol(player);
        // Los snapshots usan IDs especiales que no permiten deducir qué interfaces admiten.
        if (protocol >= DIALOG_PROTOCOL && protocol < (1 << 30)) {
            player.showDialog(createHomeDialog(player, names));
        } else {
            showHomeListInChat(player, names);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.5f);
    }

    int clientProtocol(Player player) {
        Plugin via = plugin.getServer().getPluginManager().getPlugin("ViaVersion");
        if (via != null && via.isEnabled()) {
            try {
                // Via puede traducir el handshake antes de que lo lea Paper: su API tiene prioridad.
                ClassLoader loader = via.getClass().getClassLoader();
                Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via", true, loader);
                Class<?> apiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI", true, loader);
                Object api = viaClass.getMethod("getAPI").invoke(null);
                return ((Number) apiClass.getMethod("getPlayerVersion", UUID.class)
                        .invoke(api, player.getUniqueId())).intValue();
            } catch (ReflectiveOperationException | LinkageError | ClassCastException unavailable) {
                // Si no se puede conocer el cliente traducido, el chat funciona en todas las versiones.
                return -1;
            }
        }
        return player.getProtocolVersion();
    }

    private Dialog createHomeDialog(Player player, List<String> names) {
        List<ActionButton> buttons = new ArrayList<>(MAX_HOMES);
        for (int slot = 0; slot < MAX_HOMES; slot++) {
            String name = slot < names.size() ? names.get(slot) : null;
            boolean usable = name != null && isUsableHomeName(name);
            Component label = Component.text((slot + 1) + " · ", GOLD)
                    .append(Component.text(name == null ? "Disponible" : name, usable ? GREEN : MUTED));
            Component tooltip = name == null
                    ? Component.text("Guarda un home con /sethome <nombre>.", CREAM)
                    : usable ? homeTooltip(player, name)
                    : Component.text("Nombre antiguo no compatible. Bórralo y guarda un home con otro nombre.", CREAM);
            buttons.add(ActionButton.create(label, tooltip, 150,
                    usable ? DialogAction.staticAction(ClickEvent.runCommand("/home " + name)) : null));
        }
        Component title = Component.text("۞ ", GOLD).decorate(TextDecoration.BOLD)
                .append(Component.text("Tus homes", CREAM).decorate(TextDecoration.BOLD));
        Component introduction = Component.text(names.size() + " de " + MAX_HOMES + " homes guardados", GREEN)
                .append(Component.newline())
                .append(Component.text("Elige tu destino. " + teleportInstructions(player), CREAM));
        DialogBase base = DialogBase.builder(title).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(introduction, 310))).build();
        ActionButton close = ActionButton.create(Component.text("Cerrar", ORANGE), null, 150, null);
        DialogType type = DialogType.multiAction(buttons, close, 2);
        return Dialog.create(builder -> builder.empty().base(base).type(type));
    }

    private void showHomeListInChat(Player player, List<String> names) {
        player.sendMessage(Component.text("۞ Tus homes ", GOLD).decorate(TextDecoration.BOLD)
                .append(Component.text("(" + names.size() + "/" + MAX_HOMES + ")", CREAM)));
        for (int slot = 0; slot < MAX_HOMES; slot++) {
            String name = slot < names.size() ? names.get(slot) : null;
            boolean usable = name != null && isUsableHomeName(name);
            Component line = Component.text("  " + (slot + 1) + " · ", GOLD)
                    .append(Component.text(name == null ? "Disponible" : name, usable ? GREEN : MUTED));
            if (usable) {
                line = line.append(Component.text("  [Ir]", ORANGE).decorate(TextDecoration.BOLD))
                        .clickEvent(ClickEvent.runCommand("/home " + name))
                        .hoverEvent(HoverEvent.showText(homeTooltip(player, name)));
            }
            player.sendMessage(line);
        }
        player.sendMessage(Component.text("Guarda un home con /sethome <nombre>. " + teleportInstructions(player), CREAM));
    }

    private String teleportInstructions(Player player) {
        return player.isOp() ? "Como operador, te teletransportas al instante."
                : "El viaje tarda 5 segundos; moverte o recibir daño lo cancela.";
    }

    private Component homeTooltip(Player player, String name) {
        String data = homesConfig.getString("Homes." + player.getName() + "." + name);
        return Component.text("Ir a " + name, GREEN)
                .append(Component.newline())
                .append(Component.text(data == null ? "Destino no disponible" : data, CREAM));
    }

    // Si le pegan mientras espera el tp se cancela
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!event.isCancelled() && event.getFinalDamage() > 0 && event.getEntity() instanceof Player player) {
            cancelTeleport(player, "¡Teletransporte cancelado por recibir daño!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        PendingTeleport pending = teleportingPlayers.get(event.getPlayer().getUniqueId());
        if (pending != null && hasMoved(pending.origin, event.getTo())) {
            cancelTeleport(event.getPlayer(), "¡Teletransporte cancelado por moverte!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        onPlayerMove(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PendingTeleport pending = teleportingPlayers.remove(event.getPlayer().getUniqueId());
        if (pending != null) pending.cancel();
    }

    private boolean hasMoved(Location from, Location to) {
        return to == null || !Objects.equals(from.getWorld(), to.getWorld())
                || from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
    }

    private void cancelTeleport(Player player, String message) {
        PendingTeleport pending = teleportingPlayers.remove(player.getUniqueId());
        if (pending == null) return;
        pending.cancel();
        player.sendMessage(ChatColor.RED + message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l¡Teletransporte Cancelado!"));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private void teleportHome(Player player, Location location, String homeName) {
        player.teleport(location);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l¡Teletransportado a " + homeName + "!"));
    }

    public void shutdown() {
        teleportingPlayers.values().forEach(PendingTeleport::cancel);
        teleportingPlayers.clear();
    }

    // Si el jugador tenía el formato viejo (un solo home) lo pasa a 'base'
    private Set<String> getPlayerHomes(Player player) {
        String path = "Homes." + player.getName();

        if (homesConfig.isString(path)) {
            String oldData = homesConfig.getString(path);
            homesConfig.set(path, null);
            homesConfig.set(path + ".base", oldData);
            saveHomesConfig();
        }

        if (homesConfig.isConfigurationSection(path)) {
            return homesConfig.getConfigurationSection(path).getKeys(false);
        }

        return new HashSet<>();
    }

    private void createHomesConfig() {
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            homesFile.getParentFile().mkdirs();
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void saveHomesConfig() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
