package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class Mission9 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission9(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "¡Qué frialdad!"; }

    @Override
    public String getDescription() { return "Observa a un Iceologer con catalejo y elimínalo."; }

    @Override
    public int getMissionNumber() { return 9; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(12);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 10);
        ItemStack diamonds = new ItemStack(Material.NETHERITE_SCRAP, 8);
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
    public void onSpyglassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.SPYGLASS) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 9)) return;

        MissionData data = missionHandler.getData(player, 9);
        if (data.isCompleted() || data.getProgressBool("spotted")) return;

        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50,
                e -> e instanceof Evoker && e.getPersistentDataContainer().has(new NamespacedKey(plugin, "iceologer"), PersistentDataType.BYTE));

        if (result != null && result.getHitEntity() != null) {
            data.setProgressValue("spotted", true);
            missionHandler.saveData(player, 9, data);

            String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Iceologer avistado! Ahora elimínalo.";
            actionBarHandler.sendActionBar(player, msg);
            successNotification.showSuccess(player);
        }
    }

    @EventHandler
    public void onIceologerDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Evoker) || !entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "iceologer"), PersistentDataType.BYTE)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 9)) return;

        MissionData data = missionHandler.getData(killer, 9);
        if (data.isCompleted()) return;

        if (data.getProgressBool("spotted")) {
            successNotification.showSuccess(killer);
            missionHandler.completeMission(killer, 9);
        }
    }
}