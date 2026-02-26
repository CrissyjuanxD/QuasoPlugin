package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission8 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission8(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Nervios de Acero"; }

    @Override
    public String getDescription() { return "Golpea a un Warden con un proyectil y elimínalo."; }

    @Override
    public int getMissionNumber() { return 8; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5);
        ItemStack diamonds = new ItemStack(Material.EMERALD_BLOCK, 20);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
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
    public void onHitWarden(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            if (missionHandler.isMissionActive(player, 8)) {
                updateProgress(player, "hit_projectile");
            }
        }
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            if (missionHandler.isMissionActive(player, 8)) {
                updateProgress(player, "killed_warden");
            }
        }
    }

    private void updateProgress(Player player, String objectiveKey) {
        MissionData data = missionHandler.getData(player, 8);
        if (data.isCompleted()) return;

        // Si ya tiene este objetivo, no hacemos nada (evitar spam DB)
        if (data.getProgressBool(objectiveKey)) return;

        data.setProgressValue(objectiveKey, true);
        missionHandler.saveData(player, 8, data);

        boolean hit = data.getProgressBool("hit_projectile");
        boolean killed = data.getProgressBool("killed_warden");

        if (hit && killed) {
            successNotification.showSuccess(player);
            missionHandler.completeMission(player, 8);
        } else {
            String hitStatus = hit ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖";
            String killStatus = killed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖";
            String msg = ChatColor.GOLD + "۞ " +
                    ChatColor.of("#FFCC99") + "Warden: " +
                    ChatColor.GRAY + "Hit " + hitStatus +
                    ChatColor.of("#FFE4B5") + " | " +
                    ChatColor.GRAY + "Kill " + killStatus;
            actionBarHandler.sendActionBar(player, msg);
        }
    }
}