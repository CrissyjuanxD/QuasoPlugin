package Dificultades.CustomMobs;

import Dificultades.Features.InfestedMob;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InfestedCaveSpider extends InfestedMob implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    private final Random random = new Random();

    private static final List<SpiderEffect> POSSIBLE_EFFECTS = Arrays.asList(
            new SpiderEffect("Velocidad",    PotionEffectType.SPEED,         4),
            new SpiderEffect("Regeneración", PotionEffectType.REGENERATION,  2),
            new SpiderEffect("Fuerza",       PotionEffectType.STRENGTH,      4),
            new SpiderEffect("Salto",        PotionEffectType.JUMP_BOOST,    3),
            new SpiderEffect("Brillo",       PotionEffectType.GLOWING,       1),
            new SpiderEffect("Caída lenta",  PotionEffectType.SLOW_FALLING,  1),
            new SpiderEffect("Resistencia",  PotionEffectType.RESISTANCE,    2)
    );

    public InfestedCaveSpider(JavaPlugin plugin) {
        super(plugin, "infested_cave_spider");
    }

    @Override
    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isCustomMob(entity)) entity.remove();
                }
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public CaveSpider spawnInfestedCaveSpider(Location location) {
        CaveSpider spider = (CaveSpider) location.getWorld().spawnEntity(location, EntityType.CAVE_SPIDER);
        applyAttributes(spider);
        activeMobs.add(spider.getUniqueId());
        startGlobalParticleTask();
        return spider;
    }

    // Araña de cueva más grande con 100 de vida y entre 3 y 5 efectos al azar
    private void applyAttributes(CaveSpider spider) {
        spider.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Infested Cave Spider");
        spider.setCustomNameVisible(false);

        spider.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
        spider.setHealth(100);
        spider.getAttribute(Attribute.SCALE).setBaseValue(1.5);
        spider.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(8.0);
        spider.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(32.0);

        List<SpiderEffect> shuffled = new ArrayList<>(POSSIBLE_EFFECTS);
        Collections.shuffle(shuffled, random);
        int numEffects = 3 + random.nextInt(3);

        for (int i = 0; i < numEffects && i < shuffled.size(); i++) {
            SpiderEffect effect = shuffled.get(i);
            spider.addPotionEffect(new PotionEffect(
                    effect.type(), PotionEffect.INFINITE_DURATION,
                    effect.amplifier(), true, true
            ));
        }

        spider.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    // 30% de lanzar un sonic boom al pegarle a un jugador
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isCustomMob(event.getDamager())) return;
        if (!(event.getEntity() instanceof Player target)) return;

        if (Math.random() < 0.3) {
            launchSonicBoom((LivingEntity) event.getDamager(), target);
        }
    }

    // Los proyectiles no le hacen daño
    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!isCustomMob(event.getEntity())) return;

        if (event.getDamager() instanceof Projectile) {
            event.setCancelled(true);
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_SPIDER_HURT, 1.5f, 0.5f);
        event.getEntity().getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                event.getEntity().getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        CaveSpider spider = (CaveSpider) event.getEntity();
        event.getDrops().clear();

        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 2.0f, 0.5f);
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0f, 1.2f);
        spider.getWorld().spawnParticle(Particle.SOUL, spider.getLocation(), 30, 0.5, 0.5, 0.5, 0.2);
        spider.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, spider.getLocation(), 20, 0.5, 0.5, 0.5, 0.03);

        activeMobs.remove(spider.getUniqueId());
    }

    private void startGlobalParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) return;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMobs.isEmpty()) return;
                Iterator<UUID> it = activeMobs.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }
                    entity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                            entity.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.02);
                    entity.getWorld().spawnParticle(Particle.WITCH,
                            entity.getLocation(), 2, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof CaveSpider &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }

    private record SpiderEffect(String name, PotionEffectType type, int amplifier) {}
}