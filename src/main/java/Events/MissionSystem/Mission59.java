package Events.MissionSystem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission59 extends BaseMission {
    private final List<String> drinks = drinkNames();

    public Mission59(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 59, "De bar en bar", MissionDifficulty.FACIL, 12,
                "Prueba 8 bebidas distintas del bar.");
        counter("bebidas", "Bebidas distintas", 8);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("dinofichas", 20), drinks(5));
    }

    // Las bebidas del bar se reconocen por su nombre
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String name = ChatColor.stripColor(meta.getDisplayName());
        if (!drinks.contains(name)) return;

        Player player = event.getPlayer();
        if (!tracking(player)) return;
        MissionData data = data(player);
        List<String> tried = data.getProgressList("bebidas_lista");
        if (tried.contains(name)) return;

        tried.add(name);
        data.setProgressValue("bebidas_lista", tried);
        set(player, "bebidas", tried.size());
    }
}
