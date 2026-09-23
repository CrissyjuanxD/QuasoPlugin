package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Achievement9 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Map<String, Integer> fallStartHeights = new HashMap<>();
    private final Map<String, Long> fallStartTimes = new HashMap<>();
    private final Map<String, Boolean> usedTotem = new HashMap<>();
    private static final int MAX_FALL_TIME = 10000;
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 255;

    public Achievement9(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Descenso al Inframundo";
    }

    @Override
    public String getDescription() {
        return "Cae de Y=255 a Y=1 en el Nether en menos de 10 segundos sin usar totems";
    }

    @Override
    public void initializePlayerData(String playerName) {
    }

    @Override
    public void checkCompletion(String playerName) {
    }

    // Empieza a contar al pasar por Y=255 cayendo; si se detiene antes de Y=1 o pasan 10 segundos se reinicia
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!eventHandler.isEventActive()) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        String playerName = player.getName();

        if (world.getEnvironment() != World.Environment.NETHER) {
            resetPlayerData(playerName);
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        if (data.getBoolean("players." + playerName + ".achievements.nether_fall.completed", false)) {
            resetPlayerData(playerName);
            return;
        }

        int currentY = event.getTo().getBlockY();
        int previousY = event.getFrom().getBlockY();

        if (currentY < previousY) {
            if (currentY == MAX_HEIGHT && !fallStartHeights.containsKey(playerName)) {
                fallStartHeights.put(playerName, currentY);
                fallStartTimes.put(playerName, System.currentTimeMillis());
                usedTotem.put(playerName, false);
                player.sendMessage("§e¡Comienza el Descenso al Inframundo! Tienes 10 segundos.");
            }
        }
        else if (currentY == previousY && fallStartHeights.containsKey(playerName)) {
            if (currentY > MIN_HEIGHT) {
                player.sendMessage("§c¡Descenso interrumpido! Debes caer directamente hasta Y=1.");
                resetPlayerData(playerName);
            }
        }
        else if (currentY <= MIN_HEIGHT && fallStartHeights.containsKey(playerName)) {
            if (usedTotem.getOrDefault(playerName, false)) {
                player.sendMessage("§c¡Descenso fallido! No puedes usar totems para sobrevivir.");
                resetPlayerData(playerName);
                return;
            }

            long fallTime = System.currentTimeMillis() - fallStartTimes.get(playerName);

            if (fallTime <= MAX_FALL_TIME) {
                eventHandler.completeAchievement(playerName, "nether_fall");
                successNotification.showSuccess(player);
                player.sendMessage("§a¡Descenso al Inframundo completado en " + (fallTime/1000.0) + " segundos!");
            } else {
                player.sendMessage("§c¡Demasiado lento! El descenso debe completarse en menos de 10 segundos.");
            }

            resetPlayerData(playerName);
        }

        if (fallStartTimes.containsKey(playerName)) {
            long fallTime = System.currentTimeMillis() - fallStartTimes.get(playerName);
            if (fallTime > MAX_FALL_TIME) {
                player.sendMessage("§c¡Tiempo agotado! El descenso debe completarse en menos de 10 segundos.");
                resetPlayerData(playerName);
            }
        }
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        String playerName = player.getName();

        if (fallStartHeights.containsKey(playerName)) {
            usedTotem.put(playerName, true);
            player.sendMessage("§c¡Totem usado! El descenso no contará para el logro.");
        }
    }

    private void resetPlayerData(String playerName) {
        fallStartHeights.remove(playerName);
        fallStartTimes.remove(playerName);
        usedTotem.remove(playerName);
    }
}