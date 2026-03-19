package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Mission7 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    private final Map<UUID, Double> startYMap = new HashMap<>();
    private final Set<UUID> failedAttempt = new HashSet<>();

    public Mission7(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
    }

    @Override
    public String getName() { return "Salto de fe ardiente"; }

    @Override
    public String getDescription() { return "Cae 200 bloques de altura en el Nether y sobrevive el impacto."; }

    @Override
    public int getMissionNumber() { return 7; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);

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
        if (event.getFrom().getY() == event.getTo().getY()) return;

        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;

        if (!missionHandler.isMissionActive(player, 7)) return;
        if (missionHandler.isMissionCompleted(player, 7)) return;

        UUID id = player.getUniqueId();

        if (player.isGliding() || player.isFlying()) {
            startYMap.remove(id);
            return;
        }

        double currentY = event.getTo().getY();
        double prevY = event.getFrom().getY();

        boolean onGround = player.isOnGround();
        Material locMat = player.getLocation().getBlock().getType();
        Material belowMat = player.getLocation().subtract(0, 0.1, 0).getBlock().getType();

        // Bloques que anulan el daño y en los que el jugador podría aterrizar para salvarse (MLG)
        boolean isSafeBlock = locMat == Material.WATER || locMat == Material.LAVA || locMat == Material.COBWEB ||
                locMat == Material.VINE || locMat == Material.TWISTING_VINES || locMat == Material.WEEPING_VINES ||
                locMat == Material.LADDER || locMat == Material.SCAFFOLDING || locMat == Material.POWDER_SNOW ||
                belowMat == Material.SLIME_BLOCK || belowMat == Material.HONEY_BLOCK ||
                belowMat == Material.WATER || belowMat == Material.LAVA || locMat == Material.SWEET_BERRY_BUSH || belowMat == Material.SWEET_BERRY_BUSH;

        if (!onGround && !isSafeBlock && currentY < prevY) {
            // El jugador está cayendo al vacío
            startYMap.putIfAbsent(id, prevY);

            double dist = startYMap.get(id) - currentY;
            if (dist > 25) { // Empezar a mostrar a partir de 3 bloques para no llenar la pantalla por saltitos
                String color = dist >= 200 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
                String msg = ChatColor.GOLD + "۞ " + ChatColor.of("#FFCC99") + "Caída: " + color + (int)dist + ChatColor.of("#FFE4B5") + "/200m";
                actionBarHandler.sendActionBar(player, msg);
            }
        } else {
            // El jugador tocó el suelo, tocó un bloque seguro (MLG nieve/lava/telaraña),
            // o rebotó hacia arriba (Usó su habilidad de Doble Salto a último momento o rebotó en Slime).
            if (startYMap.containsKey(id)) {

                double lowestPoint = Math.min(currentY, prevY);
                double totalDist = startYMap.get(id) - lowestPoint;
                startYMap.remove(id);

                if (totalDist >= 200) {
                    failedAttempt.remove(id);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {

                        if (!failedAttempt.contains(id) && player.isOnline() && !player.isDead()) {
                            // ¡Misión Cumplida! Sobrevivió sin daño.
                            successNotification.showSuccess(player);
                            String msg = ChatColor.GOLD + "۞ " + ChatColor.GREEN + "¡Salto de " + (int)totalDist + "m completado sin rasguños!";
                            actionBarHandler.sendActionBar(player, msg);
                            missionHandler.completeMission(player, 7);
                        } else {
                            // Falló por recibir daño al estamparse contra el suelo
                            String msg = ChatColor.GOLD + "۞ " + ChatColor.RED + "¡Fallaste! Recibiste daño al aterrizar.";
                            actionBarHandler.sendActionBar(player, msg);
                        }

                        failedAttempt.remove(id);
                    }, 2L);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Si el daño fue específicamente por caída, lo anotamos como intento fallido
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                failedAttempt.add(player.getUniqueId());
            }
        }
    }
}