package Handlers;

import org.bukkit.entity.Snowball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SnowballDamage implements Listener {
    private final JavaPlugin plugin;
    private final double DAMAGE_PER_TICK = 1.0;
    private final int DURATION_TICKS = 5;

    public SnowballDamage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Las bolas de nieve hacen 1 de daño por tick durante 5 ticks
    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) {
            return;
        }

        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) hitEntity;
        Snowball snowball = (Snowball) event.getEntity();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= DURATION_TICKS || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                target.damage(DAMAGE_PER_TICK, snowball.getShooter() instanceof LivingEntity ?
                        (LivingEntity) snowball.getShooter() : null);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}