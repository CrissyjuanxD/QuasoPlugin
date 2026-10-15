package Events.MissionSystem;

import BloodMoon.BloodMoonMobs;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission215 extends BaseMission {

    public Mission215(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 215, "Desactivador", MissionDifficulty.MEDIA, 15,
                "Mata 8 Creepers BloodMoon a distancia (flechas, tridente o cualquier proyectil) antes de que te "
                        + "dejen el charco de sangre.");
        counter("creepers", "Creepers BloodMoon a distancia", 8);
        extraOf(83);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.ARROW, 64), book(Enchantment.POWER, 5));
    }

    // Solo cuenta si el golpe final fue un proyectil de un jugador
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (BloodMoonMobs.tipo(event.getEntity()) != BloodMoonMobs.Tipo.CREEPER) return;
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damage)
                || !(damage.getDamager() instanceof Projectile projectile)
                || !(projectile.getShooter() instanceof Player killer)) return;
        add(killer, "creepers", 1);
    }
}
