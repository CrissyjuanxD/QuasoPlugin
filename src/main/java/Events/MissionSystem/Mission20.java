package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission20 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private static final int REQUIRED_AMOUNT = 35;

    public Mission20(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Jugando a ser músico"; }

    @Override
    public String getDescription() { return "Rompe 35 chilladores (Sculk Shriekers) en el bioma Deep Dark."; }

    @Override
    public int getMissionNumber() { return 20; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(16);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5);
        ItemStack artefact = EconomyItems.createYunqueReparadorNivel2();
        artefact.setAmount(1);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldenApples);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(artefact);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SCULK_SHRIEKER) return;
        if (block.getBiome() != Biome.DEEP_DARK) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 20)) return;

        MissionData data = missionHandler.getData(player, 20);
        if (data.isCompleted()) return;

        int broken = data.getProgressInt("shriekers_broken");

        if (broken < REQUIRED_AMOUNT) {
            broken++;
            data.setProgressValue("shriekers_broken", broken);

            event.setDropItems(false);

            missionHandler.saveData(player, 20, data);

            if (broken >= REQUIRED_AMOUNT) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 20);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Chilladores: " +
                        ChatColor.of("#FFA07A") + broken +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + REQUIRED_AMOUNT;
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}