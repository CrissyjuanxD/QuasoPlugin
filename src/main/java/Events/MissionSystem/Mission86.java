package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission86 extends BaseMission {

    public Mission86(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 86, "Corazones de Warden", MissionDifficulty.MUY_DIFICIL, 21,
                "Mata al Infested Warden Boss 5 veces.");
        counter("jefes", "Infested Warden Boss", 5);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.PROTECTION, 5), custom("artefacto_nivel_2", 1));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("infested_warden_boss")) return;
        for (Player player : event.getPlayers()) add(player, "jefes", 1);
    }
}
