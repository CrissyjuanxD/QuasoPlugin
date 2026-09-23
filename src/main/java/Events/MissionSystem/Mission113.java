package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission113 extends BaseMission {
    private static final Map<Material, String> SPEARS = new LinkedHashMap<>();

    static {
        SPEARS.put(Material.WOODEN_SPEAR, "Madera");
        SPEARS.put(Material.STONE_SPEAR, "Piedra");
        SPEARS.put(Material.COPPER_SPEAR, "Cobre");
        SPEARS.put(Material.IRON_SPEAR, "Hierro");
        SPEARS.put(Material.GOLDEN_SPEAR, "Oro");
        SPEARS.put(Material.DIAMOND_SPEAR, "Diamante");
        SPEARS.put(Material.NETHERITE_SPEAR, "Netherite");
    }

    public Mission113(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 113, "Colección de lanzas", MissionDifficulty.MEDIA, 14,
                "Consigue las 7 lanzas: madera, piedra, cobre, hierro, oro, diamante y netherite.");
        SPEARS.forEach((spear, label) -> flag(spear.name(), "Lanza de " + label));
        extraOf(43);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.LUNGE, 3), book(Enchantment.MENDING, 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        for (Material spear : SPEARS.keySet()) {
            if (player.getInventory().contains(spear)) mark(player, spear.name());
        }
    }
}
