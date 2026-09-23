package Events.MissionSystem;

import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission79 extends BaseMission {

    public Mission79(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 79, "Suerte de principiante", MissionDifficulty.MEDIA, 13,
                "Gana 10 manos de Blackjack en el casino.");
        counter("manos", "Manos ganadas", 10);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("dinofichas", 25), drinks(3));
    }

    // El Blackjack avisa cada mano ganada con un MissionTriggerEvent
    @EventHandler
    public void onTrigger(MissionTriggerEvent event) {
        if (event.getAction().equals("blackjack_ganada")) add(event.getPlayer(), "manos", event.getAmount());
    }
}
