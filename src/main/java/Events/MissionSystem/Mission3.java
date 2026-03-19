package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyFlyTotem;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission3 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;
    private final EconomyFlyTotem economyFlyTotem;

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "El Héroe Dorado";
    }

    @Override
    public String getDescription() {
        return "Completa 5 Raids y fabrica\n64 manzanas de oro.";
    }

    @Override
    public int getMissionNumber() {
        return 3;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(15);

        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        ItemStack potion = CustomPotions.getResistanceIIPotion();
        potion.setAmount(1);
        ItemStack potion2 = CustomPotions.getSplashAbsorptionXPotion();
        potion.setAmount(1);
        ItemStack flyTotem = economyFlyTotem.createFlyTotem();

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 12) {
                rewards.add(potion.clone());
            }
            else if (i == 11) {
                rewards.add(potion2.clone());
            }
            else if (i == 13) {
                rewards.add(coins);
            }
            else if (i == 14 || i == 16) {
                rewards.add(totem.clone());
            }
            else if (i == 15) {
                rewards.add(flyTotem.clone());
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
    public void onRaidFinish(RaidFinishEvent event) {
        if (event.getRaid().getStatus() != org.bukkit.Raid.RaidStatus.VICTORY) return;

        for (Player player : event.getWinners()) {
            if (missionHandler.isMissionActive(player, 3)) {
                updateProgress(player, "raids_completed", 1);
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 3)) return;

        ItemStack result = event.getRecipe().getResult();
        if (result.getType() != Material.GOLDEN_APPLE) return;

        int amount = result.getAmount();
        if (event.isShiftClick()) {
            int max = Integer.MAX_VALUE;
            for (ItemStack i : event.getInventory().getMatrix()) {
                if (i != null && i.getType() != Material.AIR) max = Math.min(max, i.getAmount());
            }
            if (max == Integer.MAX_VALUE) max = 0;
            amount = max * result.getAmount();
        }

        if (amount > 0) {
            updateProgress(player, "apples_crafted", amount);
        }
    }

    private void updateProgress(Player player, String type, int value) {
        MissionData data = missionHandler.getData(player, 3);
        if (data.isCompleted()) return;

        boolean updated = false;

        if (type.equals("raids_completed")) {
            int current = data.getProgressInt("raids_completed");
            if (current < 5) {
                data.setProgressValue("raids_completed", Math.min(5, current + value));
                updated = true;
            }
        } else if (type.equals("apples_crafted")) {
            int current = data.getProgressInt("apples_crafted");
            if (current < 64) {
                int newTotal = Math.min(64, current + value);
                data.setProgressValue("apples_crafted", newTotal);
                updated = true;
            }
        }

        if (updated) {
            missionHandler.saveData(player, 3, data);

            int raidsDone = data.getProgressInt("raids_completed");
            int apples = data.getProgressInt("apples_crafted");

            if (raidsDone >= 5 && apples >= 64) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 3);
            } else {
                String raidStatus = (raidsDone >= 5 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + String.valueOf(raidsDone) + "/5";
                String appleStatus = (apples >= 64 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + String.valueOf(apples) + "/64";

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Raids: " + raidStatus +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Manzanas: " + appleStatus;

                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}