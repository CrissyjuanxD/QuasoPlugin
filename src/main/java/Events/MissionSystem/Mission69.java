package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission69 extends BaseMission {

    public Mission69(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 69, "Estás demente", MissionDifficulty.MUY_DIFICIL, 22,
                "Aguanta 25 minutos seguidos a medio corazón en la Warden Cave.");
        timer("tiempo", "Tiempo a medio corazón", 1500);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("amuleto_inmortalidad", 1), custom("corrupted_golden_apple", 2));
    }

    @Override
    protected int tickSeconds() {
        return 1;
    }

    // Si se cura o sale de la Warden Cave el contador vuelve a 0
    @Override
    protected void tick(Player player) {
        if (!player.isDead() && player.getHealth() <= 1.0 && MissionUtils.inWardenCave(player)) {
            add(player, "tiempo", 1);
        } else if (data(player).getProgressInt("tiempo") > 0) {
            set(player, "tiempo", 0);
            sendBar(player, ChatColor.RED + "✖ Desafío cancelado, el tiempo volvió a 0");
        }
    }
}
