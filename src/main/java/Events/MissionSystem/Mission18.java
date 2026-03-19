package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.DoubleLifeTotem;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission18 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;
    private final DoubleLifeTotem doubleLifeTotem;

    public Mission18(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
    }

    @Override
    public String getName() { return "Jugando a ser Dios"; }

    @Override
    public String getDescription() { return "Activa 10 Tótems de la Inmortalidad."; }

    @Override
    public int getMissionNumber() { return 18; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(15);
        ItemStack proBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) proBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.PROTECTION, 5, true);
            proBook.setItemMeta(meta);
        }
        ItemStack dTotems = doubleLifeTotem.createDoubleLifeTotem();
        dTotems.setAmount(1);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 12) rewards.add(dTotems);
            else if (i == 14) rewards.add(coins);
            else if (i == 16) rewards.add(proBook);
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

        if (!missionHandler.isMissionActive(player, 18)) return;

        MissionData data = missionHandler.getData(player, 18);
        if (data.isCompleted()) return;

        int popped = data.getProgressInt("totems_popped");

        if (popped < 10) {
            popped++;
            data.setProgressValue("totems_popped", popped);
            missionHandler.saveData(player, 18, data);

            if (popped >= 10) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 18);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Tótems activados: " +
                        ChatColor.of("#FFA07A") + popped +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "10";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}