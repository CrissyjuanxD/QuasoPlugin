package Commands;

import imp.crissyjuanxd.QuasoPlugin;
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
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Homes implements CommandExecutor, TabCompleter, Listener {

    private final QuasoPlugin plugin;
    private File homesFile;
    private FileConfiguration homesConfig;

    private final Map<UUID, BukkitTask> teleportingPlayers = new HashMap<>();

    public Homes(QuasoPlugin plugin) {
        this.plugin = plugin;
        createHomesConfig();
        // Registramos el listener para cancelar el tp al recibir daño
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }

        Player player = (Player) sender;
        String homeName = (args.length > 0) ? args[0].toLowerCase() : "base";

        // --- COMANDO: /SETHOME ---
        if (command.getName().equalsIgnoreCase("sethome")) {
            Set<String> playerHomes = getPlayerHomes(player);

            if (playerHomes.size() >= 10 && !playerHomes.contains(homeName)) {
                player.sendMessage(ChatColor.RED + "Has alcanzado el límite máximo de 10 homes. Usa /delhome para borrar alguna.");
                return true;
            }

            // Guardar ubicación
            Location loc = player.getLocation();
            String worldName = loc.getWorld().getName();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            // Formato de guardado: "Mundo, x, y, z"
            String saveFormat = worldName + ", " + x + ", " + y + ", " + z;

            homesConfig.set("Homes." + player.getName() + "." + homeName, saveFormat);
            saveHomesConfig();

            // Mensajes con colores HEX
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

        // --- COMANDO: /DELHOME ---
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

        // --- COMANDO: /HOME ---
        if (command.getName().equalsIgnoreCase("home")) {
            Set<String> playerHomes = getPlayerHomes(player);

            if (playerHomes.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No has establecido ningún punto de casa. Usa /sethome.");
                return true;
            }

            if (!playerHomes.contains(homeName)) {
                player.sendMessage(ChatColor.RED + "No tienes un home llamado '" + homeName + "'. Tus homes: " + ChatColor.YELLOW + String.join(", ", playerHomes));
                return true;
            }

            // Leer datos del YML
            String data = homesConfig.getString("Homes." + player.getName() + "." + homeName);
            if (data == null) return true;

            String[] parts = data.split(", ");
            if (parts.length < 4) {
                player.sendMessage(ChatColor.RED + "Error en los datos de la casa. Vuelve a establecerla con /sethome.");
                return true;
            }

            // Parsear coordenadas y mundo
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]) + 0.5;
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]) + 0.5;

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(ChatColor.RED + "El mundo de tu casa no existe o no está cargado.");
                return true;
            }

            Location homeLoc = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

            // --- LÓGICA DE TELETRANSPORTE CON COUNTDOWN ---

            // Si ya se estaba tepeando a otro lugar, cancelamos el anterior
            if (teleportingPlayers.containsKey(player.getUniqueId())) {
                teleportingPlayers.get(player.getUniqueId()).cancel();
            }

            player.sendMessage(ChatColor.of("#F4D990") + "Teletransportándote a " + ChatColor.of("#C9DC8A") + homeName + ChatColor.of("#F4D990") + " en 5 segundos. ¡No te muevas y no recibas daño!");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.6f);

            BukkitTask tpTask = new BukkitRunnable() {
                int count = 5;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        teleportingPlayers.remove(player.getUniqueId());
                        this.cancel();
                        return;
                    }

                    if (count > 0) {
                        String color = count <= 2 ? "§c" : "§e";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bTeletransportando en " + color + count + "§b..."));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                        count--;
                    } else {
                        // TP Final
                        player.teleport(homeLoc);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l¡Teletransportado a " + homeName + "!"));

                        teleportingPlayers.remove(player.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // Ejecuta cada segundo (20 ticks)

            teleportingPlayers.put(player.getUniqueId(), tpTask);
            return true;
        }

        return false;
    }

    // --- AUTOCOMPLETADO (Tab Completer) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            Set<String> homes = getPlayerHomes(player);
            List<String> completions = new ArrayList<>();

            StringUtil.copyPartialMatches(args[0], homes, completions);
            Collections.sort(completions);
            return completions;
        }
        return Collections.emptyList();
    }

    // --- CANCELAR TP AL RECIBIR DAÑO ---
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (teleportingPlayers.containsKey(player.getUniqueId())) {
                teleportingPlayers.get(player.getUniqueId()).cancel();
                teleportingPlayers.remove(player.getUniqueId());

                player.sendMessage(ChatColor.RED + "¡Teletransporte cancelado por recibir daño!");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l¡Teletransporte Cancelado!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    // --- MÉTODOS PARA GESTIONAR EL ARCHIVO HOMES.YML ---

    private Set<String> getPlayerHomes(Player player) {
        String path = "Homes." + player.getName();

        // MIGRACIÓN: Si el path es un String significa que tiene el formato antiguo de 1 solo home. Lo pasamos a "base"
        if (homesConfig.isString(path)) {
            String oldData = homesConfig.getString(path);
            homesConfig.set(path, null); // Borrar viejo
            homesConfig.set(path + ".base", oldData); // Migrar a base
            saveHomesConfig();
        }

        // Devolver la lista de nombres de homes
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