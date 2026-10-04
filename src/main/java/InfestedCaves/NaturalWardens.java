package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

// Los Warden que llaman los chilladores de la Warden Cave salen a la mitad de tamaño y pegan la mitad, con el golpe
// y con el sonic boom: en la dimensión hay muchos chilladores. Los del plugin (jefes, Mini Wardens) no cambian
public class NaturalWardens implements Listener {

    private static final double SCALE = 0.5;
    private static final double DAMAGE = 0.5;

    private final NamespacedKey naturalKey;

    public NaturalWardens(JavaPlugin plugin) {
        this.naturalKey = new NamespacedKey(plugin, "warden_natural");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Warden warden)) return;
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!warden.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;

        AttributeInstance scale = warden.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(SCALE);
        AttributeInstance damage = warden.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(damage.getBaseValue() * DAMAGE);
        warden.getPersistentDataContainer().set(naturalKey, PersistentDataType.BYTE, (byte) 1);
    }

    // El sonic boom no usa el daño de ataque, se baja acá
    @EventHandler(ignoreCancelled = true)
    public void onSonicBoom(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.SONIC_BOOM) return;
        if (!(e.getDamager() instanceof Warden warden)) return;
        if (!warden.getPersistentDataContainer().has(naturalKey, PersistentDataType.BYTE)) return;
        e.setDamage(e.getDamage() * DAMAGE);
    }
}
