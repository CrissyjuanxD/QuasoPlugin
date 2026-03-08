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
    public String getDescription() { return "Sube 300 bloques de altura en menos de 7 segundos."; }

    @Override
    public int getMissionNumber() { return 15; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(12);
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

        if (player.isGliding() || player.isRiptiding() || player.getLocation().getY() > 350) {
            processFlight(player, event.getFrom().getY(), event.getTo().getY());
        } else {
            if (event.getTo().getY() < event.getFrom().getY()) {
                startY.remove(player.getUniqueId());
                startTime.remove(player.getUniqueId());
            }
        }
    }

    private void processFlight(Player player, double fromY, double toY) {
        UUID id = player.getUniqueId();

        if (toY < fromY) {
            startY.remove(id);
            startTime.remove(id);
            return;
        }

        if (!startY.containsKey(id)) {
            startY.put(id, fromY);
            startTime.put(id, System.currentTimeMillis());
            return;
        }

        long timeElapsed = System.currentTimeMillis() - startTime.get(id);

        if (timeElapsed > 7000) {
            startY.put(id, fromY);
            startTime.put(id, System.currentTimeMillis());
            return;
        }

        double heightGained = toY - startY.get(id);

        if (heightGained >= 300) {
            successNotification.showSuccess(player);
            String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Velocidad supersónica alcanzada!";
            actionBarHandler.sendActionBar(player, msg);

            missionHandler.completeMission(player, 15);

            startY.remove(id);
            startTime.remove(id);
        }
    }
}