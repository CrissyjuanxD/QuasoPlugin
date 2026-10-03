package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission24 extends BaseMission {

    public Mission24(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 24, "El corazón de la cueva", MissionDifficulty.DIFICIL, 20,
                "Mata al Infested Warden Boss, que suelta los Corazones para invocar al Ultra Warden.");
        flag("jefe", "Infested Warden Boss derrotado");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.PROTECTION, 5), custom("splash_resistance_3", 2));
    }

    // El jefe tiene que llevar la PDC boss_id = infested_warden_boss
    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("infested_warden_boss")) return;
        for (Player player : event.getPlayers()) mark(player, "jefe");
    }
}
