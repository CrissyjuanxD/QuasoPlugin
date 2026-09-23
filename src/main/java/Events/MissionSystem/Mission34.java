package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission34 extends BaseMission {

    public Mission34(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 34, "¡Jugando con fuego!", MissionDifficulty.DIFICIL, 18,
                "Sobrevive 15 minutos seguidos con medio corazón y la mano secundaria vacía.");
        timer("tiempo", "Tiempo a medio corazón", 900);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_regeneration_3", 3), item(Material.DIAMOND_BLOCK, 8));
    }

    @Override
    protected int tickSeconds() {
        return 1;
    }

    // Si se cura o se pone algo en la mano secundaria el contador vuelve a 0
    @Override
    protected void tick(Player player) {
        boolean holding = !player.isDead() && player.getHealth() <= 1.0 && player.getInventory().getItemInOffHand().getType().isAir();
        if (holding) {
            add(player, "tiempo", 1);
        } else if (data(player).getProgressInt("tiempo") > 0) {
            set(player, "tiempo", 0);
            sendBar(player, ChatColor.RED + "✖ Desafío cancelado, el tiempo volvió a 0");
        }
    }
}
