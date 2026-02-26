package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import items.excavatorItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mission1 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;
    private final excavatorItem ExcavatorItem;

    public Mission1(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.ExcavatorItem = new excavatorItem(plugin);
    }

    @Override
    public String getName() {
        return "¡Si hay que ser minero!";
    }

    @Override
    public String getDescription() {
        return "Recolecta con Toque de Seda\n10 de cada uno de los 19\nminerales del juego.";
    }

    @Override
    public int getMissionNumber() {
        return 1;
    }

    // Lista de los 19 minerales del juego
    public List<Material> getRequiredOres() {
        return Arrays.asList(
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
                Material.ANCIENT_DEBRIS
        );
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(16);
        ItemStack excavator = new ItemStack(ExcavatorItem.createExcavator());
        ItemStack en_goldenapple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3);

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                rewards.add(excavator);
            } else if (i == 13) {
                rewards.add(coins);
            } else if (i == 15) {
                rewards.add(en_goldenapple);
            } else {
                rewards.add(xpFill.clone());
            }
        }

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
    }

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        MissionData data = missionHandler.getData(player, 1);

        if (!data.isActive() || data.isCompleted()) return;

        // Verificar si está usando Toque de Seda
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !tool.hasItemMeta() || !tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            return;
        }

        Material type = event.getBlock().getType();

        // Verificar si el bloque es uno de los 19 ores
        if (!getRequiredOres().contains(type)) return;

        int current = data.getProgressInt("ore_" + type.name());
        int target = 10;

        if (current < target) {
            current++;
            data.setProgressValue("ore_" + type.name(), current);

            // Comprobar si TODOS los ores ya llegaron a 10
            boolean allCompleted = true;
            for (Material ore : getRequiredOres()) {
                if (data.getProgressInt("ore_" + ore.name()) < target) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) {
                successNotification.showSuccess(player);
                missionHandler.saveData(player, 1, data);
                missionHandler.completeMission(player, 1);
            } else {
                missionHandler.saveData(player, 1, data);

                // Mensaje en Action Bar dinámico y limpio
                String oreName = type.name().toLowerCase().replace("deepslate_", "d. ").replace("_ore", "").replace("_", " ");
                oreName = oreName.substring(0, 1).toUpperCase() + oreName.substring(1);

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + oreName + ": " +
                        ChatColor.of("#FFA07A") + current +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + target;
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}