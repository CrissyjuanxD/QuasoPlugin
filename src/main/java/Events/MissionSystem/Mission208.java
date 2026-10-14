package Events.MissionSystem;

import items.WardenCaveItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission208 extends MisionCrafteo {

    public Mission208(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 208, "Fundición profunda", MissionDifficulty.MEDIA, 13,
                "Funde un Mineral Crudo en el alto horno y fabrica un Lingote Profundo: 8 fragmentos del mismo "
                        + "bioma y un lingote de oro.");
        objetivo("fragmento", "Fragmento Profundo", item -> {
            String id = WardenCaveItems.idOf(item);
            return id != null && id.startsWith("fragmento_profundo_");
        });
        objetivo("lingote", "Lingote Profundo", WardenCaveItems::isDeepIngot);
        extraOf(23);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ECHO_SHARD, 4), item(Material.GOLD_INGOT, 16));
    }
}
