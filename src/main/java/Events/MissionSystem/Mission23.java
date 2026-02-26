package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission23 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission23(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Golpe Final Brillante"; }

    @Override
    public String getDescription() { return "Da el golpe final al Ender Dragon usando una Flecha Espectral."; }

    @Override
    public int getMissionNumber() { return 23; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(15);
        ItemStack goldenApples = new ItemStack(Material.DRAGON_EGG, 1);
        ItemStack diamonds = new ItemStack(Material.DRAGON_BREATH, 16);
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
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;

        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            if (damageEvent.getDamager() instanceof SpectralArrow arrow) {
                if (arrow.getShooter() instanceof Player killer) {

                    if (!missionHandler.isMissionActive(killer, 23)) return;
                    if (missionHandler.isMissionCompleted(killer, 23)) return;

                    successNotification.showSuccess(killer);
                    String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Brillante!";
                    actionBarHandler.sendActionBar(killer, msg);
                    missionHandler.completeMission(killer, 23);
                }
            }
        }
    }
}