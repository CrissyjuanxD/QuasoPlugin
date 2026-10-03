package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

import static Events.MissionSystem.MissionRewards.*;

public class Mission21 extends BaseMission {

    public Mission21(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 21, "Turista profundo", MissionDifficulty.MEDIA, 14,
                "Visita los 4 biomas de la Warden Cave.");
        flag(key(WardenBiome.CAVERNA_SCULK), "Caverna Sculk");
        flag(key(WardenBiome.PANTANO_PROFUNDO), "Pantano Profundo");
        flag(key(WardenBiome.ABISMO_FLOTANTE), "Abismo Flotante");
        flag(key(WardenBiome.RUINAS_DE_CENIZA), "Ruinas de Ceniza");
    }

    private static String key(WardenBiome biome) {
        return biome.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("mochila_nivel_2", 1), custom("frasco_de_velocidad", 3));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    // El bioma sale del mismo mapa que usa el generador, así funciona aunque no esté el datapack
    @Override
    protected void tick(Player player) {
        if (MissionUtils.inWardenCave(player)) mark(player, key(MissionUtils.wardenBiome(player.getLocation())));
    }
}
