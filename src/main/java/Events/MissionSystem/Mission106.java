package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission106 extends BaseMission {

    public Mission106(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 106, "Olor a azufre", MissionDifficulty.FACIL, 12,
                "Encuentra una Sulfur Cave y junta 64 de Sulfur y 32 de Cinnabar.");
        counter("sulfur", "Sulfur", 64);
        counter("cinnabar", "Cinnabar", 32);
        extraOf(17);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_2", 2), potion(PotionType.LONG_NIGHT_VISION, 2));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        raise(player, "sulfur", MissionUtils.count(player, Material.SULFUR));
        raise(player, "cinnabar", MissionUtils.count(player, Material.CINNABAR));
    }
}
