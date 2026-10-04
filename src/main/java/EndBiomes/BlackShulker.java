package EndBiomes;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// Shulker Negro: vive en los santuarios y agujas de obsidiana del Páramo Marchito. Tiene el doble de vida, sus balas
// pegan el doble y además de hacer levitar dan Wither II y Ceguera. Suelta Esencia Marchita (EndDrops)
public class BlackShulker implements Listener {

    private static final double HEALTH = 60;

    private final NamespacedKey key;

    public BlackShulker(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "shulker_negro");
    }

    public static Shulker spawn(JavaPlugin plugin, Location location) {
        BlackShulker template = new BlackShulker(plugin);
        return location.getWorld().spawn(location, Shulker.class, template::setup);
    }

    public void setup(Shulker shulker) {
        shulker.setColor(DyeColor.BLACK);
        AttributeInstance health = shulker.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(HEALTH);
        shulker.setHealth(HEALTH);
        shulker.setPersistent(true);
        shulker.setRemoveWhenFarAway(false);
        shulker.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean is(Entity entity) {
        return entity instanceof Shulker && entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBullet(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof ShulkerBullet bullet) || !is((Entity) bullet.getShooter())) return;
        e.setDamage(e.getDamage() * 2);
        if (e.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        }
    }
}
