package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static Events.MissionSystem.MissionRewards.*;

public class Mission57 extends BaseMission {
    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.MELON_STEM, Material.PUMPKIN_STEM, Material.NETHER_WART, Material.COCOA,
            Material.SWEET_BERRY_BUSH, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP
    );

    public Mission57(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 57, "Farmer simulator", MissionDifficulty.MEDIA, 14,
                "Planta 2.000 cultivos de cualquier tipo.");
        counter("cultivos", "Cultivos plantados", 2000);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(enchanted(Material.NETHERITE_HOE, Enchantment.FORTUNE, 3, null, 0), custom("tarta_calabaza_mejorada", 16));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlant(BlockPlaceEvent event) {
        if (CROPS.contains(event.getBlockPlaced().getType())) add(event.getPlayer(), "cultivos", 1);
    }
}
