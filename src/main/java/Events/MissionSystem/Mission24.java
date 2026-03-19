package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
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
    public String getDescription() { return "Sobrevive 10 minutos con medio\ncorazón y la mano secundaria vacía."; }

    @Override
    public int getMissionNumber() { return 24; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(17);
        ItemStack potion = CustomPotions.getSplashRegenerationIIIPotion();
        potion.setAmount(1);
        ItemStack diamondBlocks = new ItemStack(Material.DIAMOND_BLOCK, 15);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) {
                rewards.add(potion.clone());
            }
            else if (i == 14) {
                rewards.add(coins);
            }
            else if (i == 16) {
                rewards.add(diamondBlocks);
            }
            else {
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

        player.sendMessage(ChatColor.RED + "⚠ " + ChatColor.of("#FFA07A") + "¡Sobrevive 10 minutos!");

        BukkitTask task = new BukkitRunnable() {
            int secondsElapsed = 0;
            final int targetSeconds = 600;

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

                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                String timeFormatted = String.format("%02d:%02d", minutes, seconds);

                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "Tiempo sobrevivido: " +
                        ChatColor.of("#FFA07A") + timeFormatted + ChatColor.of("#FFE4B5") + " / 10:00";
                actionBarHandler.sendActionBar(player, msg);

                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 2f);

                // Condición de victoria
                if (secondsElapsed >= targetSeconds) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(player, 24);
                    activeTimers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                secondsElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTimers.put(player.getUniqueId(), task);
    }

    private boolean isOffHandEmpty(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand == null || offhand.getType() == Material.AIR;
    }
}