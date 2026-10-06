package BloodMoon;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
        try (var stream = plugin.getResource("bloodmoon/mensajes.yml")) {
            if (stream == null) throw new IOException("Faltan los mensajes de BloodMoon");
            locales.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            locales.options().copyDefaults(true);
            locales.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudieron guardar los mensajes de BloodMoon: " + ex.getMessage());
        }
    }

    public String GetLocaleString(String id) {
        String value = locales.getString(id, "&#EF9292No se encontró el mensaje: " + id);
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
    public static void MessageAllLocale(String id, String[] args, String[] replacements, World world) {
        String message = format(id, args, replacements);
        if (!message.isEmpty()) for (Player player : world.getPlayers()) player.sendMessage(message);
    }
    public static void BroadcastLocale(String id, String[] args, String[] replacements) {
        String message = format(id, args, replacements);
        if (!message.isEmpty()) Bukkit.broadcastMessage(message);
    }
    public static void actionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ORANGE + "۞ " + message));
    }
    public static void amuletMessage(Player player, String message) {
        player.sendMessage(ORANGE + "Bloodmoon > " + RED + message);
    }
}
