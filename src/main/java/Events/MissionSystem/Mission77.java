package Events.MissionSystem;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission77 extends BaseMission {

    public Mission77(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 77, "Más allá del límite", MissionDifficulty.MUY_DIFICIL, 22,
                "Llega a 40 corazones (con Vitalidad y la armadura de Warden).");
        counter("corazones", "Corazones", 40);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("corrupted_golden_apple", 3), custom("amuleto_inmortalidad", 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) raise(player, "corazones", (int) (maxHealth.getValue() / 2));
    }
}
