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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
// pegan el doble y además de hacer levitar dan Wither II y Ceguera. Cada 10 a 14 segundos se abre y le tira una ráfaga
// de 6 balas a la vez al jugador que vea. Suelta Esencia Marchita (EndDrops)
public class BlackShulker implements Listener {

    private static final double HEALTH = 60;
    private static final int BURST = 6;
    private static final double BURST_RANGE = 16;
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
                    burst(shulker, player);
                    nextBurst.put(shulker.getUniqueId(), tick + 200 + ThreadLocalRandom.current().nextInt(81));
                }
            }
        }
        if (nextBurst.size() > 300) nextBurst.keySet().removeIf(id -> Bukkit.getEntity(id) == null);
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
