package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission27 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission27(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Día del Omega"; }

    @Override
    public String getDescription() { return "Mata al prieto de Crosszy."; }

    @Override
    public int getMissionNumber() { return 27; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(16);
        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5);

        ItemStack specificHead = new ItemStack(Material.PLAYER_HEAD, 1);
        if (specificHead.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer("Crosszy"));
            meta.setDisplayName("§cCabeza de Crosszy");
            specificHead.setItemMeta(meta);
        }

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 5);

        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldenApples);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(specificHead);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (victim.getName().equalsIgnoreCase("Crosszy")) {
            if (killer != null && !killer.equals(victim)) {

                if (!missionHandler.isMissionActive(killer, 27)) return;

                MissionData data = missionHandler.getData(killer, 27);

                if (!data.isCompleted()) {
                    successNotification.showSuccess(killer);
                    String msg = ChatColor.GOLD + "۞ " + ChatColor.RED + "¡Omega DERROTADO!";
                    actionBarHandler.sendActionBar(killer, msg);

                    missionHandler.completeMission(killer, 27);
                }
            }
        }
    }
}