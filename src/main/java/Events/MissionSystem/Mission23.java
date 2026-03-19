package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
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
    public String getName() { return "Con su propia medicina"; }

    @Override
    public String getDescription() { return "Mata a 10 Piglin Brutes usando un\nHacha de Oro y llevando al menos\nuna pieza de armadura de oro."; }

    @Override
    public int getMissionNumber() { return 23; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(16);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 20);
        ItemStack diamonds = new ItemStack(Material.NETHERITE_INGOT, 2);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 2);
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
        if (event.getEntityType() != EntityType.PIGLIN_BRUTE) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!missionHandler.isMissionActive(killer, 23)) return;

        MissionData data = missionHandler.getData(killer, 23);
        if (data.isCompleted()) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.GOLDEN_AXE) return;

        if (hasGoldenArmor(killer)) {
            int current = data.getProgressInt("piglin_brutes_killed");

            if (current < 10) {
                current++;
                data.setProgressValue("piglin_brutes_killed", current);
                missionHandler.saveData(killer, 23, data);

                if (current >= 10) {
                    successNotification.showSuccess(killer);
                    String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "Justicia completa!";
                    actionBarHandler.sendActionBar(killer, msg);
                    missionHandler.completeMission(killer, 23);
                } else {
                    String color = (current >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString());
                    String msg = ChatColor.GOLD + "۞ " +
                            ChatColor.of("#FFCC99") + "Piglin Brute Eliminado: " +
                            color + current +
                            ChatColor.of("#FFE4B5") + "/" +
                            ChatColor.of("#FFA07A") + "10";
                    actionBarHandler.sendActionBar(killer, msg);
                }
            }
        }
    }

    private boolean hasGoldenArmor(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) return false;

        ItemStack[] armor = eq.getArmorContents();
        for (ItemStack item : armor) {
            if (item != null && item.getType().name().contains("GOLDEN_")) {
                return true;
            }
        }
        return false;
    }
}