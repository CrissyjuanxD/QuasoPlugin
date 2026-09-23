package Events.MissionSystem;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission119 extends BaseMission {
    private static final NamespacedKey ADVENTURING_TIME = NamespacedKey.minecraft("adventure/adventuring_time");

    public Mission119(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 119, "Hora de aventuras", MissionDifficulty.DIFICIL, 20,
                "Consigue el logro Hora de aventuras, que ahora también pide las Sulfur Caves.");
        flag("logro", "Hora de aventuras");
        extraOf(90);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_4", 1), custom("frasco_de_velocidad", 4));
    }

    @Override
    protected int tickSeconds() {
        return 10;
    }

    @Override
    protected void tick(Player player) {
        Advancement advancement = Bukkit.getAdvancement(ADVENTURING_TIME);
        if (advancement != null && player.getAdvancementProgress(advancement).isDone()) mark(player, "logro");
    }
}
