package Events.MissionSystem;

import EndBiomes.EndBiome;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission135 extends BaseMission {

    public Mission135(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 135, "Explorador de colores", MissionDifficulty.FACIL, 12,
                "Visita el Bosque Prismático y el Páramo Marchito del End.");
        flag("bosque_prismatico", "Bosque Prismático");
        flag("paramo_marchito", "Páramo Marchito");
        extraOf(42);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SPYGLASS, 1), item(Material.AMETHYST_SHARD, 16));
    }

    @Override
    protected int tickSeconds() {
        return 5;
    }

    // El Bosque Prismático tiene una variante por color de hojas: cualquiera vale
    @Override
    protected void tick(Player player) {
        Biome biome = player.getLocation().getBlock().getBiome();
        if (EndBiome.isPrismatic(biome)) mark(player, "bosque_prismatico");
        else if (EndBiome.isParamo(biome)) mark(player, "paramo_marchito");
    }
}
