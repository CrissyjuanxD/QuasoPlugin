package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission41 extends BaseMission {

    public Mission41(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 41, "Con alas", MissionDifficulty.MEDIA, 15,
                "Consigue unas Elytra en una End City.");
        flag("elytra", "Elytra conseguidas");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_2", 1), custom("flytotem", 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Las Elytra solo salen en los barcos de las End Cities, así que basta con tenerlas en el End
    @Override
    protected void tick(Player player) {
        if (player.getWorld().getEnvironment() == World.Environment.THE_END && MissionUtils.count(player, Material.ELYTRA) > 0) {
            mark(player, "elytra");
        }
    }
}
