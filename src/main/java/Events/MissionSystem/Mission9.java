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

    private final NamespacedKey spyglassMarkerKey;

    public Mission9(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.spyglassMarkerKey = new NamespacedKey(plugin, "mission9_spyglass_marker");
    }

    @Override
    public String getName() { return "¡Qué frialdad!"; }

    @Override
    public String getDescription() { return "Observa a un Iceologer con\nun catalejo y elimínalo.\nRepite el proceso 10 veces."; }

    @Override
    public int getMissionNumber() { return 9; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(17);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 15);
        ItemStack diamonds = new ItemStack(Material.NETHERITE_SCRAP, 10);
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

    // Evento cuando usa el catalejo
    @EventHandler
    public void onSpyglassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.SPYGLASS) return;

        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 9)) return;
        if (missionHandler.isMissionCompleted(player, 9)) return;

        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 50,
                e -> e instanceof Evoker && e.getPersistentDataContainer().has(new NamespacedKey(plugin, "iceologer"), PersistentDataType.BYTE));

        if (result != null && result.getHitEntity() != null) {
            Entity hitEntity = result.getHitEntity();

            String currentMarker = hitEntity.getPersistentDataContainer().get(spyglassMarkerKey, PersistentDataType.STRING);
            if (currentMarker != null && currentMarker.equals(player.getUniqueId().toString())) {
                return;
            }

            // Inyectamos la marca en el Iceologer
            hitEntity.getPersistentDataContainer().set(spyglassMarkerKey, PersistentDataType.STRING, player.getUniqueId().toString());

            String msg = ChatColor.GOLD + "۞ " + ChatColor.AQUA + "¡Iceologer avistado y marcado! Ahora elimínalo.";
            actionBarHandler.sendActionBar(player, msg);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
        }
    }

    // Evento cuando el Iceologer muere
    @EventHandler
    public void onIceologerDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Evoker) || !entity.getPersistentDataContainer().has(new NamespacedKey(plugin, "iceologer"), PersistentDataType.BYTE)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (!missionHandler.isMissionActive(killer, 9)) return;
        if (missionHandler.isMissionCompleted(killer, 9)) return;

        // Comprobamos si el que lo mató es el mismo que lo marcó
        String markedPlayerUUID = entity.getPersistentDataContainer().get(spyglassMarkerKey, PersistentDataType.STRING);

        if (markedPlayerUUID != null && markedPlayerUUID.equals(killer.getUniqueId().toString())) {
            updateProgress(killer);
        }
    }

    private void updateProgress(Player player) {
        MissionData data = missionHandler.getData(player, 9);
        if (data.isCompleted()) return;

        int current = data.getProgressInt("iceologers_spotted_killed");

        if (current < 10) {
            current++;
            data.setProgressValue("iceologers_spotted_killed", current);
            missionHandler.saveData(player, 9, data);

            if (current >= 10) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 9);
            } else {
                String color = (current >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString());
                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Iceologers Eliminados: " +
                        color + current +
                        ChatColor.of("#FFE4B5") + "/" +
                        ChatColor.of("#FFA07A") + "10";
                actionBarHandler.sendActionBar(player, msg);
            }
        }
    }
}