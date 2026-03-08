package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public class Mission12 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;
    private final NamespacedKey friendKey;

    public Mission12(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.friendKey = new NamespacedKey(plugin, "mission_friend_snowman");
    }

    @Override
    public String getName() { return "Los mejores amigos"; }

    @Override
    public String getDescription() { return "Crea un Snow Golem en Warped Forest, quítale la calabaza y espera a que muera."; }

    @Override
    public int getMissionNumber() { return 12; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(10);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 5);
        ItemStack pie = new ItemStack(Material.PUMPKIN_PIE, 64);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldenApples);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(pie);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowman snowman)) return;
        if (snowman.getLocation().getBlock().getBiome() != Biome.WARPED_FOREST) return;
        if (!snowman.isDerp()) {
            if (event.getCause() == EntityDamageEvent.DamageCause.MELTING ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShearSnowman(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Snowman snowman)) return;
        Player player = event.getPlayer();

        if (!missionHandler.isMissionActive(player, 12)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SHEARS) return;
        if (player.getWorld().getBiome(player.getLocation()) != Biome.WARPED_FOREST) return;
        if (snowman.isDerp()) return;

        snowman.getPersistentDataContainer().set(friendKey, PersistentDataType.STRING, player.getName());

        String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Adiós calabaza! Ahora espera su triste final...";
        actionBarHandler.sendActionBar(player, msg);
    }

    @EventHandler
    public void onSnowmanDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Snowman snowman)) return;
        if (!snowman.getPersistentDataContainer().has(friendKey, PersistentDataType.STRING)) return;

        EntityDamageEvent damageEvent = snowman.getLastDamageCause();
        if (damageEvent == null) return;

        if (damageEvent.getCause() == EntityDamageEvent.DamageCause.MELTING ||
                damageEvent.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                damageEvent.getCause() == EntityDamageEvent.DamageCause.FIRE) {

            String playerName = snowman.getPersistentDataContainer().get(friendKey, PersistentDataType.STRING);
            Player player = plugin.getServer().getPlayer(playerName);

            if (player != null && player.isOnline()) {
                if (missionHandler.isMissionActive(player, 12) && !missionHandler.isMissionCompleted(player, 12)) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(player, 12);
                }
            }
        }
    }
}