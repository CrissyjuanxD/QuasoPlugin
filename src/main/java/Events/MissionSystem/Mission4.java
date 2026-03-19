package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mission4 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final List<Material> allArmors = Arrays.asList(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.COPPER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS
    );

    public Mission4(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() {
        return "El Mejor Guerrero";
    }

    @Override
    public String getDescription() {
        return "Equípate todas las piezas\nde todas las armaduras\ndel juego.";
    }

    @Override
    public int getMissionNumber() {
        return 4;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);

        ItemStack goldenApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2);

        ItemStack protBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) protBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.PROTECTION, 5, true);
            protBook.setItemMeta(meta);
        }

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(protBook);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(goldenApples);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (missionHandler.isMissionActive(player, 4)) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkArmor(player), 1L);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (missionHandler.isMissionActive(event.getPlayer(), 4)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkArmor(event.getPlayer()), 1L);
        }
    }

    private void checkArmor(Player player) {
        MissionData data = missionHandler.getData(player, 4);

        if (data.isCompleted()) return;

        ItemStack[] equipped = {
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        };

        boolean updated = false;

        for (ItemStack item : equipped) {
            if (item != null && allArmors.contains(item.getType())) {
                String key = "armor_" + item.getType().name();
                if (!data.getProgressBool(key)) {
                    data.setProgressValue(key, true);
                    updated = true;
                }
            }
        }

        if (updated) {
            int current = 0;
            for (Material mat : allArmors) {
                if (data.getProgressBool("armor_" + mat.name())) {
                    current++;
                }
            }

            missionHandler.saveData(player, 4, data);

            if (current >= allArmors.size()) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 4);
            } else {
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Prog. Armaduras: " +
                        ChatColor.of("#FFA07A") + current +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + allArmors.size();
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}