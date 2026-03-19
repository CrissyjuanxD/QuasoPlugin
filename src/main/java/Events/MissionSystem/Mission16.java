package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission16 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission16(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Estado en Descomposición"; }

    @Override
    public String getDescription() { return "Derrota a 5 Withers."; }

    @Override
    public int getMissionNumber() { return 16; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(18);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 7);
        ItemStack potion = CustomPotions.getHasteIIIPotion();
        potion.setAmount(1);

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) rewards.add(potion.clone());
            else if (i == 14) rewards.add(goldenApples);
            else if (i == 16) rewards.add(coins);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WITHER) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 16)) return;

        MissionData data = missionHandler.getData(killer, 16);
        if (data.isCompleted()) return;

        int killed = data.getProgressInt("withers_killed");

        if (killed < 5) {
            killed++;
            data.setProgressValue("withers_killed", killed);
            missionHandler.saveData(killer, 16, data);

            if (killed >= 5) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 16);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Withers: " +
                        ChatColor.of("#FFA07A") + killed +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "5";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}