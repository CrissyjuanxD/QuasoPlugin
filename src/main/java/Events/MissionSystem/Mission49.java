package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission49 extends BaseMission {

    public Mission49(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 49, "Cazador de Guardianes Acuáticos", MissionDifficulty.DIFICIL, 18,
                "Mata 5 Elder Guardians.");
        counter("guardianes", "Elder Guardians", 5);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SPONGE, 32), item(Material.GOLD_BLOCK, 12));
    }

    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("elder_guardian")) return;
        for (Player player : event.getPlayers()) add(player, "guardianes", 1);
    }
}
