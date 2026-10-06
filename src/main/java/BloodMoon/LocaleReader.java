package BloodMoon;

import Handlers.ActionBarHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Mensajes configurables en español; no importa los avisos ingleses del plugin externo. */
public final class LocaleReader {
    public static final String ORANGE = ChatColor.of("#F4B183").toString();
    public static final String RED = ChatColor.of("#EF9292").toString();
    public static final String PEACH = ChatColor.of("#FFD2AE").toString();
    public static final String CORAL = ChatColor.of("#F7AAA1").toString();
    public static final String LIME = ChatColor.of("#C9F5A5").toString();
    public static final String LIME_TITLE = ChatColor.of("#AEE87A").toString();
    public static final String PREFIX = ORANGE + "Bloodmoon " + ChatColor.GRAY + "► " + RED;
    private static final Pattern HEX = Pattern.compile("&#([a-fA-F0-9]{6})");
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration locales;

    public LocaleReader(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "bloodmoon/mensajes.yml");
        RefreshLocales();
    }

    public void RefreshLocales() {
        locales = YamlConfiguration.loadConfiguration(file);
        try (var stream = plugin.getResource("bloodmoon-defaults/mensajes.yml")) {
            if (stream == null) throw new IOException("Faltan los mensajes de BloodMoon");
            var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            migratePreviousDefaults(defaults);
            locales.setDefaults(defaults);
            locales.options().copyDefaults(true);
            locales.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudieron guardar los mensajes de BloodMoon: " + ex.getMessage());
        }
    }

    private void migratePreviousDefaults(YamlConfiguration defaults) throws IOException {
        // Solo cambia mensajes idénticos a la versión anterior; conserva ediciones y %void%.
        for (String resource : java.util.List.of("mensajes-v1.yml", "mensajes-v2.yml")) {
            try (var previous = plugin.getResource("bloodmoon-defaults/" + resource)) {
                if (previous == null) continue;
                var oldDefaults = YamlConfiguration.loadConfiguration(new InputStreamReader(previous, StandardCharsets.UTF_8));
                for (String id : oldDefaults.getKeys(false)) {
                    String oldValue = oldDefaults.getString(id);
                    String newValue = defaults.getString(id);
                    if (oldValue != null && newValue != null && oldValue.equals(locales.getString(id))) {
                        locales.set(id, newValue);
                    }
                }
            }
        }
    }

    public String GetLocaleString(String id) {
        String value = locales.getString(id);
        if (value == null) value = "&#EF9292No se encontró el mensaje: " + id;
        if (value.equals("%void%")) return "";
        return color(value.replace("$n", "\n"));
    }

    public static String color(String value) {
        value = HEX.matcher(value).replaceAll(result -> ChatColor.of("#" + result.group(1)).toString());
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static String format(String id, String[] args, String[] replacements) {
        String text = BloodMoon.GetInstance().getLocaleReader().GetLocaleString(id);
        if (args != null && replacements != null) {
            for (int i = 0; i < Math.min(args.length, replacements.length); i++) text = text.replace(args[i], replacements[i]);
        }
        return text;
    }
    public static void MessageLocale(String id, String[] args, String[] replacements, CommandSender sender) {
        String message = format(id, args, replacements);
        if (!message.isEmpty()) sender.sendMessage(message);
    }
    public static void CommandLocale(String id, String[] args, String[] replacements, CommandSender sender) {
        commandMessage(sender, format(id, args, replacements));
    }
    public static void commandMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        String plain = ChatColor.stripColor(message);
        String prefix = "Bloodmoon ► ";
        if (plain.startsWith(prefix)) {
            sender.sendMessage(LIME_TITLE + "Bloodmoon " + ChatColor.GRAY + "► " + LIME + plain.substring(prefix.length()));
        } else {
            sender.sendMessage(LIME + plain.replace("►", ChatColor.GRAY + "►" + LIME));
        }
    }
    public static void MessageAllLocale(String id, String[] args, String[] replacements, World world) {
        String message = format(id, args, replacements);
        if (!message.isEmpty()) for (Player player : world.getPlayers()) player.sendMessage(message);
    }
    public static void BroadcastLocale(String id, String[] args, String[] replacements) {
        String message = format(id, args, replacements);
        if (!message.isEmpty()) Bukkit.broadcastMessage(message);
    }
    public static void actionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        ActionBarHandler.get(BloodMoon.GetInstance().getPlugin())
                .sendNotification(player, "bloodmoon:" + message, ORANGE + "۞ " + message);
    }
    public static void amuletMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }
}
