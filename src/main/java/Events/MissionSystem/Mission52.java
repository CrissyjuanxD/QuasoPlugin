package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission52 extends BaseMission {

    public Mission52(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 52, "Bajo presión", MissionDifficulty.DIFICIL, 18,
                "Quédate 15 minutos seguidos bajo el agua sin salir.");
        timer("tiempo", "Tiempo bajo el agua", 900);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.TRIDENT, 1), potion(PotionType.LONG_WATER_BREATHING, 3));
    }

    @Override
    protected int tickSeconds() {
        return 1;
    }

    // Si saca la cabeza del agua el contador vuelve a 0
    @Override
    protected void tick(Player player) {
        if (!player.isDead() && player.isUnderWater()) {
            add(player, "tiempo", 1);
        } else if (data(player).getProgressInt("tiempo") > 0) {
            set(player, "tiempo", 0);
            sendBar(player, ChatColor.RED + "✖ Saliste del agua, el tiempo volvió a 0");
        }
    }
}
