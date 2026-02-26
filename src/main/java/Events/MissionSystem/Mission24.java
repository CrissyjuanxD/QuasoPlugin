package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Mission24 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final Map<UUID, BukkitTask> activeTimers = new HashMap<>();

    public Mission24(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "¡Jugando con Fuego!"; }

    @Override
    public String getDescription() { return "Sobrevive 5 segundos con medio corazón y la mano secundaria vacía."; }

    @Override
    public int getMissionNumber() { return 24; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(5);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 10);
        ItemStack diamonds = new ItemStack(Material.DIAMOND_BLOCK, 5);
        ItemStack xpFill = new ItemStack(Material.GOLDEN_CARROT, 4);
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
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 24)) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();

        if (finalHealth <= 1.0 && finalHealth > 0) {
            if (activeTimers.containsKey(player.getUniqueId())) return;
            if (isOffHandEmpty(player)) {
                startSurvivalTimer(player);
            }
        }
    }

    private void startSurvivalTimer(Player player) {
        if (missionHandler.isMissionCompleted(player, 24)) return;

        player.sendMessage(ChatColor.RED + "⚠ " + ChatColor.of("#FFA07A") + "¡Sobrevive 5 segundos!");

        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = 5;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || player.getHealth() > 1.0 || !isOffHandEmpty(player)) {
                    if (player.isOnline() && !player.isDead()) {
                        String msg = ChatColor.RED + "✖ Desafío cancelado";
                        actionBarHandler.sendActionBar(player, msg);
                    }
                    activeTimers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "Aguanta: " +
                        ChatColor.of("#FFA07A") + secondsLeft + "s";
                actionBarHandler.sendActionBar(player, msg);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);

                secondsLeft--;

                if (secondsLeft < 0) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(player, 24);
                    activeTimers.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTimers.put(player.getUniqueId(), task);
    }

    private boolean isOffHandEmpty(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand == null || offhand.getType() == Material.AIR;
    }
}