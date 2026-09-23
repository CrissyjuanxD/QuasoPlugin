package Events.MissionSystem;

import Habilidades.HabilidadesManager;
import Habilidades.HabilidadesType;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission81 extends BaseMission {

    public Mission81(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 81, "Maestro de habilidades", MissionDifficulty.DIFICIL, 18,
                "Sube una habilidad a nivel 8.");
        counter("nivel", "Nivel más alto", 8);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("corrupted_golden_apple", 2), custom("potion_haste_3", 2));
    }

    @Override
    protected int tickSeconds() {
        return 5;
    }

    @Override
    protected void tick(Player player) {
        HabilidadesManager habilidades = QuasoPlugin.getInstance().getHabilidadesManager();
        if (habilidades == null) return;
        int highest = 0;
        for (HabilidadesType type : HabilidadesType.values()) {
            highest = Math.max(highest, habilidades.getHighestLevel(player.getUniqueId(), type));
        }
        raise(player, "nivel", highest);
    }
}
