package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission33 extends BaseMission {

    public Mission33(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 33, "Un cambio de look", MissionDifficulty.DIFICIL, 20,
                "Ponte la armadura de Warden completa.");
        flag("armadura", "Armadura de Warden puesta");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_2", 1), custom("amuleto_inmortalidad", 1));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    @Override
    protected void tick(Player player) {
        if (MissionUtils.fullWardenArmor(player)) mark(player, "armadura");
    }
}
