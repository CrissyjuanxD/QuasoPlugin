package Events.MissionSystem;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission116 extends BaseMission {

    public Mission116(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 116, "Geólogo", MissionDifficulty.MEDIA, 15,
                "Junta 1.000 bloques de Sulfur y Cinnabar minando, de cualquier variante.");
        counter("bloques", "Sulfur y Cinnabar", 1000);
        extraOf(60);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_3", 2), custom("artefacto_nivel_1", 1));
    }

    // Cuenta lo que suelta cada bloque que rompe el jugador
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        int amount = 0;
        for (Item item : event.getItems()) {
            String name = item.getItemStack().getType().name();
            if ((name.contains("SULFUR") || name.contains("CINNABAR")) && !name.startsWith("SULFUR_CUBE")) {
                amount += item.getItemStack().getAmount();
            }
        }
        if (amount > 0) add(event.getPlayer(), "bloques", amount);
    }
}
