package EndBiomes;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Ender Insect: el endermite del Bosque Prismático. Es más grande y aguanta más, los Enderman no lo atacan, le
// escupe bolas moradas al jugador que envenenan y dejan una nube de esporas 30 segundos, y suena como endermite y
// como araña. Suelta Fragmento Astral (EndDrops)
public class EnderInsect implements Listener {

    private static final int RANGE = 14;
    private static final Color SPORES = Color.fromRGB(0x9B30FF);

    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final NamespacedKey spitKey;
    private final Map<UUID, Long> nextShot = new HashMap<>();
    private long tick = 0;

    public EnderInsect(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "ender_insect");
        this.spitKey = new NamespacedKey(plugin, "ender_insect_spit");
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 10L);
    }

    public static Endermite spawn(JavaPlugin plugin, Location location) {
        NamespacedKey key = new NamespacedKey(plugin, "ender_insect");
        return location.getWorld().spawn(location, Endermite.class, m -> setup(key, m));
    }

    private static void setup(NamespacedKey key, Endermite insect) {
        attribute(insect, Attribute.MAX_HEALTH, 16);
        attribute(insect, Attribute.ATTACK_DAMAGE, 4);
        attribute(insect, Attribute.SCALE, 1.5);
        insect.setHealth(16);
        insect.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    private static void attribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    public boolean is(Entity entity) {
        return entity instanceof Endermite && entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    // Los endermites que spawnea el juego en el Bosque Prismático son Ender Insects
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(e.getEntity() instanceof Endermite insect)) return;
        if (!EndBiome.isPrismatic(e.getLocation().getBlock().getBiome())) return;
        setup(key, insect);
    }

    // Cada medio segundo: no se mueren de viejos como los endermites, hacen ruido de araña y le disparan al jugador
    // más cercano que vean
    private void update() {
        tick += 10;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            for (Endermite insect : world.getEntitiesByClass(Endermite.class)) {
                if (!is(insect) || insect.isDead()) continue;
                insect.setLifetimeTicks(0);
                ThreadLocalRandom r = ThreadLocalRandom.current();
                if (r.nextInt(100) < 4) world.playSound(insect.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.7f, 1.5f);
                if (insect.getVelocity().setY(0).lengthSquared() > 0.002 && r.nextInt(3) == 0) {
                    world.playSound(insect.getLocation(), Sound.ENTITY_SPIDER_STEP, 0.4f, 1.6f);
                }

                if (tick < nextShot.getOrDefault(insect.getUniqueId(), 0L)) continue;
                Player target = target(insect);
                if (target == null) continue;
                shoot(insect, target);
                nextShot.put(insect.getUniqueId(), tick + 60 + r.nextInt(30));
            }
        }
        if (nextShot.size() > 500) nextShot.keySet().removeIf(id -> Bukkit.getEntity(id) == null);
    }

    private Player target(Endermite insect) {
        Player best = null;
        double bestDistance = RANGE * RANGE;
        for (Player player : insect.getWorld().getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) continue;
            double d = player.getLocation().distanceSquared(insect.getLocation());
            if (d < bestDistance && insect.hasLineOfSight(player)) {
                best = player;
                bestDistance = d;
            }
        }
        return best;
    }

    private void shoot(Endermite insect, Player target) {
        Location origin = insect.getLocation().add(0, 0.4, 0);
        Vector direction = target.getEyeLocation().toVector().subtract(origin.toVector());
        double distance = direction.length();
        direction.normalize().multiply(1.1).add(new Vector(0, 0.04 * distance / 4, 0));
        Snowball spit = insect.launchProjectile(Snowball.class, direction);
        spit.setItem(new ItemStack(Material.PURPLE_DYE));
        spit.getPersistentDataContainer().set(spitKey, PersistentDataType.BYTE, (byte) 1);
        World world = insect.getWorld();
        world.playSound(origin, Sound.ENTITY_LLAMA_SPIT, 0.8f, 1.6f);
        world.playSound(origin, Sound.ENTITY_ENDERMITE_AMBIENT, 0.8f, 0.8f);
        world.spawnParticle(Particle.DUST, origin, 8, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(SPORES, 1.2f));
    }

    // La bola envenena al que toca y donde cae deja una nube de esporas que envenena y suena durante 30 segundos
    @EventHandler
    public void onSpitHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball spit) || !spit.getPersistentDataContainer().has(spitKey, PersistentDataType.BYTE)) return;
        Location at = spit.getLocation();
        if (e.getHitEntity() instanceof LivingEntity hit) {
            hit.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
            at = hit.getLocation();
        } else if (e.getHitBlock() != null && e.getHitBlockFace() != null) {
            at = e.getHitBlock().getRelative(e.getHitBlockFace()).getLocation().add(0.5, 0, 0.5);
        }
        spores(at);
    }

    private void spores(Location at) {
        World world = at.getWorld();
        world.spawn(at, AreaEffectCloud.class, cloud -> {
            cloud.setRadius(2.5f);
            cloud.setDuration(600);
            cloud.setRadiusPerTick(0);
            cloud.setRadiusOnUse(0);
            cloud.setDurationOnUse(0);
            cloud.setWaitTime(0);
            cloud.setReapplicationDelay(20);
            cloud.setColor(SPORES);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 0), true);
        });
        world.playSound(at, Sound.BLOCK_SPORE_BLOSSOM_BREAK, 1f, 0.6f);

        new BukkitRunnable() {
            int left = 15;

            @Override
            public void run() {
                if (left-- <= 0) {
                    cancel();
                    return;
                }
                world.playSound(at, Sound.ENTITY_SILVERFISH_AMBIENT, 0.6f, 0.6f);
                world.playSound(at, Sound.BLOCK_SPORE_BLOSSOM_FALL, 0.5f, 0.8f);
                world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, at.clone().add(0, 0.6, 0), 10, 1.4, 0.4, 1.4, 0);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    // Los Enderman lo dejan tranquilo: no lo eligen de objetivo ni le pegan
    @EventHandler(ignoreCancelled = true)
    public void onEndermanTarget(EntityTargetLivingEntityEvent e) {
        if (e.getEntity() instanceof Enderman && is(e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEndermanHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Enderman && is(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent e) {
        if (is(e.getEntity())) e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_SPIDER_HURT, 0.8f, 1.5f);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!is(e.getEntity())) return;
        e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_SPIDER_DEATH, 0.9f, 1.5f);
        nextShot.remove(e.getEntity().getUniqueId());
    }
}
