package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission6 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission6(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Cazador de Corruptos"; }

    @Override
    public String getDescription() { return "Mata a 30 Corrupted Zombies\ny 30 Corrupted Spiders.\nAparecen en oleadas en las raids con prob. muy baja."; }

    @Override
    public int getMissionNumber() { return 6; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(18);

        ItemStack potion = CustomPotions.getHasteIIPotion();
        potion.setAmount(1);

        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4);

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            // Ponemos las 3 pociones en los slots 10, 11 y 12
            if (i == 10 || i == 11 || i == 12) rewards.add(potion.clone());
            else if (i == 14) rewards.add(coins);
            else if (i == 16) rewards.add(goldenApples);
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

        MissionData data = missionHandler.getData(killer, 6);
        if (!data.isActive() || data.isCompleted()) return;

        boolean isZ = entity instanceof Zombie && entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "corrupted_zombie"), PersistentDataType.BYTE);
        boolean isS = entity instanceof Spider && entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "corruptedspider"), PersistentDataType.BYTE);

        if (!isZ && !isS) return;

        boolean updated = false;
        int zKilled = data.getProgressInt("zombies_killed");
        int sKilled = data.getProgressInt("spiders_killed");

        if (isZ && zKilled < 30) {
            zKilled++;
            data.setProgressValue("zombies_killed", zKilled);
            updated = true;
        } else if (isS && sKilled < 30) {
            sKilled++;
            data.setProgressValue("spiders_killed", sKilled);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 6, data);

            if (zKilled >= 30 && sKilled >= 30) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 6);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Corr. Zombies: " + ChatColor.of("#FFA07A") + zKilled + ChatColor.of("#FFE4B5") + "/" + ChatColor.of("#FFA07A") + "30" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Corr. Arañas: " + ChatColor.of("#FFA07A") + sKilled + ChatColor.of("#FFE4B5") + "/" + ChatColor.of("#FFA07A") + "30";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}