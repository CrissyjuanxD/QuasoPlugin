package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission133 extends BaseMission {

    public Mission133(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 133, "Perlas del vacío", MissionDifficulty.FACIL, 11,
                "Ten 16 perlas de ender en el inventario.");
        counter("perlas", "Perlas de ender", 16);
        extraOf(36);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENDER_EYE, 4), item(Material.GOLDEN_CARROT, 16));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        raise(player, "perlas", MissionUtils.count(player, Material.ENDER_PEARL));
    }
}
