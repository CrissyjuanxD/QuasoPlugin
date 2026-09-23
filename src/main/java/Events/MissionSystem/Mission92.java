package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission92 extends BaseMission {

    public Mission92(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 92, "Equipo legendario", MissionDifficulty.MUY_DIFICIL, 22,
                "Ponte la armadura de Warden y lleva la Warden Gun, la Excavadora y la Perla Infinita en el inventario, todo a la vez.");
        flag("armadura", "Armadura de Warden puesta");
        flag("gun", "Warden Gun");
        flag("excavadora", "La Excavadora");
        flag("perla", "Perla Infinita");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_4", 1), custom("corrupted_golden_apple", 2));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Muestra lo que lleva ahora; se completa cuando tiene las 4 cosas al mismo tiempo
    @Override
    protected void tick(Player player) {
        set(player, "armadura", MissionUtils.fullWardenArmor(player) ? 1 : 0);
        set(player, "gun", MissionUtils.has(player, MissionUtils::isWardenGun) ? 1 : 0);
        set(player, "excavadora", MissionUtils.has(player, MissionUtils::isExcavator) ? 1 : 0);
        set(player, "perla", MissionUtils.has(player, MissionUtils::isInfinitePearl) ? 1 : 0);
    }
}
