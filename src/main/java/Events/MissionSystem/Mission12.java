package Events.MissionSystem;

import Dificultades.DayOneChanges;
import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public String getDescription() { return "Defiende a 10 Snow Golems en\nWarped Forest sin calabaza hasta\nque se derritan solos."; }

    @Override
    public int getMissionNumber() { return 12; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 10);
        ItemStack pie = DayOneChanges.improvedPumpkinPie();
        pie.setAmount(64);
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

        // Bloqueamos COMPLETAMENTE el daño de fuego/calor de Vanilla para controlarlo nosotros
        if (event.getCause() == EntityDamageEvent.DamageCause.MELTING ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onShearSnowman(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Snowman snowman)) return;
        Player player = event.getPlayer();

        if (!missionHandler.isMissionActive(player, 12)) return;
        if (missionHandler.isMissionCompleted(player, 12)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SHEARS) return;
        if (snowman.getLocation().getBlock().getBiome() != Biome.WARPED_FOREST) return;

        // isDerp = true significa que YA NO tiene calabaza. Así evitamos doble ejecución.
        if (snowman.isDerp()) return;
        if (snowman.getPersistentDataContainer().has(friendKey, PersistentDataType.STRING)) return;

        // Guardamos el UUID del jugador para mayor seguridad
        snowman.getPersistentDataContainer().set(friendKey, PersistentDataType.STRING, player.getUniqueId().toString());

        String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "¡Protege al Golem hasta que se derrita!";
        actionBarHandler.sendActionBar(player, msg);

        // Iniciamos el Custom Melting y el Aggro de los Mobs
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!snowman.isValid() || snowman.isDead()) {
                    this.cancel();
                    return;
                }

                // RADAR DE ENEMIGOS: Cada 10 ticks (0.5s) provocamos a los mobs cercanos
                for (Entity e : snowman.getNearbyEntities(15, 15, 15)) {
                    if (e instanceof Enderman || e instanceof PiglinAbstract || e instanceof Zoglin) {
                        if (e instanceof Mob mob) {
                            if (mob.getTarget() == null || !(mob.getTarget() instanceof Snowman)) {
                                mob.setTarget(snowman);
                            }
                        }
                    }
                }

                // DERRETIMIENTO CUSTOM: Cada 6 ciclos (60 ticks = 3 segundos)
                if (ticks % 6 == 0) {
                    snowman.setMetadata("custom_melt", new FixedMetadataValue(plugin, true));
                    snowman.damage(1.0); // Le hacemos 1 de daño exacto
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    @EventHandler
    public void onSnowmanDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Snowman snowman)) return;
        if (!snowman.getPersistentDataContainer().has(friendKey, PersistentDataType.STRING)) return;

        EntityDamageEvent damageEvent = snowman.getLastDamageCause();
        if (damageEvent == null) return;

        if (damageEvent.getCause() == EntityDamageEvent.DamageCause.CUSTOM && snowman.hasMetadata("custom_melt")) {

            String playerUUID = snowman.getPersistentDataContainer().get(friendKey, PersistentDataType.STRING);
            Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));

            if (player != null && player.isOnline() && missionHandler.isMissionActive(player, 12)) {
                MissionData data = missionHandler.getData(player, 12);
                if (data.isCompleted()) return;

                int current = data.getProgressInt("snowmen_melted");
                if (current < 10) {
                    current++;
                    data.setProgressValue("snowmen_melted", current);
                    missionHandler.saveData(player, 12, data);

                    if (current >= 10) {
                        successNotification.showSuccess(player);
                        missionHandler.completeMission(player, 12);
                    } else {
                        String color = current >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
                        String msg = ChatColor.GOLD + "۞ " +
                                ChatColor.of("#FFCC99") + "Amigos derretidos: " + color + current + ChatColor.of("#FFE4B5") + "/10";
                        actionBarHandler.sendActionBar(player, msg);
                    }
                }
            }
        }
    }
}