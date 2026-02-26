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

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "El Héroe Dorado";
    }

    @Override
    public String getDescription() {
        return "Completa una Raid y fabrica\n20 manzanas de oro.";
    }

    @Override
    public int getMissionNumber() {
        return 3;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(10);

        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        ItemStack potion = CustomPotions.getResistanceIIPotion();
        potion.setAmount(1);
        ItemStack potion2 = CustomPotions.getSplashAbsorptionXPotion();
        potion.setAmount(1);

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
            else if (i == 14 || i == 15 || i == 16) {
                rewards.add(totem.clone());
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
                updateProgress(player, "raid_completed", true);
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

    private void updateProgress(Player player, String type, Object value) {
        MissionData data = missionHandler.getData(player, 3);
        if (data.isCompleted()) return;

        boolean updated = false;

        if (type.equals("raid_completed")) {
            if (!data.getProgressBool("raid_completed")) {
                data.setProgressValue("raid_completed", true);
                updated = true;
            }
        } else if (type.equals("apples_crafted")) {
            int current = data.getProgressInt("apples_crafted");
            int amount = (int) value;
            if (current < 20) {
                int newTotal = Math.min(20, current + amount);
                data.setProgressValue("apples_crafted", newTotal);
                updated = true;
            }
        }

        if (updated) {
            missionHandler.saveData(player, 3, data);

            boolean raidDone = data.getProgressBool("raid_completed");
            int apples = data.getProgressInt("apples_crafted");

            if (raidDone && apples >= 20) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 3);
            } else {
                String raidStatus = raidDone ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖";
                String appleStatus = (apples >= 20 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + String.valueOf(apples) + "/20";

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Raid: " + raidStatus +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Manzanas: " + appleStatus;

                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}