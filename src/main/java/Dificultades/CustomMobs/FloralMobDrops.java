package Dificultades.CustomMobs;

import items.FloralMobItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
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

/** Una tirada del 30% por muerte, también para mobs creados por comandos antes del día 1. Los que invoca la Abeja Floral
 *  en su pelea no sueltan nada, si no se farmean en el boss. */
public final class FloralMobDrops implements Listener {
    private final NamespacedKey zombieKey;
    private final NamespacedKey spiderKey;
    private final NamespacedKey creeperKey;
    private final NamespacedKey beeKey;
    private final NamespacedKey summonKey;
    private final DoubleSupplier random;
    // La marca de los mobs que invoca la Abeja Floral
    public static final String INVOCADO = "invocado_abeja_floral";

    public FloralMobDrops(JavaPlugin plugin) {
        this(plugin, () -> ThreadLocalRandom.current().nextDouble());
    }
    FloralMobDrops(JavaPlugin plugin, DoubleSupplier random) {
        this.zombieKey = new NamespacedKey(plugin, "corrupted_zombie");
        this.spiderKey = new NamespacedKey(plugin, "corruptedspider");
        this.creeperKey = new NamespacedKey(plugin, "bombita");
        this.beeKey = new NamespacedKey(plugin, "corrupted_bee");
        this.summonKey = new NamespacedKey(plugin, INVOCADO);
        this.random = random;
    }
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Entity mob = event.getEntity();
        if (marked(mob, summonKey)) return;
        if (mob instanceof Zombie && marked(mob, zombieKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createFlesh());
        } else if (mob instanceof Spider && marked(mob, spiderKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createEye());
        } else if (mob instanceof Creeper && marked(mob, creeperKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createGunpowder());
        } else if (mob instanceof Bee && marked(mob, beeKey)) {
            if (random.getAsDouble() < .30) event.getDrops().add(FloralMobItems.createStinger());
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
