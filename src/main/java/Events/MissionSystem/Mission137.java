package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission137 extends BaseMission {

    public Mission137(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 137, "Flores marchitas", MissionDifficulty.FACIL, 12,
                "Junta 16 rosas del Wither.");
        counter("rosas", "Rosas del Wither", 16);
        extraOf(49);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BONE_MEAL, 32), item(Material.FLOWER_POT, 4));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        raise(player, "rosas", MissionUtils.count(player, Material.WITHER_ROSE));
    }
}
