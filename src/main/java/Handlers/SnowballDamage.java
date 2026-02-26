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
    private final double DAMAGE_PER_TICK = 1.0; // ½ corazón por tick
    private final int DURATION_TICKS = 5; // 1 segundo (20 ticks)

    public SnowballDamage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        // Verificar si es una bola de nieve
        if (!(event.getEntity() instanceof Snowball)) {
            return;
        }

        // Obtener el entidad golpeada
        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity)) {
            return; // Solo afectar entidades vivas
        }

        LivingEntity target = (LivingEntity) hitEntity;
        Snowball snowball = (Snowball) event.getEntity();

        // Aplicar daño por tick
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= DURATION_TICKS || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                // Aplicar daño
                target.damage(DAMAGE_PER_TICK, snowball.getShooter() instanceof LivingEntity ?
                        (LivingEntity) snowball.getShooter() : null);
            }
        }.runTaskTimer(plugin, 0L, 1L); // Ejecutar cada tick (20 veces por segundo)
    }
}