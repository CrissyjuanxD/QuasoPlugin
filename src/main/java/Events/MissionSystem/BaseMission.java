package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public abstract class BaseMission implements Mission, Listener {
    private static final String C_LABEL = "#FFCC99";
    private static final String C_VALUE = "#FFA07A";
    private final ActionBarHandler actionBar;
    private final SuccessNotification success;

    protected final JavaPlugin plugin;
    protected final MissionHandler handler;
    private final int number;
    private final String name;
    private final String description;
    private final MissionDifficulty difficulty;
    private final int coins;
    private int parent;
    private final Map<String, MissionObjective> objectives = new LinkedHashMap<>();

    protected BaseMission(JavaPlugin plugin, MissionHandler handler, int number, String name,
                          MissionDifficulty difficulty, int coins, String description) {
        this.plugin = plugin;
        this.handler = handler;
        this.number = number;
        this.name = name;
        this.difficulty = difficulty;
        this.coins = coins;
        this.description = wrap(description);
        actionBar = ActionBarHandler.get(plugin);
        success = new SuccessNotification(plugin);
    }

    protected void extraOf(int baseMission) {
        this.parent = baseMission;
    }

    protected MissionObjective counter(String key, String label, int target) {
        return register(new MissionObjective(key, label, target, MissionObjective.Format.NUMBER));
    }

    protected MissionObjective flag(String key, String label) {
        return register(new MissionObjective(key, label, 1, MissionObjective.Format.FLAG));
    }

    protected MissionObjective timer(String key, String label, int seconds) {
        return register(new MissionObjective(key, label, seconds, MissionObjective.Format.TIME));
    }

    private MissionObjective register(MissionObjective objective) {
        objectives.put(objective.key(), objective);
        return objective;
    }

    // Los 2 objetos de la recompensa (cada uno puede ser varios stacks); las DinoCoins y la XP las pone MissionRewards
    protected abstract List<List<ItemStack>> rewardItems();

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public int getMissionNumber() { return number; }
    @Override public int getParentMission() { return parent; }
    @Override public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) check(player);
    }

    @Override
    public List<ItemStack> getRewards() {
        return MissionRewards.chest(coins, difficulty, rewardItems());
    }

    public MissionDifficulty getDifficulty() { return difficulty; }
    public int getCoins() { return coins; }
    public Collection<MissionObjective> getObjectives() { return objectives.values(); }

    // Solo cuenta si la misión está activa, el jugador ya cargó sus datos y todavía no la completó
    protected boolean tracking(Player player) {
        if (player == null || !handler.isMissionActive(player, number)) return false;
        MissionData data = handler.getData(player, number);
        return data.isActive() && !data.isCompleted();
    }

    protected MissionData data(Player player) {
        return handler.getData(player, number);
    }

    protected void save(Player player, MissionData data) {
        handler.saveData(player, number, data);
    }

    // Las misiones que miden algo en vivo (estadísticas, inventario) lo sobrescriben
    protected int value(Player player, MissionData data, MissionObjective objective) {
        return data.getProgressInt(objective.key());
    }

    protected void add(Player player, String key, int amount) {
        update(player, key, current -> current + amount);
    }

    protected void set(Player player, String key, int value) {
        update(player, key, current -> value);
    }

    protected void raise(Player player, String key, int value) {
        update(player, key, current -> Math.max(current, value));
    }

    protected void mark(Player player, String key) {
        update(player, key, current -> Integer.MAX_VALUE);
    }

    private void update(Player player, String key, IntUnaryOperator operation) {
        if (!tracking(player)) return;
        MissionObjective objective = objectives.get(key);
        if (objective == null) {
            plugin.getLogger().warning("Misión " + number + ": el objetivo '" + key + "' no existe.");
            return;
        }

        MissionData data = data(player);
        int old = data.getProgressInt(key);
        int now = (int) Math.max(0, Math.min(objective.target(), (long) operation.applyAsInt(old)));
        if (now == old) return;

        data.setProgressValue(key, now);
        save(player, data);

        // También se anuncia el último objetivo antes de entregar la recompensa de la misión.
        showProgress(player, data, objective, now >= objective.target());
        check(player);
    }

    // Si ya cumplió todos los objetivos la completa y entrega la ficha
    protected boolean check(Player player) {
        if (!tracking(player)) return false;
        MissionData data = data(player);
        boolean completed = true;
        boolean changed = false;
        for (MissionObjective objective : objectives.values()) {
            int current = Math.max(0, Math.min(objective.target(), value(player, data, objective)));
            if (current != data.getProgressInt(objective.key())) {
                // Estadísticas y logros se calculan en vivo: también deben entrar en la cola.
                data.setProgressValue(objective.key(), current);
                changed = true;
                showProgress(player, data, objective, current >= objective.target());
            }
            if (current < objective.target()) completed = false;
        }
        if (changed) save(player, data);
        if (!completed) return false;
        success.showSuccess(player);
        handler.completeMission(player, number);
        return true;
    }

    // Cuenta una estadística de Minecraft desde que el jugador empezó la misión
    protected void statSince(Player player, String key, Statistic statistic, int divisor) {
        MissionData data = data(player);
        String baseKey = "base_" + key;
        int current = player.getStatistic(statistic);
        if (data.getProgressValue(baseKey) == null) {
            data.setProgressValue(baseKey, current);
            save(player, data);
        }
        set(player, key, (current - data.getProgressInt(baseKey)) / divisor);
    }

    protected void sendBar(Player player, String message) {
        actionBar.sendProgress(player, "mission:" + number + ":info", ChatColor.GOLD + "۞ " + message);
    }

    protected void showProgress(Player player, MissionData data, MissionObjective changed, boolean force) {
        String key = "mission:" + number + ":" + changed.key();
        String message = ChatColor.GOLD + "۞ " + ChatColor.of(C_LABEL) + "[" + handler.tag(number) + "] "
                + format(changed, value(player, data, changed));
        if (force) actionBar.sendNotification(player, key, message);
        else actionBar.sendProgress(player, key, message);
    }

    private String format(MissionObjective objective, int value) {
        boolean done = value >= objective.target();
        if (objective.format() == MissionObjective.Format.FLAG) {
            return (done ? ChatColor.GREEN + "✔ " : ChatColor.of(C_VALUE) + "✖ ") + ChatColor.of(C_LABEL) + objective.label();
        }
        return ChatColor.of(C_LABEL) + objective.label() + ": " + (done ? ChatColor.GREEN : ChatColor.of(C_VALUE)) + objective.formatValue(value);
    }

    // Líneas de progreso para el menú de misiones (de a dos por línea si son muchas)
    public List<String> progressLines(Player player, MissionData data) {
        List<String> lines = new ArrayList<>();
        if (objectives.isEmpty()) return lines;

        lines.add(ChatColor.of("#F0E68C") + "Progreso:");
        List<String> parts = new ArrayList<>();
        for (MissionObjective objective : objectives.values()) {
            int value = value(player, data, objective);
            boolean done = value >= objective.target();
            if (objective.format() == MissionObjective.Format.FLAG) {
                parts.add(ChatColor.of(done ? "#98FB98" : "#D3D3D3") + "- " + objective.label());
            } else {
                parts.add(ChatColor.of("#DDA0DD") + "- " + objective.label() + ": " + ChatColor.of(done ? "#98FB98" : "#FFA07A")
                        + objective.formatCurrent(value) + ChatColor.of("#D3D3D3") + "/" + objective.formatTarget());
            }
        }

        if (parts.size() <= 6) {
            lines.addAll(parts);
        } else {
            for (int i = 0; i < parts.size(); i += 2) {
                lines.add(i + 1 < parts.size() ? parts.get(i) + ChatColor.DARK_GRAY + " │ " + parts.get(i + 1) : parts.get(i));
            }
        }
        return lines;
    }

    // Cada cuántos segundos se llama a tick() mientras la misión está activa (0 = nunca)
    protected int tickSeconds() {
        return 0;
    }

    protected void tick(Player player) {}

    void runTick(Player player, long second) {
        int every = tickSeconds();
        if (every <= 0 || second % every != 0 || !tracking(player)) return;
        tick(player);
    }

    // Para limpiar lo que la misión guarda en memoria de cada jugador
    public void onQuit(Player player) {}

    private static String wrap(String text) {
        StringBuilder result = new StringBuilder();
        for (String paragraph : text.split("\n")) {
            if (!result.isEmpty()) result.append("\n");
            int lineLength = 0;
            for (String word : paragraph.split(" ")) {
                if (lineLength > 0 && lineLength + word.length() + 1 > 32) {
                    result.append("\n");
                    lineLength = 0;
                } else if (lineLength > 0) {
                    result.append(" ");
                    lineLength++;
                }
                result.append(word);
                lineLength += word.length();
            }
        }
        return result.toString();
    }
}
