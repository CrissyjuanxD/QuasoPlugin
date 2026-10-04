package Encantamientos;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.Set;

// De dónde salen los libros: los jefes de la Warden Cave y del End dan uno seguro, los mobs de cada lugar un 1%
// y los cofres de las Ancient City de la Warden Cave y de las End City un 25%
public class EnchantDrops implements Listener {

    private static final double MOB_CHANCE = 0.01;
    private static final double CHEST_CHANCE = 0.25;
    private static final Set<String> WARDEN_CAVE_BOSSES = Set.of("infested_warden_boss", "ultra_warden");
    private static final Set<String> END_BOSSES = Set.of("rey_ender");

    private final NamespacedKey bossIdKey;
    private final Random random = new Random();

    public EnchantDrops(JavaPlugin plugin) {
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        String bossId = entity.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING);

        if (bossId != null && WARDEN_CAVE_BOSSES.contains(bossId)) {
            add(e, QuasoEnchant.randomBook(QuasoEnchant.Group.WARDEN_CAVE, random, 0.5));
        } else if (entity.getType() == EntityType.ENDER_DRAGON || (bossId != null && END_BOSSES.contains(bossId))) {
            give(e, QuasoEnchant.randomBook(QuasoEnchant.Group.END, random, 0.5));
        } else if (entity instanceof Enemy && entity.getKiller() != null && random.nextDouble() < MOB_CHANCE) {
            World world = entity.getWorld();
            if (world.getName().equals(QuasoPlugin.WORLD_NAME)) {
                add(e, QuasoEnchant.randomBook(QuasoEnchant.Group.WARDEN_CAVE, random, 0.3));
            } else if (world.getEnvironment() == World.Environment.THE_END && entity.getType() != EntityType.ENDERMAN) {
                // Los Enderman no: con las granjas saldrían cientos
                add(e, QuasoEnchant.randomBook(QuasoEnchant.Group.END, random, 0.3));
            }
        }
    }

    private void add(EntityDeathEvent e, ItemStack book) {
        if (book != null) e.getDrops().add(book);
    }

    // El dragón muere volando y lo que suelta puede caer al vacío o al portal: el libro va directo al que lo mató
    private void give(EntityDeathEvent e, ItemStack book) {
        if (book == null) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) {
            e.getDrops().add(book);
            return;
        }
        killer.getInventory().addItem(book).values()
                .forEach(left -> killer.getWorld().dropItemNaturally(killer.getLocation(), left));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoot(LootGenerateEvent e) {
        if (e.getLootTable() == null) return;
        NamespacedKey table = e.getLootTable().getKey();
        if (!table.getNamespace().equals(NamespacedKey.MINECRAFT)) return;
        String path = table.getKey();

        QuasoEnchant.Group group = null;
        if ((path.equals("chests/ancient_city") || path.equals("chests/ancient_city_ice_box"))
                && e.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) {
            group = QuasoEnchant.Group.WARDEN_CAVE;
        } else if (path.equals("chests/end_city_treasure")) {
            group = QuasoEnchant.Group.END;
        }
        if (group == null || random.nextDouble() >= CHEST_CHANCE) return;

        ItemStack book = QuasoEnchant.randomBook(group, random, 0.35);
        if (book != null) e.getLoot().add(book);
    }
}
