package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission201 extends MisionCrafteo {

    public Mission201(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 201, "Puntería reforzada", MissionDifficulty.MEDIA, 14,
                "Fabrica el Arco Mejorado: 3 bloques de hierro, 3 bloques de oro, un lingote de Netherite y un arco. "
                        + "Es el único arco que acepta Perforación y Poder VI.");
        objetivo("arco", "Arco Mejorado", id("arco_mejorado"));
        extraOf(1);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ARROW, 64), book(Enchantment.PIERCING, 4));
    }
}
