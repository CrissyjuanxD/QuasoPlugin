package Dificultades.CustomMobs;

import items.FloralMobItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/** Una tirada del 30% por muerte, también para mobs creados por comandos antes del día 1. */
public final class FloralMobDrops implements Listener {
    private final NamespacedKey zombieKey;
    private final NamespacedKey spiderKey;
    private final NamespacedKey creeperKey;
    private final DoubleSupplier random;

    public FloralMobDrops(JavaPlugin plugin) {
        this(plugin, () -> ThreadLocalRandom.current().nextDouble());
    }
    FloralMobDrops(JavaPlugin plugin, DoubleSupplier random) {
        this.zombieKey = new NamespacedKey(plugin, "corrupted_zombie");
        this.spiderKey = new NamespacedKey(plugin, "corruptedspider");
        this.creeperKey = new NamespacedKey(plugin, "bombita");
        this.random = random;
    }
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Entity mob = event.getEntity();
        if (mob instanceof Zombie && marked(mob, zombieKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createFlesh());
        } else if (mob instanceof Spider && marked(mob, spiderKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createEye());
        } else if (mob instanceof Creeper && marked(mob, creeperKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createGunpowder());
        }
    }
    public void refreshName(Entity mob) {
        if (mob instanceof Creeper && marked(mob, creeperKey))
            mob.setCustomName(net.md_5.bungee.api.ChatColor.of("#ef94b6") + "§lCreeper Floral");
    }
    @EventHandler
    public void onLoad(EntitiesLoadEvent event) { event.getEntities().forEach(this::refreshName); }
    private boolean marked(Entity mob, NamespacedKey key) {
        return mob.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
