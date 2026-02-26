package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission12 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission12(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Veneno Explosivo"; }

    @Override
    public String getDescription() { return "Mata 30 Corrupted Bees y 30 Bombitas."; }

    @Override
    public int getMissionNumber() { return 12; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(10);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5);
        ItemStack diamonds = new ItemStack(Material.GUNPOWDER, 32);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 3);
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
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 12)) return;

        boolean isBee = entity instanceof Bee && entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "corrupted_bee"), PersistentDataType.BYTE);
        boolean isBombita = entity instanceof Creeper && entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "bombita"), PersistentDataType.BYTE);

        if (!isBee && !isBombita) return;

        MissionData data = missionHandler.getData(killer, 12);
        if (data.isCompleted()) return;

        boolean updated = false;
        int bees = data.getProgressInt("bees_killed");
        int bombs = data.getProgressInt("bombitas_killed");

        if (isBee && bees < 30) {
            bees++;
            data.setProgressValue("bees_killed", bees);
            updated = true;
        } else if (isBombita && bombs < 30) {
            bombs++;
            data.setProgressValue("bombitas_killed", bombs);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 12, data);

            if (bees >= 30 && bombs >= 30) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 12);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Bees: " + ChatColor.of("#FFA07A") + bees + ChatColor.of("#FFE4B5") + "/" + ChatColor.of("#FFA07A") + "30" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Bombitas: " + ChatColor.of("#FFA07A") + bombs + ChatColor.of("#FFE4B5") + "/" + ChatColor.of("#FFA07A") + "30";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}