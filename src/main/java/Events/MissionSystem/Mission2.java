package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.spectralmemories.bloodmoon.BloodmoonActuator;

import java.util.ArrayList;
import java.util.List;

public class Mission2 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission2(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "No le tengo miedo a nada";
    }

    @Override
    public String getDescription() {
        return "Mata 125 mobs hostiles\nmientras haya una\nBloodMoon activa.";
    }

    @Override
    public int getMissionNumber() {
        return 2;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(17);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 10);
        ItemStack sharpBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) sharpBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.SHARPNESS, 6, true);
            sharpBook.setItemMeta(meta);
        }

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(sharpBook);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(goldenApples);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) { }

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;

        Player player = event.getEntity().getKiller();
        if (player == null) return;

        MissionData data = missionHandler.getData(player, 2);
        if (!data.isActive() || data.isCompleted()) return;

        BloodmoonActuator actuator = BloodmoonActuator.GetActuator(player.getWorld());
        if (actuator == null || !actuator.isInProgress()) return;

        int current = data.getProgressInt("bloodmoon_kills");
        int target = 125;

        if (current < target) {
            current++;
            data.setProgressValue("bloodmoon_kills", current);

            if (current >= target) {
                successNotification.showSuccess(player);
                missionHandler.saveData(player, 2, data);
                missionHandler.completeMission(player, 2);
            } else {
                missionHandler.saveData(player, 2, data);

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Kills (BloodMoon): " +
                        ChatColor.of("#FFA07A") + current +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + target;
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}