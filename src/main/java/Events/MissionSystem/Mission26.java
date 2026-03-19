package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Mission26 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final Set<Material> requiredBlocks = new LinkedHashSet<>(Arrays.asList(
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.COPPER_BLOCK,
            Material.NETHERITE_BLOCK
    ));

    public Mission26(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Ve a tocar pasto"; }

    @Override
    public String getDescription() { return "Rompe bloques de Hierro, Oro,\nEsmeralda, Diamante, Cobre y\nNetherite con Fatiga Minera III."; }

    @Override
    public int getMissionNumber() { return 26; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(16);
        ItemStack potion = CustomPotions.getHasteIIIPotion();
        potion.setAmount(1);
        ItemStack diamondBlocks = new ItemStack(Material.DIAMOND_BLOCK, 10);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) {
                rewards.add(potion.clone());
            }
            else if (i == 14) {
                rewards.add(coins);
            }
            else if (i == 16) {
                rewards.add(diamondBlocks);
            }
            else {
                rewards.add(xpFill.clone());
            }
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    public Set<Material> getRequiredBlocks() { return requiredBlocks; }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!requiredBlocks.contains(block.getType())) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 26)) return;

        PotionEffect effect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (effect != null && effect.getAmplifier() >= 2) {

            MissionData data = missionHandler.getData(player, 26);
            if (data.isCompleted()) return;

            String key = "broken_" + block.getType().name();

            if (!data.getProgressBool(key)) {
                data.setProgressValue(key, true);

                boolean all = true;
                int count = 0;
                for (Material m : requiredBlocks) {
                    if (data.getProgressBool("broken_" + m.name())) {
                        count++;
                    } else {
                        all = false;
                    }
                }

                missionHandler.saveData(player, 26, data);

                if (all) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(player, 26);
                } else {
                    String blockName = block.getType().name().toLowerCase().replace("_", " ");
                    blockName = blockName.substring(0, 1).toUpperCase() + blockName.substring(1);

                    String msg = ChatColor.GOLD + "۞ " +
                            ChatColor.of("#FFCC99") + "Roto: " + ChatColor.GREEN + blockName + " " +
                            ChatColor.of("#FFA07A") + count +
                            ChatColor.of("#FFE4B5") + "/" +
                            ChatColor.of("#FFA07A") + requiredBlocks.size();
                    actionBarHandler.sendActionBar(player, msg);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
            }
        }
    }
}