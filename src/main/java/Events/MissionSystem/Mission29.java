package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Mission29 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final long REQUIRED_TICKS = 21600000L;

    private final Map<UUID, Long> lastNotifiedHour = new HashMap<>();

    public Mission29(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (missionHandler.isMissionActive(player, 29)) {
                    MissionData data = missionHandler.getData(player, 29);

                    if (!data.isCompleted()) {
                        long currentTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                        long currentHours = currentTicks / 72000L;

                        // Notificación de horas subidas
                        UUID id = player.getUniqueId();
                        if (lastNotifiedHour.containsKey(id)) {
                            long lastHour = lastNotifiedHour.get(id);
                            if (currentHours > lastHour) {
                                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "Has jugado " +
                                        ChatColor.GREEN + currentHours + ChatColor.of("#FFE4B5") + "/300 Horas!";
                                actionBarHandler.sendActionBar(player, msg);

                                lastNotifiedHour.put(id, currentHours);
                            }
                        } else {
                            lastNotifiedHour.put(id, currentHours);
                        }

                        if (currentTicks >= REQUIRED_TICKS) {
                            successNotification.showSuccess(player);
                            missionHandler.completeMission(player, 29);
                        }
                    }
                }
            }
        }, 1200L, 1200L);
    }

    @Override
    public String getName() {
        return "Sin Vida Social";
    }

    @Override
    public String getDescription() {
        return "Juega activamente en el servidor\npor más de 300 horas.";
    }

    @Override
    public int getMissionNumber() {
        return 29;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(30);

        ItemStack beacons = new ItemStack(Material.BEACON, 3);
        ItemStack diamondBlocks = new ItemStack(Material.DIAMOND_BLOCK, 30);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                rewards.add(beacons);
            } else if (i == 13) {
                rewards.add(coins);
            } else if (i == 15) {
                rewards.add(diamondBlocks);
            } else {
                rewards.add(xpFill.clone());
            }
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastNotifiedHour.remove(event.getPlayer().getUniqueId());
    }
}