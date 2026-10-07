package Twitch;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

// twitch/datos.yml: las cuentas vinculadas con sus rachas, el historial de kits y el estado del /fly.
// Todo se toca desde el hilo del server; el archivo se escribe entero en uno temporal y después se reemplaza
final class TwitchStore {

    private static final int MAX_HISTORY = 1000;

    record Entry(long time, String player, String account, String kit, String by) {}

    private final File file;
    private final Logger logger;
    private final Map<String, TwitchAccount> accounts = new LinkedHashMap<>();
    private final Map<UUID, TwitchAccount> linked = new HashMap<>();
    private final Map<UUID, TwitchAccount> simulated = new HashMap<>();
    private final List<Entry> history = new ArrayList<>();
    final Set<UUID> flyActive = new HashSet<>();
    boolean flyEnabled = true;
    private boolean dirty;

    TwitchStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    // La cuenta que cuenta para el jugador: la simulada de /twitchadmin simular si tiene una, si no la vinculada
    TwitchAccount byPlayer(UUID player) {
        TwitchAccount account = simulated.get(player);
        return account != null ? account : linked.get(player);
    }

    TwitchAccount linkedTo(UUID player) {
        return linked.get(player);
    }

    TwitchAccount simulatedFor(UUID player) {
        return simulated.get(player);
    }

    TwitchAccount byTwitchId(String twitchId) {
        return accounts.get(twitchId);
    }

    // Busca por nick de Minecraft o por usuario de Twitch (sin importar mayúsculas)
    TwitchAccount find(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (TwitchAccount account : accounts.values()) {
            if (account.isSimulated() || account.player == null) continue;
            if (lower.equals(lower(account.playerName))) return account;
        }
        for (TwitchAccount account : accounts.values()) {
            if (account.isSimulated() || account.player == null) continue;
            if (lower.equals(lower(account.login))) return account;
        }
        for (TwitchAccount account : simulated.values()) {
            if (lower.equals(lower(account.playerName))) return account;
        }
        return null;
    }

    Collection<TwitchAccount> linkedAccounts() {
        return linked.values();
    }

    Collection<TwitchAccount> simulatedAccounts() {
        return simulated.values();
    }

    // Una cuenta de Twitch con un solo jugador y un jugador con una sola cuenta
    void link(TwitchAccount account, UUID player, String playerName) {
        TwitchAccount previous = linked.remove(player);
        if (previous != null && previous != account) previous.player = null;
        if (account.player != null && !account.player.equals(player)) linked.remove(account.player);
        account.player = player;
        account.playerName = playerName;
        account.linkedAt = System.currentTimeMillis();
        accounts.put(account.twitchId, account);
        linked.put(player, account);
        markDirty();
    }

    // La cuenta queda guardada sin jugador: si se vuelve a vincular sigue sabiendo qué kits ya cobró
    void unlink(TwitchAccount account) {
        if (account.player != null) linked.remove(account.player, account);
        account.player = null;
        markDirty();
    }

    void putSimulated(TwitchAccount account) {
        accounts.put(account.twitchId, account);
        simulated.put(account.player, account);
        markDirty();
    }

    void removeSimulated(UUID player) {
        TwitchAccount account = simulated.remove(player);
        if (account != null) accounts.remove(account.twitchId);
        markDirty();
    }

    void log(String player, String account, String kit, String by) {
        history.add(new Entry(System.currentTimeMillis(), player, account, kit, by == null ? "" : by));
        while (history.size() > MAX_HISTORY) history.remove(0);
        markDirty();
    }

    // Del más nuevo al más viejo; con nombre, solo los de ese jugador o esa cuenta
    List<Entry> history(String filter) {
        List<Entry> result = new ArrayList<>();
        String lower = filter == null ? null : filter.toLowerCase(Locale.ROOT);
        for (int i = history.size() - 1; i >= 0; i--) {
            Entry entry = history.get(i);
            if (lower == null || lower.equals(lower(entry.player())) || lower.equals(lower(entry.account()))) result.add(entry);
        }
        return result;
    }

    void markDirty() {
        dirty = true;
    }

    boolean dirty() {
        return dirty;
    }

    // Arma el YAML en el hilo del server; escribirlo puede ir en otro hilo
    String serialize() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().setHeader(List.of("Datos del sistema de Twitch. Se escribe solo: para cambiar algo usa /twitchadmin"));
        yml.set("fly.activado", flyEnabled);
        List<String> fly = new ArrayList<>();
        for (UUID uuid : flyActive) fly.add(uuid.toString());
        yml.set("fly.usando", fly);
        ConfigurationSection section = yml.createSection("cuentas");
        for (TwitchAccount account : accounts.values()) account.save(section.createSection(account.twitchId));
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Entry entry : history) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fecha", entry.time());
            map.put("jugador", entry.player());
            map.put("cuenta", entry.account());
            map.put("kit", entry.kit());
            map.put("por", entry.by());
            entries.add(map);
        }
        yml.set("historial", entries);
        dirty = false;
        return yml.saveToString();
    }

    void write(String data) {
        try {
            Path target = file.toPath();
            Files.createDirectories(target.getParent());
            Path temp = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(temp, data, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.warning("[Twitch] No se pudo guardar " + file.getName() + ": " + e.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        flyEnabled = yml.getBoolean("fly.activado", true);
        for (String uuid : yml.getStringList("fly.usando")) {
            try {
                flyActive.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ConfigurationSection section = yml.getConfigurationSection("cuentas");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection data = section.getConfigurationSection(id);
                if (data == null) continue;
                TwitchAccount account = TwitchAccount.load(id, data);
                if (account.isSimulated() && account.player == null) continue;
                accounts.put(id, account);
                if (account.player == null) continue;
                if (account.isSimulated()) simulated.put(account.player, account);
                else linked.put(account.player, account);
            }
        }
        for (Map<?, ?> map : yml.getMapList("historial")) {
            history.add(new Entry(number(map.get("fecha")), string(map.get("jugador")), string(map.get("cuenta")),
                    string(map.get("kit")), string(map.get("por"))));
        }
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
