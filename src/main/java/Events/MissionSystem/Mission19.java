package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission19 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission19(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Vida Opaca"; }

    @Override
    public String getDescription() { return "Rompe 35 Creaking Hearts en un Pale Garden."; }

    @Override
    public int getMissionNumber() { return 19; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);
        ItemStack potion = CustomPotions.getSplashAbsorptionXPotion();
        potion.setAmount(1);
        ItemStack gapple = new ItemStack(Material.GOLDEN_APPLE, 20);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) {
                rewards.add(potion.clone());
            }
            else if (i == 14) {
                rewards.add(coins);
            }
            else if (i == 16) {
                rewards.add(gapple);
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
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.CREAKING_HEART) return;
        if (event.getBlock().getBiome() != Biome.PALE_GARDEN) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 19)) return;

        MissionData data = missionHandler.getData(player, 19);
        if (data.isCompleted()) return;

        int broken = data.getProgressInt("hearts_broken");

        if (broken < 35) {
            broken++;
            data.setProgressValue("hearts_broken", broken);
            event.setDropItems(false);
            missionHandler.saveData(player, 19, data);

            if (broken >= 35) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 19);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Creaking Hearts: " +
                        ChatColor.of("#FFA07A") + broken +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "35";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}