package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Mission7 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final Map<UUID, Double> lastGroundY = new HashMap<>();

    public Mission7(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Salto de fe ardiente"; }

    @Override
    public String getDescription() { return "Cae 115 bloques de altura en el Nether y sobrevive el impacto."; }

    @Override
    public int getMissionNumber() { return 7; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(10);

        ItemStack potion = CustomPotions.getSlowFallingPotion();
        potion.setAmount(1);

        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 12 || i == 14) rewards.add(potion.clone());
            else if (i == 16) rewards.add(coins);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Optimización rápida
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;

        // Verificación en memoria (rápida)
        if (!missionHandler.isMissionActive(player, 7)) return;
        if (missionHandler.isMissionCompleted(player, 7)) return;

        UUID id = player.getUniqueId();
        double currentY = event.getTo().getY();

        if (player.isOnGround() || player.getLocation().getBlock().getType() != Material.AIR) {
            lastGroundY.put(id, currentY);
            return;
        }

        if (lastGroundY.containsKey(id)) {
            double startY = lastGroundY.get(id);

            // Si sube, reseteamos
            if (currentY > event.getFrom().getY()) {
                lastGroundY.put(id, currentY);
                return;
            }

            double distanceFallen = startY - currentY;

            if (distanceFallen >= 115) {
                successNotification.showSuccess(player);
                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Salto Infernal: " + (int)distanceFallen + "m!";
                actionBarHandler.sendActionBar(player, msg);

                missionHandler.completeMission(player, 7);
                lastGroundY.remove(id);
            }
        }
    }
}