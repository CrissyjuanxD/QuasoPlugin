package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission15 extends BaseMission {
    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    private static final String[] PIECES = {"_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS"};

    static {
        TYPES.put("LEATHER", "Cuero");
        TYPES.put("GOLDEN", "Oro");
        TYPES.put("CHAINMAIL", "Malla");
        TYPES.put("IRON", "Hierro");
        TYPES.put("DIAMOND", "Diamante");
        TYPES.put("NETHERITE", "Netherite");
        TYPES.put("COPPER", "Cobre");
    }

    public Mission15(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 15, "El Mejor Guerrero", MissionDifficulty.MEDIA, 15,
                "Ponte cada pieza de todas las armaduras del juego.");
        TYPES.forEach((type, label) -> counter(type, label, PIECES.length));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("artefacto_nivel_1", 2), book(Enchantment.PROTECTION, 4));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    // Guarda cada pieza que se puso y suma cuántas lleva de cada armadura
    @Override
    protected void tick(Player player) {
        MissionData data = data(player);
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            String name = piece.getType().name();
            for (String type : TYPES.keySet()) {
                if (!name.startsWith(type + "_")) continue;
                String pieceKey = "pieza_" + name;
                if (data.getProgressBool(pieceKey)) continue;

                data.setProgressValue(pieceKey, true);
                int count = 0;
                for (String part : PIECES) {
                    if (data.getProgressBool("pieza_" + type + part)) count++;
                }
                set(player, type, count);
            }
        }
    }
}
