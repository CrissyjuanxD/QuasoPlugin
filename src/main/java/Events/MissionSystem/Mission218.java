package Events.MissionSystem;

import BloodMoon.BloodMoonMobs;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission218 extends BaseMission {

    public Mission218(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 218, "Duelo de arqueros", MissionDifficulty.MEDIA, 15,
                "Mata 10 Esqueletos BloodMoon con arco o ballesta, aguantando su ráfaga de tres flechas.");
        counter("esqueletos", "Esqueletos BloodMoon con flechas", 10);
        extraOf(83);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.INFINITY, 1), item(Material.SPECTRAL_ARROW, 32));
    }

    // El golpe final tiene que ser una flecha (el tridente no cuenta)
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (BloodMoonMobs.tipo(event.getEntity()) != BloodMoonMobs.Tipo.ESQUELETO) return;
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damage)
                || !(damage.getDamager() instanceof AbstractArrow arrow) || arrow instanceof Trident
                || !(arrow.getShooter() instanceof Player killer)) return;
        add(killer, "esqueletos", 1);
    }
}
