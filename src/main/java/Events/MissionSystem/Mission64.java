package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission64 extends BaseMission {
    // Los premios del lago son pepitas de hierro con custom model data del 1000 al 1007
    private static final Map<Integer, String> PRIZES = new LinkedHashMap<>();

    static {
        PRIZES.put(1000, "Chatarra");
        PRIZES.put(1001, "Manzana Podrida");
        PRIZES.put(1002, "Zanahoria Encantada");
        PRIZES.put(1003, "Pepitas Oxidadas");
        PRIZES.put(1004, "Pepitas de Diamante");
        PRIZES.put(1005, "Fragmentos de Ámbar");
        PRIZES.put(1006, "Fósiles Pequeños");
        PRIZES.put(1007, "Lingote de Platino");
    }

    public Mission64(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 64, "Los quiero a todos", MissionDifficulty.DIFICIL, 18,
                "Consigue los 8 premios del lago de pesca.");
        PRIZES.forEach((model, label) -> flag("premio_" + model, label));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_3", 1), custom("lingote_platino", 3));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void tick(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.IRON_NUGGET || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) continue;
            int model = item.getItemMeta().getCustomModelData();
            if (PRIZES.containsKey(model)) mark(player, "premio_" + model);
        }
    }
}
