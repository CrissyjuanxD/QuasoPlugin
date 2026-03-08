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

public class Mission17 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission17(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "Elite Profesional";
    }

    @Override
    public String getDescription() {
        return "Mata a 20 Elite Endermans y\n20 Elite Creepers.";
    }

    @Override
    public int getMissionNumber() {
        return 17;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(19);

        ItemStack sharpBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) sharpBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.SHARPNESS, 7, true);
            sharpBook.setItemMeta(meta);
        }

        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 15);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                rewards.add(sharpBook);
            } else if (i == 13) {
                rewards.add(coins);
            } else if (i == 15) {
                rewards.add(goldenApples);
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

        boolean isEnderman = entity instanceof org.bukkit.entity.Enderman;
        boolean isCreeper = entity instanceof org.bukkit.entity.Creeper;

        if (!isEnderman && !isCreeper) return;

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

        MissionData data = missionHandler.getData(killer, 17);
        if (!data.isActive() || data.isCompleted()) return;

        int endermen = data.getProgressInt("elite_endermen_killed");
        int creepers = data.getProgressInt("elite_creepers_killed");
        boolean updated = false;

        if (isEnderman && endermen < 20) {
            endermen++;
            data.setProgressValue("elite_endermen_killed", endermen);
            updated = true;
        } else if (isCreeper && creepers < 20) {
            creepers++;
            data.setProgressValue("elite_creepers_killed", creepers);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(killer, 17, data);

            if (endermen >= 20 && creepers >= 20) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(killer, 17);
            } else {
                String enderColor = endermen >= 20 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
                String creeperColor = creepers >= 20 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Elite Endermans: " + enderColor + endermen + ChatColor.of("#FFE4B5") + "/20" +
                        ChatColor.GRAY + " | " +
                        ChatColor.of("#FFCC99") + "Elite Creepers: " + creeperColor + creepers + ChatColor.of("#FFE4B5") + "/20";
                actionBarHandler.sendActionBar(killer, msg);
            }
        }
    }
}