package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission31 extends BaseMission {
    private static final int HEIGHT = 200;
    private static final Set<Material> SOFT_LANDING = EnumSet.of(
            Material.WATER, Material.LAVA, Material.COBWEB, Material.VINE, Material.TWISTING_VINES,
            Material.WEEPING_VINES, Material.LADDER, Material.SCAFFOLDING, Material.POWDER_SNOW,
            Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.SWEET_BERRY_BUSH
    );

    private final Map<UUID, Double> startY = new HashMap<>();
    private final Set<UUID> hurt = new HashSet<>();

    public Mission31(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 31, "Salto de fe ardiente", MissionDifficulty.DIFICIL, 17,
                "Cae 200 bloques en el Nether y sobrevive.");
        flag("salto", "Caída de 200 bloques");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_slow_falling", 3), custom("flytotem", 1));
    }

    // Guarda desde dónde empieza a caer; al aterrizar revisa si fueron 200 bloques y que no haya recibido daño de caída
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getY() == event.getTo().getY()) return;
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NETHER || !tracking(player)) return;

        UUID id = player.getUniqueId();
        if (player.isGliding() || player.isFlying()) {
            startY.remove(id);
            return;
        }

        double fromY = event.getFrom().getY();
        double toY = event.getTo().getY();
        boolean soft = SOFT_LANDING.contains(player.getLocation().getBlock().getType())
                || SOFT_LANDING.contains(player.getLocation().subtract(0, 0.1, 0).getBlock().getType());

        if (!player.isOnGround() && !soft && toY < fromY) {
            startY.putIfAbsent(id, fromY);
            double fallen = startY.get(id) - toY;
            if (fallen > 25) {
                sendBar(player, ChatColor.of("#FFCC99") + "Caída: " + (fallen >= HEIGHT ? ChatColor.GREEN : ChatColor.of("#FFA07A"))
                        + (int) fallen + ChatColor.of("#FFE4B5") + "/" + HEIGHT + "m");
            }
            return;
        }

        Double start = startY.remove(id);
        if (start == null || start - Math.min(fromY, toY) < HEIGHT) return;

        hurt.remove(id);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!hurt.remove(id) && player.isOnline() && !player.isDead()) {
                mark(player, "salto");
            } else if (player.isOnline()) {
                sendBar(player, ChatColor.RED + "¡Fallaste! Recibiste daño al aterrizar.");
            }
        }, 2L);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            hurt.add(player.getUniqueId());
        }
    }

    @Override
    public void onQuit(Player player) {
        super.onQuit(player);
        startY.remove(player.getUniqueId());
        hurt.remove(player.getUniqueId());
    }
}
