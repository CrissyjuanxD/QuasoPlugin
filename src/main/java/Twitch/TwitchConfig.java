package Twitch;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// twitch.yml: la app de Twitch, el canal, los admins de los kits y los tiempos
final class TwitchConfig {

    private static final long DAY = 24L * 60 * 60 * 1000;

    final String clientId;
    final String clientSecret;
    final String channel;
    final int checkMinutes;
    final long month;
    private final List<String> admins;
    private final Set<String> adminsLower = new HashSet<>();
    private final Set<String> blockedWorlds = new HashSet<>();

    private TwitchConfig(YamlConfiguration yml) {
        clientId = yml.getString("client_id", "").trim();
        clientSecret = yml.getString("client_secret", "").trim();
        channel = yml.getString("canal", "crosszy").trim().toLowerCase(Locale.ROOT);
        checkMinutes = Math.max(2, yml.getInt("revision_minutos", 10));
        month = Math.max(1, yml.getInt("dias_por_mes", 32)) * DAY;
        admins = new ArrayList<>(yml.getStringList("administradores"));
        for (String admin : admins) adminsLower.add(admin.toLowerCase(Locale.ROOT));
        for (String world : yml.getStringList("fly.mundos_bloqueados")) blockedWorlds.add(world.toLowerCase(Locale.ROOT));
    }

    static TwitchConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "twitch.yml");
        if (!file.exists()) plugin.saveResource("twitch.yml", false);
        return new TwitchConfig(YamlConfiguration.loadConfiguration(file));
    }

    // La consola siempre; un jugador solo si su nick está en administradores (los operadores no)
    boolean isAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        return adminsLower.contains(player.getName().toLowerCase(Locale.ROOT));
    }

    List<String> admins() {
        return admins;
    }

    boolean worldBlocked(String world) {
        return blockedWorlds.contains(world.toLowerCase(Locale.ROOT));
    }

    // Lo que puede tardar entre dos revisiones sin que se corte la cuenta de días (un reinicio no la corta)
    long maxGap() {
        return Math.max(2 * 60 * 60 * 1000L, checkMinutes * 3L * 60 * 1000L);
    }
}
