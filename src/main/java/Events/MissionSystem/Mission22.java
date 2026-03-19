package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
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

public class Mission22 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission22(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Cazador de Guardianes Acuáticos"; }

    @Override
    public String getDescription() { return "Elimina a 5 Elder Guardians."; }

    @Override
    public int getMissionNumber() { return 22; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(19);
        ItemStack goldblock = new ItemStack(Material.GOLD_BLOCK, 15);
        ItemStack spongei = new ItemStack(Material.SPONGE, 32);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 2);
        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldblock);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(spongei);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ELDER_GUARDIAN) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!missionHandler.isMissionActive(killer, 22)) return;

        MissionData data = missionHandler.getData(killer, 22);
        if (data.isCompleted()) return;

        int killed = data.getProgressInt("guardians_killed");

        if (killed < 5) {
            killed++;
            data.setProgressValue("guardians_killed", killed);
            missionHandler.saveData(killer, 22, data);

            if (killed >= 5) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 22);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Elder Guardians: " +
                        ChatColor.of("#FFA07A") + killed +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "5";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}