package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// La Spider Floral (antes Corrupted Spider): mismo comportamiento, la clave PDC sigue siendo corruptedspider para que
// las misiones, los spawners y las que ya estaban en el mundo sigan contando
public class CorruptedSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corrupedtedspiderKey;
    private static boolean eventsRegistered = false;
    // Las cargadas, para el aura de partículas
    private static final Set<UUID> activeSpiders = new HashSet<>();
    private static BukkitTask auraTask;

    public CorruptedSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corrupedtedspiderKey = new NamespacedKey(plugin, "corruptedspider");
        MobSoundManager.register(
                corrupedtedspiderKey,
                Sound.ENTITY_SPIDER_AMBIENT,
                Sound.ENTITY_SPIDER_STEP,
                0.6f,
                1.0f
        );
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            for (World world : Bukkit.getWorlds()) {
                for (Spider spider : world.getEntitiesByClass(Spider.class)) {
                    if (isCorruptedSpider(spider)) activeSpiders.add(spider.getUniqueId());
                }
            }
            startAuraTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            if (auraTask != null) auraTask.cancel();
            auraTask = null;
            activeSpiders.clear();
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Spider spider && isCorruptedSpider(spider)) {
                        spider.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    // Cada segundo, unas partículas florales alrededor de cada araña cargada
    private void startAuraTask() {
        if (auraTask != null && !auraTask.isCancelled()) return;
        auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeSpiders.removeIf(id -> {
                Entity entity = Bukkit.getEntity(id);
                if (entity == null || !entity.isValid() || entity.isDead()) return true;
                ParticulasFlorales.aura(entity.getLocation(), entity.getHeight());
                return false;
            });
        }, 20L, 20L);
    }

    public Spider spawnCorruptedSpider(Location location) {
        Spider corruptedSpider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyCorruptedSpiderAttributes(corruptedSpider);
        return corruptedSpider;
    }

    public void transformspawnCorruptedSpider(Spider spider) {
        applyCorruptedSpiderAttributes(spider);
    }

    // Araña con speed y fuerza permanentes
    private void applyCorruptedSpiderAttributes(Spider spider) {
        spider.setCustomName(net.md_5.bungee.api.ChatColor.of("#F6C945") + "" + ChatColor.BOLD + "Spider Floral");
        spider.setCustomNameVisible(false);
        Objects.requireNonNull(spider.getAttribute(Attribute.FOLLOW_RANGE)).setBaseValue(32);
        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        spider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
        spider.getPersistentDataContainer().set(corrupedtedspiderKey, PersistentDataType.BYTE, (byte) 1);
        activeSpiders.add(spider.getUniqueId());
    }

    // Vuelven a la lista cuando se carga su chunk y salen cuando se descargan o hacen despawn
    @EventHandler
    public void onSpiderLoad(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) activeSpiders.add(spider.getUniqueId());
    }

    @EventHandler
    public void onSpiderUnload(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof Spider) activeSpiders.remove(event.getEntity().getUniqueId());
    }


    // Si le pega a un jugador que no se cubre con el escudo le pone una telaraña en los pies
    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider && event.getEntity() instanceof Player) {
            Spider spider = (Spider) event.getDamager();
            Player player = (Player) event.getEntity();

            if (isCorruptedSpider(spider) && !player.isBlocking()) {
                player.getLocation().getBlock().setType(Material.COBWEB);
                ParticulasFlorales.estallido(player.getLocation().add(0, 0.5, 0));
            }
        }
    }

    @EventHandler
    public void onCorruptedSpiderHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onCorruptedSpiderDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);
            activeSpiders.remove(spider.getUniqueId());
            ParticulasFlorales.estallido(spider.getLocation().add(0, 0.5, 0));
        }
    }

    public NamespacedKey getCorruptedSpiderKey() {
        return  corrupedtedspiderKey;
    }

    public boolean isCorruptedSpider(Spider spider) {
        return spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE);
    }

}
