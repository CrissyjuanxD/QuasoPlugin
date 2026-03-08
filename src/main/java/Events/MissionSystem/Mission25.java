package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission25 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission25(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "Elite Infernal";
    }

    @Override
    public String getDescription() {
        return "Mata a 10 Elite Wither Skeletons\ny 10 Elite Piglins.";
    }

    @Override
    public int getMissionNumber() {
        return 25;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(17);

        ItemStack panicApples = EconomyItems.createManzanaPanico();
        panicApples.setAmount(8);

        ItemStack protBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) protBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.PROTECTION, 5, true);
            protBook.setItemMeta(meta);
        }

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                rewards.add(protBook);
            } else if (i == 13) {
                rewards.add(coins);
            } else if (i == 15) {
                rewards.add(panicApples);
            } else {
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

        boolean isWitherSkeleton = entity instanceof org.bukkit.entity.WitherSkeleton;
        boolean isPiglin = entity instanceof org.bukkit.entity.PiglinAbstract;

        if (!isWitherSkeleton && !isPiglin) return;

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

        MissionData data = missionHandler.getData(killer, 25);
        if (!data.isActive() || data.isCompleted()) return;

        int witherSkeletons = data.getProgressInt("elite_wither_skeletons_killed");
        int piglins = data.getProgressInt("elite_piglins_killed");
        boolean updated = false;

        if (isWitherSkeleton && witherSkeletons < 10) {
            witherSkeletons++;
            data.setProgressValue("elite_wither_skeletons_killed", witherSkeletons);
            updated = true;
        } else if (isPiglin && piglins < 10) {
            piglins++;
            data.setProgressValue("elite_piglins_killed", piglins);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 25, data);

            if (witherSkeletons >= 10 && piglins >= 10) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 25);
            } else {
                String wsColor = witherSkeletons >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
                String pigColor = piglins >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Wither Skeleton: " + wsColor + witherSkeletons + ChatColor.of("#FFE4B5") + "/10" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Piglins: " + pigColor + piglins + ChatColor.of("#FFE4B5") + "/10";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}