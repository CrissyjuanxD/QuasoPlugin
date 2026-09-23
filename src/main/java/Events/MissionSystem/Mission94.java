package Events.MissionSystem;

import Habilidades.HabilidadesManager;
import Habilidades.HabilidadesType;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

import static Events.MissionSystem.MissionRewards.*;

public class Mission94 extends BaseMission {

    public Mission94(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 94, "Maestro absoluto", MissionDifficulty.MUY_DIFICIL, 23,
                "Sube las 3 habilidades a nivel 8: Vitalidad, Resistencia y Agilidad.");
        for (HabilidadesType type : HabilidadesType.values()) {
            counter(type.name().toLowerCase(Locale.ROOT), type.getDisplayName(), 8);
        }
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ENCHANTED_GOLDEN_APPLE, 5), custom("dinofichas", 50));
    }

    @Override
    protected int tickSeconds() {
        return 5;
    }

    @Override
    protected void tick(Player player) {
        HabilidadesManager habilidades = QuasoPlugin.getInstance().getHabilidadesManager();
        if (habilidades == null) return;
        for (HabilidadesType type : HabilidadesType.values()) {
            raise(player, type.name().toLowerCase(Locale.ROOT), habilidades.getHighestLevel(player.getUniqueId(), type));
        }
    }
}
