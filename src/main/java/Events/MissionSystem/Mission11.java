package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission11 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission11(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "Elite Superior";
    }

    @Override
    public String getDescription() {
        return "Mata a 30 Elite Spider y\n30 Elite Skeletons.";
    }

    @Override
    public int getMissionNumber() {
        return 11;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(18);
        ItemStack potion = CustomPotions.getSplashRegenerationIIIPotion();
        potion.setAmount(1);
        ItemStack diamondBlocks = new ItemStack(Material.DIAMOND_BLOCK, 8);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) {
                rewards.add(potion.clone());
            }
            else if (i == 14) {
                rewards.add(coins);
            }
            else if (i == 16) {
                rewards.add(diamondBlocks);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEliteDeath(EntityDeathEvent event) {
        org.bukkit.entity.LivingEntity entity = event.getEntity();

        boolean isSpider = entity instanceof org.bukkit.entity.Spider;
        boolean isSkeleton = entity instanceof org.bukkit.entity.AbstractSkeleton;

        if (!isSpider && !isSkeleton) return;

        boolean isElite = false;

        try {
            if (EntityTracker.getEliteMobEntity(entity) != null) {
                isElite = true;
            }
        } catch (Throwable ignored) {
        }

        if (!isElite && entity.getScoreboardTags().stream().anyMatch(tag -> tag.toLowerCase().contains("elitemob"))) {
            isElite = true;
        }

        if (!isElite && entity.hasMetadata("EliteMob")) {
            isElite = true;
        }

        if (!isElite) return;

        Player killer = entity.getKiller();

        if (killer == null && entity.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent damageEvent) {
            if (damageEvent.getDamager() instanceof Player) {
                killer = (Player) damageEvent.getDamager();
            } else if (damageEvent.getDamager() instanceof org.bukkit.entity.Projectile proj) {
                if (proj.getShooter() instanceof Player) {
                    killer = (Player) proj.getShooter();
                }
            }
        }

        if (killer == null) return;

        MissionData data = missionHandler.getData(killer, 11);
        if (!data.isActive() || data.isCompleted()) return;

        int spiders = data.getProgressInt("elite_spiders_killed");
        int skeletons = data.getProgressInt("elite_skeletons_killed");
        boolean updated = false;

        if (isSpider && spiders < 30) {
            spiders++;
            data.setProgressValue("elite_spiders_killed", spiders);
            updated = true;
        } else if (isSkeleton && skeletons < 30) {
            skeletons++;
            data.setProgressValue("elite_skeletons_killed", skeletons);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 11, data);

            if (spiders >= 30 && skeletons >= 30) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 11);
            } else {
                String spiderColor = spiders >= 30 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
                String skeletonColor = skeletons >= 30 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Elite Spiders: " + spiderColor + spiders + ChatColor.of("#FFE4B5") + "/30" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Elite Skeletons: " + skeletonColor + skeletons + ChatColor.of("#FFE4B5") + "/30";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}