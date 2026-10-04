package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission130 extends BaseMission {

    public Mission130(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 130, "Paso silencioso", MissionDifficulty.FACIL, 12,
                "Camina 1.000 bloques agachado.");
        counter("bloques", "Bloques agachado", 1000);
        extraOf(27);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.SWIFT_SNEAK, 1), item(Material.ECHO_SHARD, 2));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Minecraft ya cuenta los centímetros caminados agachado
    @Override
    protected void tick(Player player) {
        statSince(player, "bloques", Statistic.CROUCH_ONE_CM, 100);
    }
}
