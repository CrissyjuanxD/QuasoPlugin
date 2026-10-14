package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission212 extends MisionCrafteo {

    public Mission212(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 212, "Alas de Warden", MissionDifficulty.DIFICIL, 20,
                "Forja el Peto de Warden Alado en la herrería: la EnderKing Pearl, el Peto de Warden y unas Elytras. "
                        + "Planea como unas Elytras.");
        objetivo("peto", "Peto de Warden Alado", id("peto_warden_alado"));
        extraOf(55);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.FIREWORK_ROCKET, 64), item(Material.GOLDEN_APPLE, 8));
    }
}
