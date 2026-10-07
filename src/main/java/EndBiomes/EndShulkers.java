package EndBiomes;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Shulkers del End (los de las End Cities, no los negros): 30% de soltar su caja, sus balas a veces dan un efecto
// malo o explotan, y al morir dejan una TNT prendida. Ni la TNT ni las explosiones de las balas rompen bloques, así
// las End Cities no quedan destruidas
public class EndShulkers implements Listener {

    private static final double BOX_CHANCE = 0.30;
    private static final int EFFECT_CHANCE = 20;
    private static final int EXPLODE_CHANCE = 10;

    private final JavaPlugin plugin;
    private final BlackShulker blackShulker;
    private final NamespacedKey tntKey;

    public EndShulkers(JavaPlugin plugin, BlackShulker blackShulker) {
        this.plugin = plugin;
        this.blackShulker = blackShulker;
        this.tntKey = new NamespacedKey(plugin, "tnt_shulker");
    }

    private boolean isEndShulker(Entity entity) {
        return entity instanceof Shulker && entity.getWorld().getEnvironment() == World.Environment.THE_END && !blackShulker.is(entity);
    }

    // Balas: 20% de un efecto malo al azar y 10% de una explosión chica donde pegó
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBullet(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof ShulkerBullet bullet) || !(e.getEntity() instanceof Player player)) return;
        if (!(bullet.getShooter() instanceof Shulker shulker) || !isEndShulker(shulker)) return;

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < EFFECT_CHANCE) {
            player.addPotionEffect(randomEffect());
            player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        } else if (roll < EFFECT_CHANCE + EXPLODE_CHANCE) {
            Location loc = bullet.getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> loc.getWorld().createExplosion(loc, 1.5f, false, false, shulker));
        }
    }

    private static PotionEffect randomEffect() {
        List<PotionEffect> effects = List.of(
                new PotionEffect(PotionEffectType.SLOWNESS, 100, 1),
                new PotionEffect(PotionEffectType.WEAKNESS, 100, 0),
                new PotionEffect(PotionEffectType.MINING_FATIGUE, 160, 1),
                new PotionEffect(PotionEffectType.NAUSEA, 100, 0),
                new PotionEffect(PotionEffectType.HUNGER, 160, 1),
                new PotionEffect(PotionEffectType.POISON, 80, 0));
        return effects.get(ThreadLocalRandom.current().nextInt(effects.size()));
    }

    // Al morir: 30% de soltar su caja del color del shulker y una TNT que explota a los 2,5 segundos. Si murió por
    // una explosión no deja otra TNT, así no se encadenan
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        if (!isEndShulker(e.getEntity())) return;
        Shulker shulker = (Shulker) e.getEntity();
        if (ThreadLocalRandom.current().nextDouble() < BOX_CHANCE) e.getDrops().add(new ItemStack(boxOf(shulker.getColor())));

        EntityDamageEvent last = shulker.getLastDamageCause();
        if (last != null && (last.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || last.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) return;

        Location loc = shulker.getLocation().add(0, 0.5, 0);
        loc.getWorld().spawn(loc, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(50);
            tnt.setSource(shulker);
            tnt.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
        });
        loc.getWorld().playSound(loc, Sound.ENTITY_TNT_PRIMED, 1.5f, 0.8f);
    }

    static Material boxOf(DyeColor color) {
        if (color == null) return Material.SHULKER_BOX;
        return Material.valueOf(color.name() + "_SHULKER_BOX");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTnt(EntityExplodeEvent e) {
        if (e.getEntity() instanceof TNTPrimed tnt && tnt.getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) {
            e.blockList().clear();
        }
    }
}
