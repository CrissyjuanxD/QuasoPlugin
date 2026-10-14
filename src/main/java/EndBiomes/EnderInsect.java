package EndBiomes;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Ender Insect: el endermite del Bosque Prismático, con la lógica del Corrupted Insect de Viciont pero de endgame: más
// vida y más daño. Es el doble de grande, rápido y los Enderman no lo atacan. Cada 10 segundos, si su objetivo está a 8
// bloques, carga una esfera de esporas que crece hasta 5 bloques y al reventar envenena (Veneno II) y ciega 8 segundos
// a los jugadores alcanzados. Suena como endermite y como araña. Suelta Fragmento Astral (EndDrops)
public class EnderInsect implements Listener {

    static final double HEALTH = 50;
    static final double DAMAGE = 8;
    // Cada cuánto puede reventar la esfera y a qué distancia tiene que estar su objetivo
    static final long COOLDOWN_TICKS = 200;
    private static final double TRIGGER_RANGE = 8;
    private static final double SPHERE_RADIUS = 5;
    // Un jugador no puede quedar ciego de nuevo hasta que pasen 8 segundos
    private static final long PLAYER_COOLDOWN_TICKS = 160;

    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Long> nextAttack = new HashMap<>();
    private final Map<UUID, Long> playerCooldown = new HashMap<>();
    private final BukkitTask task;
    private long tick = 0;

    public EnderInsect(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "ender_insect");
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 10L);
    }

    public void stop() {
        task.cancel();
        nextAttack.clear();
        playerCooldown.clear();
    }

    public static Endermite spawn(JavaPlugin plugin, Location location) {
        NamespacedKey key = new NamespacedKey(plugin, "ender_insect");
        return location.getWorld().spawn(location, Endermite.class, m -> setup(key, m));
    }

    private static void setup(NamespacedKey key, Endermite insect) {
        attribute(insect, Attribute.MAX_HEALTH, HEALTH);
        attribute(insect, Attribute.ATTACK_DAMAGE, DAMAGE);
        attribute(insect, Attribute.SCALE, 2.0);
        attribute(insect, Attribute.FOLLOW_RANGE, 32);
        insect.setHealth(HEALTH);
        insect.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false));
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

    // Cada medio segundo: no se mueren de viejos como los endermites, hacen ruido de araña y revientan la esfera si su
    // objetivo está cerca y ya pasaron los 10 segundos
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

                if (tick < nextAttack.getOrDefault(insect.getUniqueId(), 0L)) continue;
                if (!(insect.getTarget() instanceof Player target) || !jugable(target)) continue;
                if (insect.getLocation().distanceSquared(target.getLocation()) > TRIGGER_RANGE * TRIGGER_RANGE) continue;
                nextAttack.put(insect.getUniqueId(), tick + COOLDOWN_TICKS);
                sphere(insect);
            }
        }
        if (nextAttack.size() > 500) nextAttack.keySet().removeIf(id -> Bukkit.getEntity(id) == null);
        playerCooldown.values().removeIf(until -> until < tick);
    }

    private static boolean jugable(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    // La esfera de esporas crece 1,5 bloques por tick desde el insecto; al pasar los 5 bloques revienta sobre los
    // jugadores de alrededor. Si el insecto muere antes, no pasa nada
    private void sphere(Endermite insect) {
        insect.getWorld().playSound(insect.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 2.0f);
        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                if (!insect.isValid() || insect.isDead()) {
                    cancel();
                    return;
                }
                Location center = insect.getLocation().add(0, 1.0, 0);
                if (radius > SPHERE_RADIUS) {
                    for (Player player : center.getNearbyPlayers(SPHERE_RADIUS + 0.5)) {
                        if (jugable(player) && player.getLocation().distance(center) <= SPHERE_RADIUS + 0.5) hit(player);
                    }
                    cancel();
                    return;
                }
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 40; i++) {
                    double phi = r.nextDouble() * Math.PI * 2;
                    double theta = Math.acos(r.nextDouble() * 2 - 1);
                    Location at = center.clone().add(radius * Math.sin(theta) * Math.cos(phi), radius * Math.sin(theta) * Math.sin(phi),
                            radius * Math.cos(theta));
                    center.getWorld().spawnParticle(Particle.WITCH, at, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, at, 1, 0, 0, 0, 0);
                }
                radius += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Veneno II 10 segundos y Ceguera 8 segundos, con el sonido del Corrupted Insect (dos veces)
    private void hit(Player player) {
        UUID id = player.getUniqueId();
        if (playerCooldown.getOrDefault(id, -1L) > tick) return;
        playerCooldown.put(id, tick + PLAYER_COOLDOWN_TICKS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0));
        for (int i = 0; i < 2; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 2.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 2.0f);
            }, i * 10L);
        }
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
        if (!is(e.getEntity())) return;
        World world = e.getEntity().getWorld();
        world.playSound(e.getEntity().getLocation(), Sound.ENTITY_ENDERMITE_HURT, SoundCategory.HOSTILE, 1.0f, 2.0f);
        world.playSound(e.getEntity().getLocation(), Sound.ENTITY_SPIDER_HURT, 0.8f, 1.5f);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!is(e.getEntity())) return;
        World world = e.getEntity().getWorld();
        world.playSound(e.getEntity().getLocation(), Sound.ENTITY_ENDERMITE_DEATH, SoundCategory.HOSTILE, 1.0f, 2.0f);
        world.playSound(e.getEntity().getLocation(), Sound.ENTITY_SPIDER_DEATH, 0.9f, 1.5f);
        nextAttack.remove(e.getEntity().getUniqueId());
    }
}
