package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission10 extends BaseMission {
    private static final Map<Material, String> FLOWERS = new LinkedHashMap<>();

    static {
        FLOWERS.put(Material.DANDELION, "Diente de León");
        FLOWERS.put(Material.POPPY, "Amapola");
        FLOWERS.put(Material.BLUE_ORCHID, "Orquídea Azul");
        FLOWERS.put(Material.ALLIUM, "Allium");
        FLOWERS.put(Material.AZURE_BLUET, "Bluet Azur");
        FLOWERS.put(Material.RED_TULIP, "Tulipán Rojo");
        FLOWERS.put(Material.ORANGE_TULIP, "Tulipán Naranja");
        FLOWERS.put(Material.WHITE_TULIP, "Tulipán Blanco");
        FLOWERS.put(Material.PINK_TULIP, "Tulipán Rosa");
        FLOWERS.put(Material.OXEYE_DAISY, "Margarita");
        FLOWERS.put(Material.CORNFLOWER, "Aciano");
        FLOWERS.put(Material.LILY_OF_THE_VALLEY, "Lirio de los Valles");
        FLOWERS.put(Material.WITHER_ROSE, "Rosa Wither");
        FLOWERS.put(Material.SUNFLOWER, "Girasol");
        FLOWERS.put(Material.LILAC, "Lila");
        FLOWERS.put(Material.ROSE_BUSH, "Rosal");
        FLOWERS.put(Material.PEONY, "Peonía");
        FLOWERS.put(Material.TORCHFLOWER, "Flor Antorcha");
        FLOWERS.put(Material.PITCHER_PLANT, "Planta Jarra");
        FLOWERS.put(Material.PINK_PETALS, "Pétalos Rosas");
        FLOWERS.put(Material.SPORE_BLOSSOM, "Flor de Esporas");
        FLOWERS.put(Material.OPEN_EYEBLOSSOM, "Eyeblossom Abierta");
        FLOWERS.put(Material.CLOSED_EYEBLOSSOM, "Eyeblossom Cerrada");
        FLOWERS.put(Material.WILDFLOWERS, "Flores Silvestres");
        FLOWERS.put(Material.CACTUS_FLOWER, "Flor de Cactus");
        FLOWERS.put(Material.GOLDEN_DANDELION, "Diente de León Dorado");
    }

    public Mission10(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 10, "Stardew Valley", MissionDifficulty.MEDIA, 14,
                "Consigue todas las flores del juego, también las nuevas: eyeblossom, wildflowers, cactus flower y golden dandelion.");
        FLOWERS.forEach((flower, label) -> flag(flower.name(), label));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BONE_BLOCK, 32), item(Material.DIAMOND, 25));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Marca cada flor que tenga en el inventario
    @Override
    protected void tick(Player player) {
        for (Material flower : FLOWERS.keySet()) {
            if (player.getInventory().contains(flower)) mark(player, flower.name());
        }
    }
}
