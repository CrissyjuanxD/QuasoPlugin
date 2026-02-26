package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
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
    public String getName() { return "Cazador de Corruptos 2"; }

    @Override
    public String getDescription() { return "Elimina a 20 Guardian Corrupted Skeletons."; }

    @Override
    public int getMissionNumber() { return 16; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(7);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 5);
        ItemStack diamonds = new ItemStack(Material.NETHERITE_SCRAP, 10);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 2);
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
    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 16)) return;

        NamespacedKey key = new NamespacedKey(plugin, "guardian_corrupted_skeleton");

        if (entity instanceof WitherSkeleton && entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {

            MissionData data = missionHandler.getData(killer, 16);
            if (data.isCompleted()) return;

            int killed = data.getProgressInt("skeletons_killed");

            if (killed < 20) {
                killed++;
                data.setProgressValue("skeletons_killed", killed);
                missionHandler.saveData(killer, 16, data);

                if (killed >= 20) {
                    successNotification.showSuccess(killer);
                    missionHandler.completeMission(killer, 16);
                } else {
                    String msg = ChatColor.GOLD + "۞ " +
                            ChatColor.of("#FFCC99") + "Guardian Skeleton: " +
                            ChatColor.of("#FFA07A") + killed +
                            ChatColor.of("#FFE4B5") + "/" +
                            ChatColor.of("#FFA07A") + "20";
                    actionBarHandler.sendActionBar(killer, msg);
                }
            }
        }
    }
}