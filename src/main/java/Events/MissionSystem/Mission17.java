package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
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
    public String getName() { return "Jugando a ser Dios"; }

    @Override
    public String getDescription() { return "Activa 5 Tótems de la Inmortalidad."; }

    @Override
    public int getMissionNumber() { return 17; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(7);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 20);
        ItemStack diamonds = new ItemStack(Material.TOTEM_OF_UNDYING, 2);
        ItemStack xpFill = new ItemStack(Material.GOLD_INGOT, 1);
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
    public void onTotemPop(EntityResurrectEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (!missionHandler.isMissionActive(player, 17)) return;

        MissionData data = missionHandler.getData(player, 17);
        if (data.isCompleted()) return;

        int popped = data.getProgressInt("totems_popped");

        if (popped < 5) {
            popped++;
            data.setProgressValue("totems_popped", popped);
            missionHandler.saveData(player, 17, data);

            if (popped >= 5) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 17);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Tótems activados: " +
                        ChatColor.of("#FFA07A") + popped +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "5";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}