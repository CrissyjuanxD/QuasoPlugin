package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission1 extends BaseMission {
    private static final Map<Material, String> PIECES = Map.of(
            Material.IRON_HELMET, "casco",
            Material.IRON_CHESTPLATE, "peto",
            Material.IRON_LEGGINGS, "pantalon",
            Material.IRON_BOOTS, "botas",
            Material.SHIELD, "escudo"
    );

    public Mission1(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 1, "Primeros pasos", MissionDifficulty.FACIL, 10,
                "Fabrica una armadura completa de hierro y un escudo.");
        flag("casco", "Casco de hierro");
        flag("peto", "Peto de hierro");
        flag("pantalon", "Pantalón de hierro");
        flag("botas", "Botas de hierro");
        flag("escudo", "Escudo");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_1", 1), custom("corrupted_steak", 16));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String piece = PIECES.get(event.getRecipe().getResult().getType());
        if (piece != null) mark(player, piece);
    }
}
