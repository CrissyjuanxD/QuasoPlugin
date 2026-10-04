package EndBiomes;

import items.EndItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

// Lo que suelta el End: Cristal de Celestita de los racimos de amatista (12%), Fragmento Astral de los mobs del End
// (lo que va en la Plantilla de Celestita) y Esencia Marchita de los Shulkers Negros y los Wither Skeletons del Páramo
public class EndDrops implements Listener {

    private static final double CRYSTAL_CHANCE = 0.12;

    private final Random random = new Random();
    private final NamespacedKey enderInsect;
    private final NamespacedKey blackShulker;
    private final NamespacedKey enderBlaze;
    private final NamespacedKey enderCreeper;
    private final NamespacedKey enderSpider;

    public EndDrops(JavaPlugin plugin) {
        this.enderInsect = new NamespacedKey(plugin, "ender_insect");
        this.blackShulker = new NamespacedKey(plugin, "shulker_negro");
        this.enderBlaze = new NamespacedKey(plugin, "ender_blaze");
        this.enderCreeper = new NamespacedKey(plugin, "ender_creeper");
        this.enderSpider = new NamespacedKey(plugin, "ender_spider");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCluster(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.AMETHYST_CLUSTER || block.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE || random.nextDouble() >= CRYSTAL_CHANCE) return;
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), EndItems.createCelestiteCrystal(1));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        if (entity.getWorld().getEnvironment() != World.Environment.THE_END) return;
        Player killer = entity.getKiller();
        if (killer == null) return;

        if (has(entity, blackShulker)) e.getDrops().add(EndItems.createWitheredEssence(1 + random.nextInt(2)));
        else if (entity.getType() == EntityType.WITHER_SKELETON && EndBiome.isParamo(entity.getLocation().getBlock().getBiome())
                && random.nextDouble() < 0.05) {
            e.getDrops().add(EndItems.createWitheredEssence(1));
        }

        double astral = astralChance(entity);
        if (astral > 0 && random.nextDouble() < astral) e.getDrops().add(EndItems.createAstralFragment(1));
    }

    private double astralChance(LivingEntity entity) {
        if (has(entity, blackShulker)) return 0.25;
        if (has(entity, enderInsect)) return 0.10;
        if (has(entity, enderBlaze) || has(entity, enderCreeper) || has(entity, enderSpider)) return 0.06;
        if (entity.getType() == EntityType.SHULKER) return 0.08;
        if (entity.getType() == EntityType.ENDERMAN) return 0.02;
        return 0;
    }

    private static boolean has(LivingEntity entity, NamespacedKey key) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
