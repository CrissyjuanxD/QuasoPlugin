package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

// Misiones 27 a 30: matar al mob infestado de un bioma y minar el Mineral Profundo de ese bioma
public abstract class BiomeHuntMission extends BaseMission {
    private final String mobKey;
    private final WardenBiome biome;

    protected BiomeHuntMission(JavaPlugin plugin, MissionHandler handler, int number, String name, int coins,
                               String description, String mobKey, String mobLabel, WardenBiome biome, String oreLabel) {
        super(plugin, handler, number, name, MissionDifficulty.DIFICIL, coins, description);
        this.mobKey = mobKey;
        this.biome = biome;
        counter("mobs", mobLabel, 50);
        counter("minerales", oreLabel, 16);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), mobKey)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "mobs", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (WardenBiome.fromOre(event.getBlock().getType()) == biome) add(event.getPlayer(), "minerales", 1);
    }
}
