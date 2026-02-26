package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mission13 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final List<Material> flowers = Arrays.asList(
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP,
            Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC,
            Material.ROSE_BUSH, Material.PEONY, Material.TORCHFLOWER,
            Material.PITCHER_PLANT, Material.PINK_PETALS, Material.SPORE_BLOSSOM
    );

    public Mission13(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Stardew Valley"; }

    @Override
    public String getDescription() { return "Consigue todas las flores del juego."; }

    @Override
    public int getMissionNumber() { return 13; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(8);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 15);
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 25);
        ItemStack xpFill = new ItemStack(Material.BONE_MEAL, 2);
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

    public List<Material> getRequiredFlowers() { return flowers; }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkFlower(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Material clickedType = event.getCurrentItem().getType();

        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                checkFlower(player, clickedType), 1L);
    }

    private void checkFlower(Player player, Material type) {
        if (type == null || type == Material.AIR) return;
        if (!flowers.contains(type)) return;
        if (!missionHandler.isMissionActive(player, 13)) return;

        MissionData data = missionHandler.getData(player, 13);
        if (data.isCompleted()) return;

        String key = "collected_" + type.name();
        if (data.getProgressBool(key)) return;

        data.setProgressValue(key, true);

        int count = 0;
        for (Material f : flowers) {
            if (data.getProgressBool("collected_" + f.name())) count++;
        }

        missionHandler.saveData(player, 13, data);

        if (count >= flowers.size()) {
            successNotification.showSuccess(player);
            missionHandler.completeMission(player, 13);
        } else {
            String flowerName = type.name().toLowerCase().replace('_', ' ');
            flowerName = flowerName.substring(0, 1).toUpperCase() + flowerName.substring(1);

            String msg = ChatColor.GOLD + "۞ " +
                    ChatColor.of("#FFCC99") + "Flor: " + ChatColor.GREEN + flowerName + " " +
                    ChatColor.of("#FFA07A") + count +
                    ChatColor.of("#FFE4B5") + "/" +
                    ChatColor.of("#FFA07A") + flowers.size();
            actionBarHandler.sendActionBar(player, msg);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }
}