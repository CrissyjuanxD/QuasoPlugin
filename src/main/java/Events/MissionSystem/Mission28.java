package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Mission28 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission28(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "Armor 101%";
    }

    @Override
    public String getDescription() {
        return "Equípate una armadura completa\nde Netherite con Protección V.";
    }

    @Override
    public int getMissionNumber() {
        return 28;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(19);

        ItemStack effBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) effBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.EFFICIENCY, 6, true);
            effBook.setItemMeta(meta);
        }

        ItemStack potion = CustomPotions.getSplashResistanceIIIPotion();
        potion.setAmount(1);

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) {
                rewards.add(potion.clone());
            } else if (i == 14) {
                rewards.add(coins);
            } else if (i == 16) {
                rewards.add(effBook);
            } else {
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
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            checkArmorEquipped(player);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT")) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    checkArmorEquipped(player);
                }
            }, 3L);
        }
    }

    private void checkArmorEquipped(Player player) {
        MissionData data = missionHandler.getData(player, 28);
        if (!data.isActive() || data.isCompleted()) return;

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = hasNetheriteWithProtectionV(helmet, Material.NETHERITE_HELMET);
        boolean hasChestplate = hasNetheriteWithProtectionV(chestplate, Material.NETHERITE_CHESTPLATE);
        boolean hasLeggings = hasNetheriteWithProtectionV(leggings, Material.NETHERITE_LEGGINGS);
        boolean hasBoots = hasNetheriteWithProtectionV(boots, Material.NETHERITE_BOOTS);

        boolean updated = false;

        // Registrar cada pieza
        if (hasHelmet && !data.getProgressBool("netherite_prot5_helmet")) {
            data.setProgressValue("netherite_prot5_helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }
        if (hasChestplate && !data.getProgressBool("netherite_prot5_chestplate")) {
            data.setProgressValue("netherite_prot5_chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }
        if (hasLeggings && !data.getProgressBool("netherite_prot5_leggings")) {
            data.setProgressValue("netherite_prot5_leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }
        if (hasBoots && !data.getProgressBool("netherite_prot5_boots")) {
            data.setProgressValue("netherite_prot5_boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(player, 28, data);

            if (data.getProgressBool("netherite_prot5_helmet") &&
                    data.getProgressBool("netherite_prot5_chestplate") &&
                    data.getProgressBool("netherite_prot5_leggings") &&
                    data.getProgressBool("netherite_prot5_boots")) {

                missionHandler.completeMission(player, 28);
            } else {
                int completed = 0;
                if (data.getProgressBool("netherite_prot5_helmet")) completed++;
                if (data.getProgressBool("netherite_prot5_chestplate")) completed++;
                if (data.getProgressBool("netherite_prot5_leggings")) completed++;
                if (data.getProgressBool("netherite_prot5_boots")) completed++;

                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso Armadura 101%: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }

    private boolean hasNetheriteWithProtectionV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) {
            return false;
        }
        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 5;
    }
}