package Bosses;

import Dificultades.CustomMobs.WardenZombie;
import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import items.WardenCaveItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// Mini boss de las Ancient City de la Warden Cave. Pelea como la Abeja Reina: de 1 a 3 ataques cuerpo a cuerpo
// y después uno especial. Los especiales se pueden esquivar: rayos que se marcan antes, una onda que se salta
// y círculos en el suelo de los que hay que salir. Va cambiando de jugador cada tanto, así no persigue siempre al mismo
// (el daño está pensado para Netherite con Protección IV)
public class InfestedWardenBoss extends BaseBoss implements Listener {

    public static final String BOSS_ID = "infested_warden_boss";
    public static final Map<UUID, InfestedWardenBoss> ACTIVE_BOSSES = new HashMap<>();

    private static final double MAX_HEALTH = 450;
    private static final double ATTACK_DAMAGE = 14;
    private static final int MAX_MINIONS = 4;
    private static final String TITLE = ChatColor.of("#29DFEB") + "" + ChatColor.BOLD + "Infested Warden";

    private static final Particle.DustOptions SCULK_DUST = new Particle.DustOptions(Color.fromRGB(0x0BB5B5), 1.6f);
    private static final Particle.DustOptions WARNING_DUST = new Particle.DustOptions(Color.fromRGB(0x29DFEB), 1.2f);
    private static final BlockData SCULK_BLOCK = Material.SCULK.createBlockData();

    private final Warden warden;
    private final WardenZombie minionFactory;
    private final Random random = new Random();
    private final List<UUID> minions = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    private final NamespacedKey arenaX;
    private final NamespacedKey arenaY;
    private final NamespacedKey arenaZ;

    private int globalTick = 0;
    private boolean busy = false;
    private boolean customBoom = false;
    private int meleeSinceSpecial = 0;
    private int meleeBeforeSpecial = 2;
    private UUID focus;
    private int focusUntil = 0;

    // Si el warden ya era boss (chunk recargado) recupera el centro de la arena de su PDC
    public InfestedWardenBoss(JavaPlugin plugin, Warden warden) {
        super(plugin, warden);
        this.warden = warden;
        this.minionFactory = new WardenZombie(plugin);
        updateStats();
        this.arenaX = new NamespacedKey(plugin, "arena_x");
        this.arenaY = new NamespacedKey(plugin, "arena_y");
        this.arenaZ = new NamespacedKey(plugin, "arena_z");

        PersistentDataContainer pdc = warden.getPersistentDataContainer();
        if (pdc.has(arenaX, PersistentDataType.DOUBLE)) {
            spawnLocation.setX(pdc.get(arenaX, PersistentDataType.DOUBLE));
            spawnLocation.setY(pdc.get(arenaY, PersistentDataType.DOUBLE));
            spawnLocation.setZ(pdc.get(arenaZ, PersistentDataType.DOUBLE));
        } else {
            pdc.set(arenaX, PersistentDataType.DOUBLE, spawnLocation.getX());
            pdc.set(arenaY, PersistentDataType.DOUBLE, spawnLocation.getY());
            pdc.set(arenaZ, PersistentDataType.DOUBLE, spawnLocation.getZ());
        }

        ACTIVE_BOSSES.put(warden.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        start();
    }

    // Spawnea el boss con sus stats; lairKey es la ciudad a la que pertenece (para el respawn)
    public static InfestedWardenBoss spawn(JavaPlugin plugin, Location location, String lairKey) {
        NamespacedKey bossIdKey = new NamespacedKey(plugin, "boss_id");
        NamespacedKey lairKeyKey = new NamespacedKey(plugin, "warden_lair");

        Warden warden = location.getWorld().spawn(location, Warden.class, w -> {
            w.setCustomName(TITLE);
            w.setCustomNameVisible(true);
            w.setRemoveWhenFarAway(false);
            w.setPersistent(true);
            setAttribute(w.getAttribute(Attribute.MAX_HEALTH), MAX_HEALTH);
            w.setHealth(MAX_HEALTH);
            setAttribute(w.getAttribute(Attribute.ATTACK_DAMAGE), ATTACK_DAMAGE);
            setAttribute(w.getAttribute(Attribute.KNOCKBACK_RESISTANCE), 1);
            setAttribute(w.getAttribute(Attribute.FOLLOW_RANGE), 40);
            setAttribute(w.getAttribute(Attribute.SCALE), 1.2);
            w.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, BOSS_ID);
            w.getPersistentDataContainer().set(lairKeyKey, PersistentDataType.STRING, lairKey);
        });

        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_WARDEN_EMERGE, 3f, 0.8f);
        world.spawnParticle(Particle.SCULK_SOUL, location.clone().add(0, 1, 0), 40, 1.2, 1.5, 1.2, 0.05);
        world.spawnParticle(Particle.BLOCK, location, 60, 1.5, 0.3, 1.5, 0, SCULK_BLOCK);
        return new InfestedWardenBoss(plugin, warden);
    }

    // Un boss que quedó de una versión anterior toma los stats de ahora, con el mismo porcentaje de vida
    private void updateStats() {
        AttributeInstance maxHealth = warden.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || maxHealth.getBaseValue() == MAX_HEALTH) return;
        double ratio = warden.getHealth() / maxHealth.getBaseValue();
        maxHealth.setBaseValue(MAX_HEALTH);
        warden.setHealth(Math.max(1, Math.min(MAX_HEALTH, MAX_HEALTH * ratio)));
        setAttribute(warden.getAttribute(Attribute.ATTACK_DAMAGE), ATTACK_DAMAGE);
    }

    private static void setAttribute(AttributeInstance attribute, double value) {
        if (attribute != null) attribute.setBaseValue(value);
    }

    @Override
    protected String getBossTitle() {
        return TITLE;
    }

    @Override
    protected String getBossArticle() {
        return "al";
    }

    @Override
    protected boolean announceSummon() {
        return false;
    }

    @Override
    protected BarColor getBarColor() {
        return BarColor.BLUE;
    }

    // La arena es toda la caverna de la Ancient City: desde el piso de la ciudad hasta el techo en cúpula
    @Override
    protected int getArenaRadius() {
        return 140;
    }

    @Override
    protected int getArenaHeightUp() {
        return 45;
    }

    @Override
    protected int getArenaHeightDown() {
        return 30;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.CIRCULAR;
    }

    @Override
    protected void onStart() {
        meleeBeforeSpecial = random.nextInt(3) + 1;
    }

    // Cada segundo se enoja con el jugador al que persigue y se olvida del resto (así no se mete bajo tierra) y cada
    // 3 segundos (2 con menos de la mitad de vida) elige el siguiente ataque
    @Override
    protected void onTick() {
        if (isHibernating()) return;
        globalTick++;

        if (globalTick % 20 == 0) {
            Player target = focusTarget();
            if (target != null) {
                for (Player p : activePlayers()) {
                    if (p.equals(target)) warden.setAnger(p, 150);
                    else warden.clearAnger(p);
                }
            }
        }

        if (busy) return;
        int delay = enraged() ? 40 : 60;
        if (globalTick % delay == 0) decideNextAttack();
    }

    @Override
    protected void onDeath() {
        cleanup();
    }

    @Override
    protected void onUnload() {
        cleanup();
    }

    private void cleanup() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        HandlerList.unregisterAll(this);
        ACTIVE_BOSSES.remove(warden.getUniqueId());
    }

    private boolean enraged() {
        return warden.getHealth() < MAX_HEALTH / 2;
    }

    private boolean alive() {
        return warden.isValid() && !warden.isDead() && !isHibernating();
    }

    private List<Player> activePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(warden.getWorld())
                    && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                list.add(p);
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(warden.getLocation())));
        return list;
    }

    private Player nearestPlayer() {
        List<Player> players = activePlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    // El jugador al que persigue: cada 8 a 14 segundos cambia a otro al azar (si hay más de uno en la arena)
    private Player focusTarget() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return null;
        Player current = focus != null ? Bukkit.getPlayer(focus) : null;
        if (current != null && players.contains(current) && globalTick < focusUntil) return current;

        List<Player> others = new ArrayList<>(players);
        if (others.size() > 1 && current != null) others.remove(current);
        Player next = others.get(random.nextInt(others.size()));
        setFocus(next);
        return next;
    }

    private void setFocus(Player player) {
        focus = player.getUniqueId();
        focusUntil = globalTick + 160 + random.nextInt(121);
    }

    // Un jugador distinto del que persigue, para el Paso Sombrío
    private Player otherTarget() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return null;
        List<Player> others = new ArrayList<>(players);
        others.removeIf(p -> p.getUniqueId().equals(focus));
        if (others.isEmpty()) return players.get(0);
        return others.get(random.nextInt(others.size()));
    }

    private void decideNextAttack() {
        if (nearestPlayer() == null) return;

        if (meleeSinceSpecial < meleeBeforeSpecial) {
            meleeSinceSpecial++;
            switch (random.nextInt(3)) {
                case 0 -> sculkSwipe();
                case 1 -> seismicSlam();
                default -> shadowStep();
            }
        } else {
            meleeSinceSpecial = 0;
            meleeBeforeSpecial = random.nextInt(3) + 1;
            switch (random.nextInt(4)) {
                case 0 -> sonicBarrage();
                case 1 -> sculkPulse();
                case 2 -> summonInfestation();
                default -> sculkEruption();
            }
        }
    }

    private void run(BukkitRunnable runnable, long delay, long period) {
        tasks.removeIf(BukkitTask::isCancelled);
        tasks.add(runnable.runTaskTimer(plugin, delay, period));
    }

    private void finish() {
        busy = false;
        if (warden.isValid() && !isHibernating()) warden.setAI(true);
    }

    private Vector horizontalTo(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector()).setY(0);
        return dir.lengthSquared() < 0.01 ? new Vector(0, 0, 0) : dir.normalize();
    }

    private void face(Vector dir) {
        if (dir.lengthSquared() < 0.01) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        warden.setRotation(yaw, 0);
    }

    // ---------------------------------------------------------------- Cuerpo a cuerpo

    // Zarpazo de Sculk: se lanza hacia el jugador y barre un arco de 120° delante suyo
    private void sculkSwipe() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive() || !target.isValid()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                double dist = warden.getLocation().distance(target.getLocation());
                if (dist > 3.5 && t < 30) {
                    if (t % 5 == 1) {
                        Vector push = horizontalTo(warden.getLocation(), target.getLocation()).multiply(0.9).setY(0.15);
                        warden.setVelocity(push);
                    }
                    return;
                }
                cancel();
                if (dist <= 6) swipe(horizontalTo(warden.getLocation(), target.getLocation()), 22);
                finish();
            }
        }, 0L, 1L);
    }

    private void swipe(Vector dir, double damage) {
        World world = warden.getWorld();
        Location origin = warden.getLocation().add(0, 1.4, 0);
        face(dir);
        warden.playEffect(EntityEffect.WARDEN_ATTACK);
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        world.playSound(origin, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2f, 0.8f);

        for (int angle = -60; angle <= 60; angle += 15) {
            Vector arc = dir.clone().rotateAroundY(Math.toRadians(angle));
            for (double r = 1.5; r <= 4; r += 1.25) {
                Location point = origin.clone().add(arc.clone().multiply(r));
                world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SCULK_SOUL, point, 2, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticle(Particle.SCULK_CHARGE_POP, point, 3, 0.25, 0.25, 0.25, 0.01);
            }
        }

        for (Player p : activePlayers()) {
            Vector to = p.getLocation().toVector().subtract(warden.getLocation().toVector());
            double dy = Math.abs(to.getY());
            to.setY(0);
            if (to.length() > 4.5 || dy > 3) continue;
            if (to.lengthSquared() > 0.01 && Math.toDegrees(dir.angle(to.clone().normalize())) > 65) continue;

            p.damage(damage, warden);
            p.setVelocity(dir.clone().multiply(1.2).setY(0.4));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
        }
    }

    // Golpe Sísmico: salta hacia el jugador y al caer golpea todo a 5 bloques y los levanta
    private void seismicSlam() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;

        Vector leap = horizontalTo(warden.getLocation(), target.getLocation()).multiply(0.7).setY(0.95);
        face(leap);
        warden.setVelocity(leap);
        warden.getWorld().playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 3f, 0.7f);

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                warden.getWorld().spawnParticle(Particle.SCULK_SOUL, warden.getLocation(), 2, 0.4, 0.2, 0.4, 0.01);
                if ((t > 6 && warden.isOnGround()) || t > 40) {
                    cancel();
                    slamImpact();
                    finish();
                }
            }
        }, 2L, 1L);
    }

    private void slamImpact() {
        World world = warden.getWorld();
        Location center = warden.getLocation();
        warden.playEffect(EntityEffect.WARDEN_ATTACK);
        world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 3f, 0.6f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.BLOCK, center, 80, 2.5, 0.2, 2.5, 0, SCULK_BLOCK);

        for (double r = 1.5; r <= 5; r += 1.75) {
            int points = (int) (r * 6);
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                Location point = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SCULK_CHARGE_POP, point, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }

        for (Player p : activePlayers()) {
            Vector away = p.getLocation().toVector().subtract(center.toVector());
            if (away.length() > 5 || Math.abs(away.getY()) > 3) continue;
            away.setY(0);
            Vector knock = away.lengthSquared() < 0.01 ? new Vector(0, 0, 0) : away.normalize().multiply(0.6);
            p.damage(20, warden);
            p.setVelocity(knock.setY(0.85));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    // Paso Sombrío: se teletransporta detrás de otro jugador (no del que venía persiguiendo) y le pega un zarpazo;
    // desde ahí lo empieza a perseguir a él
    private void shadowStep() {
        Player target = otherTarget();
        if (target == null) return;
        busy = true;

        Vector back = target.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 0.01) back = new Vector(0, 0, 1);
        Location behind = target.getLocation().subtract(back.normalize().multiply(2.5));
        behind.setY(target.getLocation().getY());
        if (!roomFor(behind)) behind = target.getLocation();
        if (!roomFor(behind)) {
            busy = false;
            sculkSwipe();
            return;
        }

        Location destination = behind;
        teleportWithVisual(destination, () -> {
            if (!alive() || !target.isValid()) {
                finish();
                return;
            }
            setFocus(target);
            Vector dir = horizontalTo(warden.getLocation(), target.getLocation());
            swipe(dir, 20);
            finish();
        });
    }

    // El Warden mide 3.5 bloques: hacen falta 4 bloques libres para que no quede asfixiándose
    private boolean roomFor(Location feet) {
        for (int h = 0; h < 4; h++) {
            Block b = feet.clone().add(0, h, 0).getBlock();
            if (!b.isPassable() || b.isLiquid()) return false;
        }
        return feet.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }

    // Teleport con aviso: esfera de partículas donde está y donde va a aparecer, y medio segundo después se mueve
    private void teleportWithVisual(Location to, Runnable after) {
        World world = warden.getWorld();
        Location from = warden.getLocation();
        sphere(from.clone().add(0, 1.6, 0), 2.6);
        sphere(to.clone().add(0, 1.6, 0), 2.6);
        world.playSound(from, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1.4f);
        world.playSound(to, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.5f, 1.6f);

        run(new BukkitRunnable() {
            @Override
            public void run() {
                cancel();
                if (!warden.isValid() || warden.isDead()) {
                    finish();
                    return;
                }
                to.setYaw(warden.getLocation().getYaw());
                warden.teleport(to);
                world.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.6f);
                world.spawnParticle(Particle.SONIC_BOOM, to.clone().add(0, 1.5, 0), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SCULK_SOUL, to.clone().add(0, 1, 0), 20, 0.8, 1, 0.8, 0.05);
                if (after != null) after.run();
            }
        }, 10L, 1L);
    }

    // Esfera de polvo de sculk con almas, como la de la Abeja Reina al teletransportarse
    private void sphere(Location center, double radius) {
        World world = center.getWorld();
        for (double phi = 0; phi < Math.PI; phi += Math.PI / 9) {
            double y = radius * Math.cos(phi);
            double r = radius * Math.sin(phi);
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 9) {
                Location point = center.clone().add(r * Math.cos(theta), y, r * Math.sin(theta));
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, SCULK_DUST);
            }
        }
        world.spawnParticle(Particle.SCULK_SOUL, center, 12, radius / 2, radius / 2, radius / 2, 0.02);
        world.spawnParticle(Particle.SCULK_CHARGE_POP, center, 15, radius / 2, radius / 2, radius / 2, 0.01);
    }

    // Si se sale de la arena vuelve al punto de spawn con el mismo efecto
    @Override
    protected void returnToArena() {
        if (busy) return;
        busy = true;
        teleportWithVisual(spawnLocation.clone(), this::finish);
    }

    // ---------------------------------------------------------------- Especiales

    // Rugido Sónico: marca una línea hacia cada uno de los 3 jugadores más cercanos y 1.75 segundos después dispara
    // por esa línea. La dirección queda fija al marcarla, así que moviéndose de costado se esquiva
    private void sonicBarrage() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        busy = true;
        warden.setAI(false);

        World world = warden.getWorld();
        Location origin = warden.getEyeLocation();
        List<Vector> beams = new ArrayList<>();
        for (Player p : players.subList(0, Math.min(3, players.size()))) {
            Vector dir = p.getEyeLocation().toVector().subtract(origin.toVector());
            if (dir.lengthSquared() > 0.01) beams.add(dir.normalize());
        }
        if (!beams.isEmpty()) face(beams.get(0).clone().setY(0));
        world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_CHARGE, 3f, 1f);

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t % 2 == 0) {
                    for (Vector dir : beams) {
                        for (double d = 1; d <= 24; d += 1) {
                            world.spawnParticle(Particle.DUST, origin.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, WARNING_DUST);
                        }
                    }
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, origin, 6, 0.4, 0.4, 0.4, 0.02);
                }
                if (t < 35) return;

                cancel();
                warden.playEffect(EntityEffect.WARDEN_SONIC_ATTACK);
                world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 1f);
                Set<Player> hit = new HashSet<>();
                for (Vector dir : beams) {
                    for (double d = 1; d <= 24; d += 1) {
                        world.spawnParticle(Particle.SONIC_BOOM, origin.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
                    }
                    for (Player p : activePlayers()) {
                        if (!hit.contains(p) && distanceToBeam(p, origin, dir) <= 1.3) {
                            hit.add(p);
                            sonicDamage(p, 6);
                            p.setVelocity(dir.clone().multiply(1.4).setY(0.35));
                        }
                    }
                }
                finish();
            }
        }, 0L, 1L);
    }

    private double distanceToBeam(Player p, Location origin, Vector dir) {
        Vector toPlayer = p.getLocation().add(0, 1, 0).toVector().subtract(origin.toVector());
        double along = toPlayer.dot(dir);
        if (along < 0 || along > 24) return Double.MAX_VALUE;
        return toPlayer.subtract(dir.clone().multiply(along)).length();
    }

    // El daño sónico ignora la armadura, como el del warden normal
    private void sonicDamage(Player p, double amount) {
        customBoom = true;
        try {
            p.damage(amount, DamageSource.builder(DamageType.SONIC_BOOM).withCausingEntity(warden).withDirectEntity(warden).build());
        } finally {
            customBoom = false;
        }
    }

    // Pulso de Sculk: ruge y sale una onda por el suelo que llega a 18 bloques. Pega a los que están parados en el
    // suelo cuando pasa, así que hay que saltarla
    private void sculkPulse() {
        if (activePlayers().isEmpty()) return;
        busy = true;
        warden.setAI(false);

        World world = warden.getWorld();
        Location center = warden.getLocation();
        world.playSound(center, Sound.ENTITY_WARDEN_ROAR, 3f, 1f);
        for (Player p : activePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.of("#29DFEB") + "¡Salta!"));
        }

        run(new BukkitRunnable() {
            int t = 0;
            double radius = 1;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t <= 20) {
                    world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1, 0), 4, 1.2, 0.8, 1.2, 0.03);
                    return;
                }

                radius += 0.6;
                int points = (int) (radius * 5);
                for (int i = 0; i < points; i++) {
                    double a = 2 * Math.PI * i / points;
                    Location point = center.clone().add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, point, 1, 0.1, 0.05, 0.1, 0);
                    if (i % 2 == 0) world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, SCULK_DUST);
                }
                if (t % 3 == 0) world.playSound(center, Sound.BLOCK_SCULK_SPREAD, 2f, 0.7f);

                for (Player p : activePlayers()) {
                    if (hit.contains(p.getUniqueId()) || !p.isOnGround()) continue;
                    Location loc = p.getLocation();
                    double dist = Math.hypot(loc.getX() - center.getX(), loc.getZ() - center.getZ());
                    if (Math.abs(dist - radius) > 1.0 || Math.abs(loc.getY() - center.getY()) > 2.5) continue;
                    hit.add(p.getUniqueId());
                    p.damage(15, warden);
                    p.setVelocity(p.getVelocity().setY(0.45));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                }

                if (radius >= 18) {
                    cancel();
                    finish();
                }
            }
        }, 0L, 1L);
    }

    // Llamado de la Infestación: de la tierra salen 2 Warden Zombies (3 con menos de la mitad de vida), hasta 4 vivos
    private void summonInfestation() {
        minions.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e == null || e.isDead();
        });
        if (minions.size() >= MAX_MINIONS) {
            sculkEruption();
            return;
        }
        busy = true;

        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ROAR, 3f, 1.2f);
        world.playSound(warden.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 3f, 0.8f);
        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);

        int count = Math.min(enraged() ? 3 : 2, MAX_MINIONS - minions.size());
        List<Location> spots = new ArrayList<>();
        for (int i = 0; i < count * 4 && spots.size() < count; i++) {
            Location spot = groundNear(warden.getLocation(), 4 + random.nextDouble() * 5);
            if (spot != null) spots.add(spot);
        }

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                for (Location spot : spots) {
                    world.spawnParticle(Particle.BLOCK, spot, 8, 0.4, 0.1, 0.4, 0, SCULK_BLOCK);
                    world.spawnParticle(Particle.SCULK_SOUL, spot.clone().add(0, 0.3, 0), 2, 0.3, 0.2, 0.3, 0.02);
                }
                if (t < 25) return;

                cancel();
                Player target = nearestPlayer();
                for (Location spot : spots) {
                    world.playSound(spot, Sound.BLOCK_SCULK_CATALYST_BLOOM, 2f, 0.8f);
                    Zombie minion = minionFactory.spawnWardenZombie(spot);
                    if (target != null) minion.setTarget(target);
                    minions.add(minion.getUniqueId());
                }
                finish();
            }
        }, 0L, 1L);
    }

    // Erupción de Sculk: marca un círculo debajo de cada jugador y otros al azar; 2 segundos después explotan
    // y levantan a los que siguen adentro. El boss se sigue moviendo mientras tanto
    private void sculkEruption() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        busy = true;

        World world = warden.getWorld();
        List<Location> circles = new ArrayList<>();
        for (Player p : players) circles.add(p.getLocation());
        int extra = enraged() ? 4 : 2;
        for (int i = 0; i < extra * 3 && circles.size() < players.size() + extra; i++) {
            Location spot = groundNear(warden.getLocation(), 3 + random.nextDouble() * 12);
            if (spot != null) circles.add(spot);
        }
        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 3f, 0.7f);
        for (Location c : circles) world.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 0.6f);

        final double radius = 2.5;
        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t < 40) {
                    if (t % (t > 30 ? 1 : 3) != 0) return;
                    for (Location c : circles) {
                        for (int i = 0; i < 16; i++) {
                            double a = 2 * Math.PI * i / 16;
                            world.spawnParticle(Particle.DUST, c.clone().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius), 1, 0, 0, 0, 0, WARNING_DUST);
                        }
                        world.spawnParticle(Particle.SCULK_CHARGE_POP, c.clone().add(0, 0.2, 0), 3, radius / 2, 0.05, radius / 2, 0);
                    }
                    return;
                }

                cancel();
                Set<UUID> hit = new HashSet<>();
                for (Location c : circles) {
                    world.playSound(c, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.5f, 0.6f);
                    world.spawnParticle(Particle.SONIC_BOOM, c.clone().add(0, 0.6, 0), 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.BLOCK, c, 40, 1.2, 0.3, 1.2, 0, SCULK_BLOCK);
                    for (double y = 0.2; y <= 4; y += 0.5) {
                        world.spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, y, 0), 3, 0.5, 0.1, 0.5, 0.02);
                    }
                    for (Player p : activePlayers()) {
                        if (hit.contains(p.getUniqueId())) continue;
                        Location loc = p.getLocation();
                        if (Math.hypot(loc.getX() - c.getX(), loc.getZ() - c.getZ()) > radius) continue;
                        if (Math.abs(loc.getY() - c.getY()) > 3) continue;
                        hit.add(p.getUniqueId());
                        p.damage(18, warden);
                        p.setVelocity(p.getVelocity().setY(1.1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                    }
                }
                world.playSound(warden.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 2f, 1.2f);
                finish();
            }
        }, 0L, 1L);
    }

    // Un lugar con suelo firme y 3 bloques libres a esa distancia del boss, dentro de la arena
    private Location groundNear(Location from, double distance) {
        double a = random.nextDouble() * Math.PI * 2;
        Location base = from.clone().add(Math.cos(a) * distance, 0, Math.sin(a) * distance);
        if (!areaZone.isInside(base)) return null;
        World world = from.getWorld();
        for (int dy = 3; dy >= -4; dy--) {
            Block feet = world.getBlockAt(base.getBlockX(), from.getBlockY() + dy, base.getBlockZ());
            if (!feet.getRelative(0, -1, 0).getType().isSolid()) continue;
            if (feet.isPassable() && !feet.isLiquid()
                    && feet.getRelative(0, 1, 0).isPassable() && feet.getRelative(0, 2, 0).isPassable()) {
                return feet.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Eventos

    @EventHandler(ignoreCancelled = true)
    public void onAnger(WardenAngerChangeEvent event) {
        if (event.getEntity().equals(warden) && !(event.getTarget() instanceof Player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(warden)) {
            Player attacker = attacker(event.getDamager());
            if (attacker != null) addAttacker(attacker);
            else if (minions.contains(event.getDamager().getUniqueId())) event.setCancelled(true);
            return;
        }
        // El sonic boom normal del warden pega menos, el fuerte es el del Rugido Sónico
        if (event.getDamager().equals(warden) && event.getCause() == EntityDamageEvent.DamageCause.SONIC_BOOM && !customBoom) {
            event.setDamage(4);
        }
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) return p;
        return null;
    }

    // Suelta de 2 a 4 Corazones de Warden (para invocar al Ultra Warden) y bastante experiencia
    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(EntityDeathEvent event) {
        if (!event.getEntity().equals(warden)) return;
        ItemStack hearts = WardenCaveItems.createWardenBossHeart();
        hearts.setAmount(2 + random.nextInt(3));
        event.getDrops().add(hearts);
        event.setDroppedExp(800);
        warden.getWorld().spawnParticle(Particle.SCULK_SOUL, warden.getLocation().add(0, 1.5, 0), 80, 1.5, 1.5, 1.5, 0.08);
    }
}
