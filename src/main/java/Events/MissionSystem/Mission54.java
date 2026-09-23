package Events.MissionSystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

import static Events.MissionSystem.MissionRewards.*;

public class Mission54 extends BaseMission {
    private static final Set<String> WEAPONS = Set.of("espada_celestita", "hacha_celestita", "lanza_celestita");

    public Mission54(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 54, "Armas para el Rey", MissionDifficulty.DIFICIL, 18,
                "Craftea una espada, hacha o lanza de Celestita.");
        flag("arma", "Arma de Celestita");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_regeneration_3", 3), custom("amuleto_inmortalidad", 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Las armas de Celestita se reconocen por su id de item custom
    @Override
    protected void tick(Player player) {
        if (MissionUtils.has(player, item -> WEAPONS.contains(MissionUtils.customId(item)))) mark(player, "arma");
    }
}
