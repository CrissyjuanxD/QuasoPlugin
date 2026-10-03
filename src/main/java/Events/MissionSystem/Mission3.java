package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission3 extends BaseMission {

    public Mission3(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 3, "Bienvenido al Nether", MissionDifficulty.FACIL, 11,
                "Entra al Nether y junta 32 de cuarzo.");
        flag("nether", "Entrar al Nether");
        counter("cuarzo", "Cuarzo en el inventario", 32);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(potion(PotionType.LONG_FIRE_RESISTANCE, 2), custom("frasco_de_velocidad", 2));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    @Override
    protected void tick(Player player) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) mark(player, "nether");
        raise(player, "cuarzo", MissionUtils.count(player, Material.QUARTZ));
    }
}
