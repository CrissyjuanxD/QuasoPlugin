package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission39 extends BaseMission {

    public Mission39(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 39, "Ya no me está gustando este sitio", MissionDifficulty.DIFICIL, 17,
                "Mata 50 Shulkers y 50 Ender Spiders.");
        counter("shulkers", "Shulkers", 50);
        counter("aranas", "Ender Spiders", 50);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.PROTECTION, 5), custom("artefacto_nivel_1", 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        String key = event.getEntityType() == EntityType.SHULKER ? "shulkers"
                : MissionUtils.isMob(event.getEntity(), MissionUtils.ENDER_SPIDER) ? "aranas" : null;
        if (key == null) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, key, 1);
    }
}
