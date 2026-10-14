package EndBiomes;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Shulker Negro: vive en los santuarios y agujas de obsidiana del Páramo Marchito. Tiene el doble de vida, sus balas
// pegan el doble y además de hacer levitar dan Wither II y Ceguera. Cada 10 a 14 segundos usa un ataque al jugador que
// vea: la ráfaga de 6 balas, una lluvia de 3 tandas de balas, una nube marchita a sus pies o una onda de choque si está
// cerca. Al morir deja una TNT más fuerte que la de los otros shulkers. Suelta Esencia Marchita (EndDrops)
public class BlackShulker implements Listener {

    private static final double HEALTH = 60;
    private static final int BURST = 6;
    private static final double BURST_RANGE = 16;
    // Potencia de su TNT al morir (la de los otros shulkers es la normal, 4)
    static final float TNT_POWER = 7f;
    private static final double ONDA_RANGE = 7;
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP};

    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Long> nextBurst = new HashMap<>();
    private BukkitTask task;
    private long tick = 0;

    public BlackShulker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "shulker_negro");
    }

    public static Shulker spawn(JavaPlugin plugin, Location location) {
        BlackShulker template = new BlackShulker(plugin);
        return location.getWorld().spawn(location, Shulker.class, template::setup);
    }

    public void setup(Shulker shulker) {
        shulker.setColor(DyeColor.BLACK);
        AttributeInstance health = shulker.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(HEALTH);
        shulker.setHealth(HEALTH);
        shulker.setPersistent(true);
        shulker.setRemoveWhenFarAway(false);
        shulker.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean is(Entity entity) {
        return entity instanceof Shulker && entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void start() {
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 40L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        nextBurst.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBullet(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof ShulkerBullet bullet) || !is((Entity) bullet.getShooter())) return;
        e.setDamage(e.getDamage() * 2);
        if (e.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        }
    }

    // Cada segundo revisa los shulkers negros cerca de cada jugador del End y dispara los que ya pueden
    private void update() {
        tick += 20;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) continue;
                for (Shulker shulker : player.getLocation().getNearbyEntitiesByType(Shulker.class, BURST_RANGE)) {
                    if (!is(shulker) || shulker.isDead() || tick < nextBurst.getOrDefault(shulker.getUniqueId(), 0L)) continue;
                    if (!shulker.hasLineOfSight(player)) continue;
                    attack(shulker, player);
                    nextBurst.put(shulker.getUniqueId(), tick + 200 + ThreadLocalRandom.current().nextInt(81));
                }
            }
        }
        if (nextBurst.size() > 300) nextBurst.keySet().removeIf(id -> Bukkit.getEntity(id) == null);
    }

    // Elige un ataque: la onda de choque solo si el jugador está cerca
    private void attack(Shulker shulker, Player target) {
        boolean cerca = shulker.getLocation().distanceSquared(target.getLocation()) <= ONDA_RANGE * ONDA_RANGE;
        int roll = ThreadLocalRandom.current().nextInt(cerca ? 4 : 3);
        switch (roll) {
            case 0 -> burst(shulker, target);
            case 1 -> rain(shulker, target);
            case 2 -> witherCloud(shulker, target);
            default -> shockwave(shulker);
        }
    }

    // Lluvia de balas: 3 tandas de 3 balas, una cada 15 ticks
    private void rain(Shulker shulker, Player target) {
        shulker.setPeek(1f);
        World world = shulker.getWorld();
        world.playSound(shulker.getLocation(), Sound.ENTITY_SHULKER_OPEN, 1.5f, 0.5f);
        for (int wave = 0; wave < 3; wave++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (shulker.isDead() || !target.isOnline()) return;
                Location center = shulker.getLocation().add(0, 1, 0);
                world.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.6f, 0.6f);
                world.spawnParticle(Particle.SMOKE, center, 12, 0.3, 0.3, 0.3, 0.02);
                for (int i = 0; i < 3; i++) {
                    Location spot = center.clone().add((i - 1) * 0.7, 0.3, 0);
                    world.spawn(spot, ShulkerBullet.class, bullet -> {
                        bullet.setShooter(shulker);
                        bullet.setTarget(target);
                        bullet.setCurrentMovementDirection(BlockFace.UP);
                    });
                }
            }, wave * 15L);
        }
    }

    // Nube marchita: una nube negra a los pies del jugador que da Wither II y Lentitud 8 segundos
    private void witherCloud(Shulker shulker, Player target) {
        shulker.setPeek(0.6f);
        Location at = target.getLocation();
        World world = at.getWorld();
        world.playSound(shulker.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.7f);
        world.spawn(at, org.bukkit.entity.AreaEffectCloud.class, cloud -> {
            cloud.setRadius(3.5f);
            cloud.setDuration(160);
            cloud.setRadiusPerTick(0);
            cloud.setRadiusOnUse(0);
            cloud.setDurationOnUse(0);
            cloud.setWaitTime(10);
            cloud.setReapplicationDelay(20);
            cloud.setColor(org.bukkit.Color.fromRGB(0x1a1a1a));
            cloud.setSource(shulker);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 160, 1), true);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1), true);
        });
        world.spawnParticle(Particle.SQUID_INK, at.clone().add(0, 0.5, 0), 40, 1.5, 0.4, 1.5, 0.02);
    }

    // Onda de choque: los jugadores a 7 bloques reciben 6 de daño, salen volando hacia atrás y levitan un poco
    private void shockwave(Shulker shulker) {
        shulker.setPeek(1f);
        Location center = shulker.getLocation().add(0, 0.5, 0);
        World world = shulker.getWorld();
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);
        world.spawnParticle(Particle.SONIC_BOOM, center, 1);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 60, 2, 1, 2, 0.1);
        for (Player player : center.getNearbyPlayers(ONDA_RANGE)) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) continue;
            player.damage(6, shulker);
            org.bukkit.util.Vector away = player.getLocation().toVector().subtract(center.toVector()).setY(0);
            if (away.lengthSquared() < 0.01) away = new org.bukkit.util.Vector(1, 0, 0);
            player.setVelocity(away.normalize().multiply(1.4).setY(0.6));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1));
        }
    }

    // Al morir deja una TNT más fuerte que la de los otros shulkers (tampoco rompe bloques). Si murió por una explosión
    // no deja otra, así no se encadenan
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        if (!is(e.getEntity())) return;
        nextBurst.remove(e.getEntity().getUniqueId());
        EntityDamageEvent last = e.getEntity().getLastDamageCause();
        if (last != null && (last.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || last.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) return;
        Location loc = e.getEntity().getLocation().add(0, 0.5, 0);
        loc.getWorld().spawn(loc, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(50);
            tnt.setYield(TNT_POWER);
            tnt.setSource(e.getEntity());
            tnt.getPersistentDataContainer().set(EndShulkers.tntKey(plugin), PersistentDataType.BYTE, (byte) 1);
        });
        loc.getWorld().playSound(loc, Sound.ENTITY_TNT_PRIMED, 2f, 0.6f);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.4, 0.4, 0.4, 0.03);
    }

    // Se abre y suelta 6 balas alrededor suyo, cada una sale para un lado distinto y después busca al jugador
    private void burst(Shulker shulker, Player target) {
        World world = shulker.getWorld();
        Location center = shulker.getLocation().add(0, 0.5, 0);
        shulker.setPeek(1f);
        world.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.5f, 0.6f);
        world.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 2f, 0.5f);
        world.spawnParticle(Particle.SMOKE, center, 25, 0.5, 0.5, 0.5, 0.03);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 30, 0.6, 0.6, 0.6, 0.05);

        for (int i = 0; i < BURST; i++) {
            double angle = 2 * Math.PI * i / BURST;
            Location spot = center.clone().add(Math.cos(angle) * 0.9, 0.6 + (i % 2) * 0.4, Math.sin(angle) * 0.9);
            BlockFace face = FACES[i];
            world.spawn(spot, ShulkerBullet.class, bullet -> {
                bullet.setShooter(shulker);
                bullet.setTarget(target);
                bullet.setCurrentMovementDirection(face);
            });
        }
    }
}
