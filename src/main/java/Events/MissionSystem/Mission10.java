package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission10 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission10(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Cazador de Abejas"; }

    @Override
    public String getDescription() { return "Elimina a una Abeja Reina.\nUsa /bosstp para ir a su Dungeon.\nInteractua con el panal del altar."; }

    @Override
    public int getMissionNumber() { return 10; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);
        ItemStack goldenApples = new ItemStack(Material.GOLD_BLOCK, 15);
        ItemStack unBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) unBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.UNBREAKING, 4, true);
            unBook.setItemMeta(meta);
        }
        ItemStack xpFill = new ItemStack(Material.HONEY_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) rewards.add(unBook);
            else if (i == 14) rewards.add(coins);
            else if (i == 16) rewards.add(goldenApples);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Bee && entity.getCustomName() != null && entity.getCustomName().contains("Abeja Reina")) {
            Player killer = event.getEntity().getKiller();
            if (killer == null) return;

            if (missionHandler.isMissionActive(killer, 10) && !missionHandler.isMissionCompleted(killer, 10)) {
                successNotification.showSuccess(killer);
                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡La Reina ha caído!";
                actionBarHandler.sendActionBar(killer, msg);
                missionHandler.completeMission(killer, 10);
            }
        }
    }
}