package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Mission15 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final Map<UUID, Double> startY = new HashMap<>();
    private final Map<UUID, Long> startTime = new HashMap<>();

    public Mission15(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "¡Sé que puedo volar!"; }

    @Override
    public String getDescription() { return "Sube 400 bloques de altura en\nmenos de 7s sin usar Elytras."; }

    @Override
    public int getMissionNumber() { return 15; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(13);
        ItemStack goldenApples = new ItemStack(Material.GOLD_INGOT, 25);
        ItemStack diamonds = new ItemStack(Material.FIREWORK_ROCKET, 64);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 3);
        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldenApples);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(diamonds);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 15)) return;
        if (missionHandler.isMissionCompleted(player, 15)) return;

        // --- ANTI-ELYTRAS ---
        // Si el jugador intenta usar elytras, el contador se reinicia.
        if (player.isGliding()) {
            startY.remove(player.getUniqueId());
            startTime.remove(player.getUniqueId());
            return;
        }

        // Si está subiendo, calculamos el vuelo
        if (event.getTo().getY() > event.getFrom().getY()) {
            processFlight(player, event.getFrom().getY(), event.getTo().getY());
        } else {
            // Si empieza a caer, reiniciamos el reto
            if (event.getTo().getY() < event.getFrom().getY()) {
                startY.remove(player.getUniqueId());
                startTime.remove(player.getUniqueId());
            }
        }
    }

    private void processFlight(Player player, double fromY, double toY) {
        UUID id = player.getUniqueId();

        // Registrar el punto y tiempo de inicio
        if (!startY.containsKey(id)) {
            startY.put(id, fromY);
            startTime.put(id, System.currentTimeMillis());
            return;
        }

        long timeElapsed = System.currentTimeMillis() - startTime.get(id);

        // Si pasan los 7 segundos, se resetea la marca al bloque actual
        if (timeElapsed > 7000) {
            startY.put(id, fromY);
            startTime.put(id, System.currentTimeMillis());
            return;
        }

        double heightGained = toY - startY.get(id);

        // Meta: 400 Bloques
        if (heightGained >= 400) {
            successNotification.showSuccess(player);
            String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Velocidad supersónica alcanzada!";
            actionBarHandler.sendActionBar(player, msg);

            missionHandler.completeMission(player, 15);

            startY.remove(id);
            startTime.remove(id);
        } else if (heightGained >= 25) {
            // Mostrar progreso a partir de los 25 bloques
            double timeLeft = (7000 - timeElapsed) / 1000.0;

            String colorAltura = heightGained >= 200 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
            String colorTiempo = timeLeft > 2.0 ? ChatColor.GREEN.toString() : ChatColor.RED.toString();

            String msg = ChatColor.GOLD + "۞ " +
                    ChatColor.of("#FFCC99") + "Ascenso: " + colorAltura + (int)heightGained + ChatColor.of("#FFE4B5") + "/400m" +
                    ChatColor.GRAY + " | " +
                    ChatColor.of("#FFCC99") + "Tiempo: " + colorTiempo + String.format(Locale.US, "%.1fs", timeLeft);

            actionBarHandler.sendActionBar(player, msg);
        }
    }
}