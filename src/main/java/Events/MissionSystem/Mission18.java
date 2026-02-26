package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
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

public class Mission18 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission18(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Vida Opaca"; }

    @Override
    public String getDescription() { return "Rompe 10 Creaking Hearts en un Pale Garden."; }

    @Override
    public int getMissionNumber() { return 18; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(12);
        rewards.add(coins);
        rewards.add(new ItemStack(Material.PALE_OAK_LOG, 32));
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
        if (!missionHandler.isMissionActive(player, 18)) return;

        MissionData data = missionHandler.getData(player, 18);
        if (data.isCompleted()) return;

        int broken = data.getProgressInt("hearts_broken");

        if (broken < 10) {
            broken++;
            data.setProgressValue("hearts_broken", broken);
            missionHandler.saveData(player, 18, data);

            if (broken >= 10) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 18);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Creaking Hearts: " +
                        ChatColor.of("#FFA07A") + broken +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "10";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}