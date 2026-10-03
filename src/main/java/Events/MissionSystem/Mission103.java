package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission103 extends BaseMission {

    public Mission103(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 103, "Desierto maldito", MissionDifficulty.MEDIA, 14,
                "Mata 15 Parched y 5 Camel Husks.");
        counter("parched", "Parched", 15);
        counter("camel_husk", "Camel Husks", 5);
        extraOf(6);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.MILK_BUCKET, 2), potion(PotionType.STRENGTH, 2));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        String key = switch (event.getEntityType()) {
            case PARCHED -> "parched";
            case CAMEL_HUSK -> "camel_husk";
            default -> null;
        };
        if (key == null) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, key, 1);
    }
}
