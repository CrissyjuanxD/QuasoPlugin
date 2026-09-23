package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission20 extends BaseMission {

    public Mission20(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 20, "¿Qué es este lugar?", MissionDifficulty.FACIL, 12,
                "Explora 10 minutos la Warden Cave.");
        timer("tiempo", "Tiempo en la Warden Cave", 600);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_resistance_2", 2), item(Material.TOTEM_OF_UNDYING, 1));
    }

    @Override
    protected int tickSeconds() {
        return 1;
    }

    @Override
    protected void tick(Player player) {
        if (MissionUtils.inWardenCave(player)) add(player, "tiempo", 1);
    }
}
