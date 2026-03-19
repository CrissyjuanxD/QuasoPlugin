package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission8 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final NamespacedKey snowballMarkerKey;

    public Mission8(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.snowballMarkerKey = new NamespacedKey(plugin, "mission8_snowball_marker");
    }

    @Override
    public String getName() { return "Nervios de Acero"; }

    @Override
    public String getDescription() { return "Golpea a un Warden con una bola\nde nieve y luego elimínalo.\nRepite el proceso 5 veces."; }

    @Override
    public int getMissionNumber() { return 8; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(18);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5);
        ItemStack diamonds = new ItemStack(Material.EMERALD_BLOCK, 30);
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
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (event.getHitEntity() == null || event.getHitEntity().getType() != EntityType.WARDEN) return;

        if (snowball.getShooter() instanceof Player player) {
            if (!missionHandler.isMissionActive(player, 8)) return;
            if (missionHandler.isMissionCompleted(player, 8)) return;

            event.getHitEntity().getPersistentDataContainer().set(snowballMarkerKey, PersistentDataType.STRING, player.getUniqueId().toString());

            actionBarHandler.sendActionBar(player, ChatColor.AQUA + "¡Warden marcado! Ahora elimínalo.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
        }
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 8)) return;
        if (missionHandler.isMissionCompleted(killer, 8)) return;

        String markedPlayerUUID = event.getEntity().getPersistentDataContainer().get(snowballMarkerKey, PersistentDataType.STRING);

        if (markedPlayerUUID != null && markedPlayerUUID.equals(killer.getUniqueId().toString())) {
            updateProgress(killer);
        }
    }

    private void updateProgress(Player player) {
        MissionData data = missionHandler.getData(player, 8);
        if (data.isCompleted()) return;

        int current = data.getProgressInt("wardens_snowballed_killed");

        if (current < 5) {
            current++;
            data.setProgressValue("wardens_snowballed_killed", current);
            missionHandler.saveData(player, 8, data);

            if (current >= 5) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 8);
            } else {
                String color = (current >= 5 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString());
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Wardens Eliminados: " +
                        color + current +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "5";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}