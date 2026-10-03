package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission14 extends BaseMission {

    public Mission14(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 14, "Veneno Explosivo", MissionDifficulty.MEDIA, 16,
                "Mata 30 Corrupted Bees y 30 Bombitas.");
        counter("abejas", "Corrupted Bees", 30);
        counter("bombitas", "Bombitas", 30);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.PROTECTION, 5), custom("frasco_de_velocidad", 4));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String key = null;
        if (entity instanceof Bee && MissionUtils.isMob(entity, MissionUtils.CORRUPTED_BEE)) key = "abejas";
        else if (entity instanceof Creeper && MissionUtils.isMob(entity, MissionUtils.BOMBITA)) key = "bombitas";
        if (key == null) return;

        Player killer = MissionUtils.killer(entity);
        if (killer != null) add(killer, key, 1);
    }
}
