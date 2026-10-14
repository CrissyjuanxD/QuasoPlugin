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
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Una tirada por muerte, también para mobs creados por comandos antes del día 1. Con el cambio uno es 10% y siempre
 *  sale 1 (el Saqueo no cambia nada). Desde el día 12 (cuando se activa la misión 12) es 40% y, si sale, el Saqueo da
 *  más: con II de 1 a 2 y con III de 1 a 3. Los que invoca la Abeja Floral en su pelea no sueltan nada, si no se
 *  farmean en el boss. */
public final class FloralMobDrops implements Listener {
    private final NamespacedKey zombieKey;
    private final NamespacedKey spiderKey;
    private final NamespacedKey creeperKey;
    private final NamespacedKey beeKey;
    private final NamespacedKey summonKey;
    private final DoubleSupplier random;
    private final BooleanSupplier dia12;
    static final double PROBABILIDAD = 0.10;
    static final double PROBABILIDAD_DIA_12 = 0.40;
    // La marca de los mobs que invoca la Abeja Floral
    public static final String INVOCADO = "invocado_abeja_floral";
    // La misión del día 12 (matar a la Abeja Floral): con ella activa los drops suben al 40%
    public static final int MISION_DIA_12 = 12;

    // dia12 dice si ya es el día 12 (la misión 12 está activa)
    public FloralMobDrops(JavaPlugin plugin, BooleanSupplier dia12) {
        this(plugin, () -> ThreadLocalRandom.current().nextDouble(), dia12);
    }
    FloralMobDrops(JavaPlugin plugin, DoubleSupplier random, BooleanSupplier dia12) {
        this.zombieKey = new NamespacedKey(plugin, "corrupted_zombie");
        this.spiderKey = new NamespacedKey(plugin, "corruptedspider");
        this.creeperKey = new NamespacedKey(plugin, "bombita");
        this.beeKey = new NamespacedKey(plugin, "corrupted_bee");
        this.summonKey = new NamespacedKey(plugin, INVOCADO);
        this.random = random;
        this.dia12 = dia12;
    }
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (marked(mob, summonKey)) return;
        ItemStack drop = drop(mob);
        if (drop == null) return;
        boolean potenciado = dia12.getAsBoolean();
        if (random.getAsDouble() >= (potenciado ? PROBABILIDAD_DIA_12 : PROBABILIDAD)) return;
        drop.setAmount(potenciado ? cantidad(saqueo(mob), random.getAsDouble()) : 1);
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
