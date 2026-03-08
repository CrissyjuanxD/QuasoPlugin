package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission21 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission21(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "¿Confías en mí?"; }

    @Override
    public String getDescription() { return "Sobrevive al vacío activando un tótem."; }

    @Override
    public int getMissionNumber() { return 21; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(10);
        ItemStack potion = CustomPotions.getSplashResistanceIIIPotion();
        potion.setAmount(1);
        ItemStack diamondBlocks = new ItemStack(Material.DIAMOND_BLOCK, 3);
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

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 21)) return;

        MissionData data = missionHandler.getData(player, 21);
        if (data.isCompleted()) return;

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        boolean isVoidDeath = false;

        if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.VOID) {
            isVoidDeath = true;
        } else if (player.getWorld().getEnvironment() == World.Environment.THE_END && player.getLocation().getY() < -50) {
            isVoidDeath = true;
        }

        if (isVoidDeath) {
            successNotification.showSuccess(player);
            String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Has desafiado al vacío!";
            actionBarHandler.sendActionBar(player, msg);
            missionHandler.completeMission(player, 21);
        }
    }
}