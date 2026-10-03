package Events.MissionSystem;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission56 extends BaseMission {
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> pending = new HashMap<>();

    public Mission56(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 56, "Un largo viaje", MissionDifficulty.MEDIA, 14,
                "Recorre 5.000 bloques caminando con Lentitud I.");
        counter("bloques", "Bloques con Lentitud", 5000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("gancho", 1), custom("frasco_de_velocidad", 3));
    }

    @Override
    protected int tickSeconds() {
        return 1;
    }

    // Cada segundo suma lo que caminó con Lentitud, sin montura, sin volar y sin tepeos raros
    @Override
    protected void tick(Player player) {
        UUID id = player.getUniqueId();
        Location now = player.getLocation();
        Location before = lastLocation.put(id, now);

        boolean walking = player.hasPotionEffect(PotionEffectType.SLOWNESS) && !player.isInsideVehicle()
                && !player.isFlying() && !player.isGliding() && !player.isSwimming();
        if (!walking || before == null || before.getWorld() != now.getWorld()) return;

        double dx = now.getX() - before.getX();
        double dz = now.getZ() - before.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 10) return;

        double total = pending.getOrDefault(id, 0.0) + distance;
        int whole = (int) total;
        pending.put(id, total - whole);
        if (whole > 0) add(player, "bloques", whole);
    }

    @Override
    public void onQuit(Player player) {
        super.onQuit(player);
        lastLocation.remove(player.getUniqueId());
        pending.remove(player.getUniqueId());
    }
}
