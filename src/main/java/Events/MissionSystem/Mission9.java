package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission9 extends BaseMission {

    public Mission9(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 9, "No le tengo miedo a nada", MissionDifficulty.MEDIA, 15,
                "Mata 100 mobs durante una BloodMoon (se suman entre noches).");
        counter("mobs", "Mobs en BloodMoon", 100);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("amulet_bloodmoon", 1), book(Enchantment.SHARPNESS, 5));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null && MissionUtils.isBloodMoon(killer.getWorld())) add(killer, "mobs", 1);
    }
}
