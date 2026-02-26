package Events.MissionSystem;

import Dificultades.DayOneChanges;
import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission5 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission5(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "Elite Principiante";
    }

    @Override
    public String getDescription() {
        return "Mata a 15 Elite Zombies.";
    }

    @Override
    public int getMissionNumber() {
        return 5;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(13);

        ItemStack corruptedSteak = DayOneChanges.corruptedSteak();
        corruptedSteak.setAmount(25);

        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 10);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(coins);
            else if (i == 13) rewards.add(corruptedSteak);
            else if (i == 15) rewards.add(goldenApples);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        org.bukkit.entity.LivingEntity entity = event.getEntity();
        if (!(entity instanceof org.bukkit.entity.Zombie)) return;

        boolean isElite = false;

        if (EntityTracker.getEliteMobEntity(entity) != null) {
            isElite = true;
        } else if (entity.getScoreboardTags().stream().anyMatch(tag -> tag.toLowerCase().contains("elitemob"))) {
            isElite = true;
        }
        if (!isElite) return;

        Player killer = entity.getKiller();

        if (killer == null && entity.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            org.bukkit.event.entity.EntityDamageByEntityEvent damageEvent = (org.bukkit.event.entity.EntityDamageByEntityEvent) entity.getLastDamageCause();
            if (damageEvent.getDamager() instanceof Player) {
                killer = (Player) damageEvent.getDamager();
            } else if (damageEvent.getDamager() instanceof org.bukkit.entity.Projectile) {
                org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) damageEvent.getDamager();
                if (proj.getShooter() instanceof Player) {
                    killer = (Player) proj.getShooter();
                }
            }
        }

        if (killer == null) return;

        MissionData data = missionHandler.getData(killer, 5);
        if (!data.isActive() || data.isCompleted()) return;
        int killed = data.getProgressInt("elite_zombies_killed");
        int target = 15;
        if (killed < target) {
            killed++;
            data.setProgressValue("elite_zombies_killed", killed);
            missionHandler.saveData(killer, 5, data);
            if (killed >= target) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 5);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Elite Zombies: " +
                        ChatColor.of("#FFA07A") + killed +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + target;
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}