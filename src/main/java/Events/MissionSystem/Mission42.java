package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission42 extends BaseMission {
    private static final int HEIGHT = 300;
    private static final long WINDOW = 7000;

    private final Map<UUID, Double> startY = new HashMap<>();
    private final Map<UUID, Long> startTime = new HashMap<>();

    public Mission42(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 42, "¡Sé que puedo volar!", MissionDifficulty.MEDIA, 14,
                "Sube 300 bloques en menos de 7 segundos sin Elytras.");
        flag("vuelo", "Subir 300 bloques en 7s");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.FIREWORK_ROCKET, 64), custom("potion_slow_falling", 3));
    }

    // Si en 7 segundos sube 300 bloques sin elytras se completa; si pasa el tiempo empieza a contar de nuevo
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;
        Player player = event.getPlayer();
        if (!tracking(player)) return;

        UUID id = player.getUniqueId();
        if (player.isGliding() || event.getTo().getY() < event.getFrom().getY()) {
            startY.remove(id);
            startTime.remove(id);
            return;
        }

        long now = System.currentTimeMillis();
        if (!startY.containsKey(id) || now - startTime.get(id) > WINDOW) {
            startY.put(id, event.getFrom().getY());
            startTime.put(id, now);
            return;
        }

        double gained = event.getTo().getY() - startY.get(id);
        if (gained >= HEIGHT) {
            startY.remove(id);
            startTime.remove(id);
            mark(player, "vuelo");
        } else if (gained >= 25) {
            double left = (WINDOW - (now - startTime.get(id))) / 1000.0;
            sendBar(player, ChatColor.of("#FFCC99") + "Ascenso: " + ChatColor.of("#FFA07A") + (int) gained + ChatColor.of("#FFE4B5") + "/" + HEIGHT + "m"
                    + ChatColor.GRAY + " | " + ChatColor.of("#FFCC99") + "Tiempo: " + (left > 2 ? ChatColor.GREEN : ChatColor.RED)
                    + String.format(Locale.US, "%.1fs", left));
        }
    }

    @Override
    public void onQuit(Player player) {
        super.onQuit(player);
        startY.remove(player.getUniqueId());
        startTime.remove(player.getUniqueId());
    }
}
