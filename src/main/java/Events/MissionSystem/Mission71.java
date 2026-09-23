package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission71 extends BaseMission {
    private static final int PER_SOUL = 2;
    // Las almas son echo shards con custom model data del 201 al 204
    private static final Map<Integer, String> SOULS = new LinkedHashMap<>();

    static {
        SOULS.put(201, "Almas de Infested Skeleton");
        SOULS.put(204, "Almas de Infested Cave Spider");
        SOULS.put(202, "Almas de Infested Ghast");
        SOULS.put(203, "Almas de Infested Creeper");
    }

    public Mission71(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 71, "Coleccionista de almas", MissionDifficulty.DIFICIL, 18,
                "Ten a la vez en el inventario 2 almas de cada mob infestado de la Warden Cave.");
        SOULS.forEach((model, label) -> counter("alma_" + model, label, PER_SOUL));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("doubletotem", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Cuenta lo que tiene ahora: si gasta un alma vuelve a bajar
    @Override
    @SuppressWarnings("deprecation")
    protected void tick(Player player) {
        for (int model : SOULS.keySet()) {
            int amount = MissionUtils.count(player, item -> item.getType() == Material.ECHO_SHARD && item.hasItemMeta()
                    && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == model);
            set(player, "alma_" + model, Math.min(PER_SOUL, amount));
        }
    }
}
