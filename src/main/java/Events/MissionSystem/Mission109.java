package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission109 extends BaseMission {

    public Mission109(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 109, "Lanza de Netherite", MissionDifficulty.MEDIA, 16,
                "Mejora una lanza a Netherite y mata 100 mobs con ella.");
        counter("mobs", "Mobs con lanza de Netherite", 100);
        extraOf(26);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.LUNGE, 3), item(Material.NETHERITE_INGOT, 1));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null && killer.getInventory().getItemInMainHand().getType() == Material.NETHERITE_SPEAR) {
            add(killer, "mobs", 1);
        }
    }
}
