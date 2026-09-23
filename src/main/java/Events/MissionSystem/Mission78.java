package Events.MissionSystem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission78 extends BaseMission {

    public Mission78(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 78, "Explorador del End", MissionDifficulty.DIFICIL, 17,
                "Visita 10 End Cities distintas.");
        counter("ciudades", "End Cities", 10);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SHULKER_BOX, 4), custom("enderbag", 1));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    // Cada ciudad se guarda por el centro de su estructura, así no se cuenta dos veces
    @Override
    protected void tick(Player player) {
        Location location = player.getLocation();
        if (location.getWorld().getEnvironment() != World.Environment.THE_END) return;

        for (GeneratedStructure city : location.getWorld().getStructures(location.getBlockX() >> 4, location.getBlockZ() >> 4, Structure.END_CITY)) {
            BoundingBox box = city.getBoundingBox();
            if (!box.contains(location.toVector())) continue;

            String id = (int) box.getCenterX() + "," + (int) box.getCenterZ();
            MissionData data = data(player);
            List<String> visited = data.getProgressList("ciudades_lista");
            if (visited.contains(id)) return;

            visited.add(id);
            data.setProgressValue("ciudades_lista", visited);
            set(player, "ciudades", visited.size());
            return;
        }
    }
}
