package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission38 extends BaseMission {

    public Mission38(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 38, "Tratar de no morir explotado", MissionDifficulty.MEDIA, 15,
                "Mata 70 Ender Creepers.");
        counter("creepers", "Ender Creepers", 70);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("splash_resistance_3", 1), item(Material.TOTEM_OF_UNDYING, 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!MissionUtils.isMob(event.getEntity(), MissionUtils.ENDER_CREEPER)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "creepers", 1);
    }
}
