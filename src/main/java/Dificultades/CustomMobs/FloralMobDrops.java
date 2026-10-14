package Dificultades.CustomMobs;

import items.FloralMobItems;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/** Una tirada del 40% por muerte, también para mobs creados por comandos antes del día 1. Si sale, el Saqueo da más:
 *  con II de 1 a 2 y con III de 1 a 3. Los que invoca la Abeja Floral en su pelea no sueltan nada, si no se farmean en
 *  el boss. */
public final class FloralMobDrops implements Listener {
    private final NamespacedKey zombieKey;
    private final NamespacedKey spiderKey;
    private final NamespacedKey creeperKey;
    private final NamespacedKey beeKey;
    private final NamespacedKey summonKey;
    private final DoubleSupplier random;
    static final double PROBABILIDAD = 0.40;
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
        LivingEntity mob = event.getEntity();
        if (marked(mob, summonKey)) return;
        ItemStack drop = drop(mob);
        if (drop == null || random.getAsDouble() >= PROBABILIDAD) return;
        drop.setAmount(cantidad(saqueo(mob), random.getAsDouble()));
        event.getDrops().add(drop);
    }

    private ItemStack drop(Entity mob) {
        if (mob instanceof Zombie && marked(mob, zombieKey)) return FloralMobItems.createFlesh();
        if (mob instanceof Spider && marked(mob, spiderKey)) return FloralMobItems.createEye();
        if (mob instanceof Creeper && marked(mob, creeperKey)) return FloralMobItems.createGunpowder();
        if (mob instanceof Bee && marked(mob, beeKey)) return FloralMobItems.createStinger();
        return null;
    }

    // Sin Saqueo o con I sale 1; con II de 1 a 2 y con III de 1 a 3 (azar de 0 a 1)
    static int cantidad(int saqueo, double azar) {
        int maximo = Math.max(1, saqueo);
        return Math.min(maximo, 1 + (int) (azar * maximo));
    }

    private static int saqueo(LivingEntity mob) {
        Player killer = mob.getKiller();
        return killer == null ? 0 : killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
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
