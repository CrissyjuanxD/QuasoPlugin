package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission26 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission26(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Cazador Experimentado"; }

    @Override
    public String getDescription() { return "Mata 15 Spectral Ghasts y 15 Spectral Creepers."; }

    @Override
    public int getMissionNumber() { return 26; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(12);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 32);
        ItemStack diamonds = new ItemStack(Material.SHULKER_SHELL, 10);
        ItemStack xpFill = new ItemStack(Material.GOLD_INGOT, 2);
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

        if (!missionHandler.isMissionActive(killer, 26)) return;

        boolean isSpectralCreeper = entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "ender_creeper"), PersistentDataType.BYTE);

        boolean isSpectralGhast = entity instanceof Ghast &&
                entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "ender_ghast"), PersistentDataType.BYTE);

        if (!isSpectralCreeper && !isSpectralGhast) return;

        MissionData data = missionHandler.getData(killer, 26);
        if (data.isCompleted()) return;

        boolean updated = false;
        int creepers = data.getProgressInt("creepers_killed");
        int ghasts = data.getProgressInt("ghasts_killed");

        if (isSpectralCreeper && creepers < 15) {
            creepers++;
            data.setProgressValue("creepers_killed", creepers);
            updated = true;
        } else if (isSpectralGhast && ghasts < 15) {
            ghasts++;
            data.setProgressValue("ghasts_killed", ghasts);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 26, data);

            if (creepers >= 15 && ghasts >= 15) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 26);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "S. Creepers: " + ChatColor.of("#FFA07A") + creepers + "/15" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "S. Ghasts: " + ChatColor.of("#FFA07A") + ghasts + "/15";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}