package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission98 extends BaseMission {
    private static final Map<String, String> BOSSES = new LinkedHashMap<>();

    static {
        BOSSES.put("abeja_reina", "Abeja Reina");
        BOSSES.put("ultra_warden", "Ultra Warden");
        BOSSES.put("ender_dragon", "Ender Dragon");
        BOSSES.put("rey_ender", "Rey Ender");
    }

    public Mission98(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 98, "Ultimátum", MissionDifficulty.MUY_DIFICIL, 25,
                "Mata a la Abeja Reina, al Ultra Warden, al Ender Dragon y al Rey Ender en un mismo día.");
        BOSSES.forEach(this::flag);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 2), custom("corrupted_golden_apple", 3));
    }

    // Lo de otro día no cuenta: el menú lo muestra en 0
    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        if (!LocalDate.now().toString().equals(data.getProgressValue("fecha"))) return 0;
        return super.value(player, data, objective);
    }

    // Si el último jefe fue otro día, los 4 vuelven a 0
    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!BOSSES.containsKey(event.getBossId())) return;
        String today = LocalDate.now().toString();

        for (Player player : event.getPlayers()) {
            if (!tracking(player)) continue;
            MissionData data = data(player);
            if (!today.equals(data.getProgressValue("fecha"))) {
                data.setProgressValue("fecha", today);
                for (String boss : BOSSES.keySet()) data.setProgressValue(boss, 0);
                save(player, data);
            }
            mark(player, event.getBossId());
        }
    }
}
