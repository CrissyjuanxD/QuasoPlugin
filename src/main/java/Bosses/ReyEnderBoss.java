package Bosses;

import Dificultades.CustomMobs.EnderBlaze;
import Dificultades.CustomMobs.EnderSpider;
import EndBiomes.EnderInsect;
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import items.EndItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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

// Rey Ender: el boss más fuerte del server (día 55). Se invoca con el Ojo del Rey Ender en el altar de un Santuario
// Marchito (ReyEnderAltar). Pelea como el Infested Warden: de 1 a 3 ataques cuerpo a cuerpo y después uno de sus 6
// especiales, y al bajar del 66% y del 33% de vida se regenera con 4 cristales que hay que romper.
// Tiene 7000 de vida: el server no deja pasar de 1024, así que el mob tiene 1000 y todo el daño que recibe se divide
// entre 7. Las armas de Celestita le pegan 50% más. El daño está pensado para la armadura de Warden con Protección IV
public class ReyEnderBoss extends BaseBoss implements Listener {

    public static final String BOSS_ID = "rey_ender";
    public static final Map<UUID, ReyEnderBoss> ACTIVE_BOSSES = new HashMap<>();

    public static final double HEALTH = 7000;
    private static final double REAL_HEALTH = 1000;
    private static final double SCALE = HEALTH / REAL_HEALTH;
    private static final double ATTACK_DAMAGE = 26;
    private static final double CELESTITE_BONUS = 1.5;
    private static final double SIZE = 1.7;
    private static final int MAX_MINIONS = 4;
    private static final double[] REGEN_AT = {0.66, 0.33};
    private static final int REGEN_TICKS = 600;
    // Vida por segundo que cura cada cristal (en la vida de 7000)
    private static final double REGEN_PER_CRYSTAL = 12;
    private static final String TITLE = ChatColor.of("#B55CFF") + "" + ChatColor.BOLD + "Rey Ender";

    private static final Particle.DustOptions VOID_DUST = new Particle.DustOptions(Color.fromRGB(0x8A2BE2), 1.6f);
    private static final Particle.DustOptions WARNING_DUST = new Particle.DustOptions(Color.fromRGB(0xD36BFF), 1.2f);

    private final Enderman king;
    private final Random random = new Random();
    private final EnderSpider spiderFactory;
    private final EnderBlaze blazeFactory;
    private final List<UUID> minions = new ArrayList<>();
    private final List<EnderCrystal> crystals = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Set<UUID> royalBullets = new HashSet<>();

    private final NamespacedKey arenaX;
    private final NamespacedKey arenaY;
    private final NamespacedKey arenaZ;
    private final NamespacedKey regensKey;
    private final NamespacedKey minionKey;

    private int globalTick = 0;
    private boolean busy = false;
    private boolean regenerating = false;
    private boolean selfTeleport = false;
    private int regenTicks = 0;
    private int meleeSinceSpecial = 0;
    private int meleeBeforeSpecial = 2;
    private int lastSpecial = -1;
    private UUID focus;
    private int focusUntil = 0;

    // Si el rey ya era boss (chunk recargado) recupera el centro de la arena de su PDC
    public ReyEnderBoss(JavaPlugin plugin, Enderman king) {
        super(plugin, king);
        this.king = king;
        this.spiderFactory = new EnderSpider(plugin);
        this.blazeFactory = new EnderBlaze(plugin);
        this.arenaX = new NamespacedKey(plugin, "arena_x");
        this.arenaY = new NamespacedKey(plugin, "arena_y");
        this.arenaZ = new NamespacedKey(plugin, "arena_z");
        this.regensKey = new NamespacedKey(plugin, "rey_ender_regens");
        this.minionKey = new NamespacedKey(plugin, "rey_ender_minion");

        PersistentDataContainer pdc = king.getPersistentDataContainer();
        if (pdc.has(arenaX, PersistentDataType.DOUBLE)) {
            spawnLocation.setX(pdc.get(arenaX, PersistentDataType.DOUBLE));
            spawnLocation.setY(pdc.get(arenaY, PersistentDataType.DOUBLE));
            spawnLocation.setZ(pdc.get(arenaZ, PersistentDataType.DOUBLE));
        } else {
            pdc.set(arenaX, PersistentDataType.DOUBLE, spawnLocation.getX());
            pdc.set(arenaY, PersistentDataType.DOUBLE, spawnLocation.getY());
            pdc.set(arenaZ, PersistentDataType.DOUBLE, spawnLocation.getZ());
        }
        king.setGravity(true);

        ACTIVE_BOSSES.put(king.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        start();
    }

    public static ReyEnderBoss spawn(JavaPlugin plugin, Location location) {
        NamespacedKey bossIdKey = new NamespacedKey(plugin, "boss_id");
        Enderman king = location.getWorld().spawn(location, Enderman.class, e -> {
            e.setCustomName(TITLE);
            e.setCustomNameVisible(true);
            e.setRemoveWhenFarAway(false);
            e.setPersistent(true);
            setAttribute(e.getAttribute(Attribute.MAX_HEALTH), REAL_HEALTH);
            e.setHealth(REAL_HEALTH);
            setAttribute(e.getAttribute(Attribute.ATTACK_DAMAGE), ATTACK_DAMAGE);
            setAttribute(e.getAttribute(Attribute.KNOCKBACK_RESISTANCE), 1);
            setAttribute(e.getAttribute(Attribute.FOLLOW_RANGE), 48);
            setAttribute(e.getAttribute(Attribute.SCALE), SIZE);
            e.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, BOSS_ID);
        });

        World world = location.getWorld();
        world.strikeLightningEffect(location);
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 4f, 0.5f);
        world.playSound(location, Sound.ENTITY_ENDERMAN_SCREAM, 3f, 0.5f);
        world.spawnParticle(Particle.REVERSE_PORTAL, location.clone().add(0, 2, 0), 200, 1.5, 2.5, 1.5, 0.1);
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 1, 0), 3, 1, 1, 1, 0);
        return new ReyEnderBoss(plugin, king);
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
    protected BarColor getBarColor() {
        return BarColor.PURPLE;
    }

    @Override
    protected double healthScale() {
        return SCALE;
    }

    // La arena es la isla del santuario
    @Override
    protected int getArenaRadius() {
        return 45;
    }

    @Override
    protected int getArenaHeightUp() {
        return 35;
    }

    @Override
    protected int getArenaHeightDown() {
        return 25;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.CIRCULAR;
    }

    @Override
    protected void returnToArena() {
        teleportKing(spawnLocation);
        king.getWorld().playSound(king.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.6f);
    }

    @Override
    protected void onStart() {
        meleeBeforeSpecial = random.nextInt(3) + 1;
    }

    // Cada segundo persigue a su jugador; cada 55 ticks (40 con menos del 35% de vida) elige el siguiente ataque y
    // al cruzar el 66% y el 33% de vida se regenera
    @Override
    protected void onTick() {
        if (isHibernating()) return;
        globalTick++;

        if (globalTick % 20 == 0) {
            minions.removeIf(id -> {
                Entity minion = Bukkit.getEntity(id);
                return minion == null || minion.isDead();
            });
            Player target = focusTarget();
            if (target != null && !regenerating && !busy) king.setTarget(target);
        }
        if (globalTick % 4 == 0) ambient();

        if (regenerating) {
            regenTick();
            return;
        }
        if (busy) return;
        if (shouldRegenerate()) {
            startRegeneration();
            return;
        }
        int delay = enraged() ? 40 : 55;
        if (globalTick % delay == 0) decideNextAttack();
    }

    @Override
    protected void onDeath() {
        for (UUID id : minions) {
            Entity minion = Bukkit.getEntity(id);
            if (minion != null) minion.remove();
        }
        minions.clear();
        cleanup();
    }

    @Override
    protected void onUnload() {
        cleanup();
    }

    private void cleanup() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        crystals.forEach(Entity::remove);
        crystals.clear();
        royalBullets.clear();
        HandlerList.unregisterAll(this);
        ACTIVE_BOSSES.remove(king.getUniqueId());
    }

    private double ratio() {
        return king.getHealth() / REAL_HEALTH;
    }

    private boolean enraged() {
        return ratio() < 0.35;
    }

    private boolean alive() {
        return king.isValid() && !king.isDead() && !isHibernating();
    }

    private int regens() {
        return king.getPersistentDataContainer().getOrDefault(regensKey, PersistentDataType.INTEGER, 0);
    }

    private boolean shouldRegenerate() {
        int done = regens();
        return done < REGEN_AT.length && ratio() < REGEN_AT[done];
    }

    private void ambient() {
        World world = king.getWorld();
        Location loc = king.getLocation();
        world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 2.5, 0), 8, 0.6, 1.4, 0.6, 0.4);
        world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.2, 0), 3, 0.6, 0.1, 0.6, 0, VOID_DUST);
        if (enraged()) world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 3, 0), 6, 0.5, 1.2, 0.5, 0.02);
    }

    private List<Player> activePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(king.getWorld()) && !p.isDead()
                    && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                list.add(p);
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(king.getLocation())));
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

    private boolean valid(Player target) {
        return target.isValid() && !target.isDead() && target.getWorld().equals(king.getWorld());
    }

    private void decideNextAttack() {
        if (nearestPlayer() == null) return;

        if (meleeSinceSpecial < meleeBeforeSpecial) {
            meleeSinceSpecial++;
            switch (random.nextInt(3)) {
                case 0 -> voidClaw();
                case 1 -> starStomp();
                default -> shadowCombo();
            }
            return;
        }

        meleeSinceSpecial = 0;
        meleeBeforeSpecial = enraged() ? random.nextInt(2) + 1 : random.nextInt(3) + 1;
        int special;
        do {
            special = random.nextInt(6);
        } while (special == lastSpecial || (special == 4 && minions.size() >= MAX_MINIONS));
        lastSpecial = special;
        switch (special) {
            case 0 -> starRain();
            case 1 -> royalVolley();
            case 2 -> voidRay();
            case 3 -> blackHole();
            case 4 -> endArmy();
            default -> dimensionalRift();
        }
    }

    private void run(BukkitRunnable runnable, long delay, long period) {
        tasks.removeIf(BukkitTask::isCancelled);
        tasks.add(runnable.runTaskTimer(plugin, delay, period));
    }

    private void finish() {
        busy = false;
        if (king.isValid() && !isHibernating() && !regenerating) king.setAI(true);
    }

    private Vector horizontalTo(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector()).setY(0);
        return dir.lengthSquared() < 0.01 ? new Vector(0, 0, 0) : dir.normalize();
    }

    private void face(Vector dir) {
        if (dir.lengthSquared() < 0.01) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        king.setRotation(yaw, 0);
    }

    private void hurt(Player player, double damage) {
        player.damage(damage, king);
    }

    // Sus propios teleports pasan; los del enderman (al recibir daño o al perseguir) se cancelan
    private void teleportKing(Location to) {
        selfTeleport = true;
        king.teleport(to);
        selfTeleport = false;
    }

    // Mide unos 5 bloques: hace falta ese espacio libre y suelo firme
    private boolean roomFor(Location feet) {
        for (int h = 0; h < 5; h++) {
            Block b = feet.clone().add(0, h, 0).getBlock();
            if (!b.isPassable() || b.isLiquid()) return false;
        }
        return feet.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }

    // El suelo más cercano a esa altura (hasta 4 arriba y 8 abajo), o null si es vacío
    private Location ground(Location around) {
        World world = around.getWorld();
        for (int dy = 4; dy >= -8; dy--) {
            Block feet = world.getBlockAt(around.getBlockX(), around.getBlockY() + dy, around.getBlockZ());
            if (feet.getRelative(BlockFace.DOWN).getType().isSolid() && feet.isPassable() && !feet.isLiquid()) {
                return feet.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    private void circle(Location center, double radius, Particle.DustOptions dust) {
        int points = (int) (radius * 8);
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius), 1, 0, 0, 0, 0, dust);
        }
    }

    // ---------------------------------------------------------------- Cuerpo a cuerpo

    // Zarpazo Abisal: corre hacia el jugador y barre un arco de 150° delante suyo
    private void voidClaw() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!alive() || !valid(target)) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                double dist = king.getLocation().distance(target.getLocation());
                if (dist > 4 && t < 30) {
                    if (t % 5 == 1) king.setVelocity(horizontalTo(king.getLocation(), target.getLocation()).multiply(1.0).setY(0.15));
                    return;
                }
                cancel();
                if (dist <= 6.5) claw(horizontalTo(king.getLocation(), target.getLocation()), 40);
                finish();
            }
        }, 0L, 1L);
    }

    private void claw(Vector dir, double damage) {
        World world = king.getWorld();
        Location origin = king.getLocation().add(0, 2, 0);
        face(dir);
        king.swingMainHand();
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        world.playSound(origin, Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0.6f);

        for (int angle = -75; angle <= 75; angle += 15) {
            Vector arc = dir.clone().rotateAroundY(Math.toRadians(angle));
            for (double r = 1.5; r <= 5; r += 1.2) {
                Location point = origin.clone().add(arc.clone().multiply(r));
                world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.DUST, point, 2, 0.2, 0.2, 0.2, 0, VOID_DUST);
                world.spawnParticle(Particle.REVERSE_PORTAL, point, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }

        for (Player p : activePlayers()) {
            Vector to = p.getLocation().toVector().subtract(king.getLocation().toVector());
            double dy = Math.abs(to.getY());
            to.setY(0);
            if (to.length() > 5.5 || dy > 3.5) continue;
            if (to.lengthSquared() > 0.01 && Math.toDegrees(dir.angle(to.clone().normalize())) > 80) continue;

            hurt(p, damage);
            p.setVelocity(dir.clone().multiply(0.9).setY(0.35));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
        }
    }

    // Pisotón Estelar: salta hacia el jugador y al caer golpea todo a 6 bloques y los levanta
    private void starStomp() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;

        Vector leap = horizontalTo(king.getLocation(), target.getLocation()).multiply(0.75).setY(1.0);
        face(leap);
        king.setVelocity(leap);
        king.getWorld().playSound(king.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 3f, 0.6f);

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
                king.getWorld().spawnParticle(Particle.PORTAL, king.getLocation(), 6, 0.5, 0.3, 0.5, 0.3);
                if ((t > 6 && king.isOnGround()) || t > 40) {
                    cancel();
                    shockwave(king.getLocation(), 6, 36, 0.75);
                    finish();
                }
            }
        }, 2L, 1L);
    }

    private void shockwave(Location center, double radius, double damage, double lift) {
        World world = center.getWorld();
        king.swingMainHand();
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        world.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 0.5, 0), 3, 1, 0.2, 1, 0);
        for (double r = 1.5; r <= radius; r += 1.5) circle(center.clone().add(0, 0.2, 0), r, VOID_DUST);
        world.spawnParticle(Particle.END_ROD, center, 40, radius / 2, 0.3, radius / 2, 0.05);

        for (Player p : activePlayers()) {
            Vector away = p.getLocation().toVector().subtract(center.toVector());
            if (away.length() > radius || Math.abs(away.getY()) > 3) continue;
            away.setY(0);
            Vector knock = away.lengthSquared() < 0.01 ? new Vector(0, 0, 0) : away.normalize().multiply(0.5);
            hurt(p, damage);
            p.setVelocity(knock.setY(lift));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    // Combo Sombrío: aparece detrás del jugador 3 veces seguidas y le pega en cada una
    private void shadowCombo() {
        Player first = focusTarget();
        if (first == null) return;
        busy = true;

        run(new BukkitRunnable() {
            int hits = 0;

            @Override
            public void run() {
                if (!alive() || hits >= 3) {
                    cancel();
                    finish();
                    return;
                }
                Player target = hits == 0 ? first : focusTarget();
                if (target == null || !valid(target)) {
                    cancel();
                    finish();
                    return;
                }
                hits++;
                Vector back = target.getLocation().getDirection().setY(0);
                if (back.lengthSquared() < 0.01) back = new Vector(0, 0, 1);
                Location behind = target.getLocation().subtract(back.normalize().multiply(2.2));
                behind.setY(target.getLocation().getY());
                World world = king.getWorld();
                if (roomFor(behind)) {
                    world.spawnParticle(Particle.PORTAL, king.getLocation().add(0, 2, 0), 60, 0.6, 1.5, 0.6, 0.5);
                    teleportKing(behind);
                    world.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.6f);
                }
                if (king.getLocation().distance(target.getLocation()) <= 4.5) {
                    face(horizontalTo(king.getLocation(), target.getLocation()));
                    king.swingMainHand();
                    world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.6f);
                    world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0);
                    hurt(target, 20);
                    target.setVelocity(horizontalTo(king.getLocation(), target.getLocation()).multiply(0.5).setY(0.25));
                }
            }
        }, 0L, 12L);
    }

    // ---------------------------------------------------------------- Especiales

    // Lluvia Estelar: marca 3 círculos alrededor de cada jugador y 2 segundos después caen estrellas en ellos
    private void starRain() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        busy = true;
        World world = king.getWorld();
        world.playSound(king.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 3f, 0.6f);

        double radius = 2.5;
        List<Location> circles = new ArrayList<>();
        for (Player p : players) {
            for (int i = 0; i < (enraged() ? 4 : 3); i++) {
                Location base = i == 0 ? p.getLocation() : p.getLocation().add(random.nextDouble() * 10 - 5, 0, random.nextDouble() * 10 - 5);
                Location spot = ground(base);
                circles.add(spot != null ? spot : base);
            }
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
                if (t < 40) {
                    if (t % 3 != 0) return;
                    for (Location c : circles) {
                        circle(c, radius, WARNING_DUST);
                        double height = 14 * (1 - t / 40.0);
                        world.spawnParticle(Particle.END_ROD, c.clone().add(0, height + 0.5, 0), 4, 0.2, 0.4, 0.2, 0.01);
                    }
                    return;
                }

                cancel();
                Set<UUID> hit = new HashSet<>();
                for (Location c : circles) {
                    world.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
                    world.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2f, 0.6f);
                    world.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 2, 0.5, 0.3, 0.5, 0);
                    world.spawnParticle(Particle.END_ROD, c.clone().add(0, 0.5, 0), 30, 1, 0.5, 1, 0.15);
                    for (Player p : activePlayers()) {
                        if (hit.contains(p.getUniqueId())) continue;
                        Location loc = p.getLocation();
                        if (Math.hypot(loc.getX() - c.getX(), loc.getZ() - c.getZ()) > radius || Math.abs(loc.getY() - c.getY()) > 3) continue;
                        hit.add(p.getUniqueId());
                        hurt(p, 38);
                        p.setVelocity(p.getVelocity().setY(0.5));
                    }
                }
                finish();
            }
        }, 0L, 1L);
    }

    // Ráfaga Real: dispara 2 balas de shulker a cada jugador (hasta 10). Pegan 14 y hacen levitar menos que las normales
    private void royalVolley() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        busy = true;
        World world = king.getWorld();
        Location head = king.getLocation().add(0, 4.2, 0);
        world.playSound(head, Sound.ENTITY_SHULKER_SHOOT, 3f, 0.5f);
        world.spawnParticle(Particle.REVERSE_PORTAL, head, 60, 0.8, 0.8, 0.8, 0.05);

        int shot = 0;
        for (Player p : players) {
            for (int i = 0; i < 2 && shot < 10; i++, shot++) {
                double a = random.nextDouble() * Math.PI * 2;
                Location spot = head.clone().add(Math.cos(a) * 1.2, random.nextDouble(), Math.sin(a) * 1.2);
                BlockFace face = i == 0 ? BlockFace.UP : BlockFace.values()[random.nextInt(4)];
                ShulkerBullet bullet = world.spawn(spot, ShulkerBullet.class, b -> {
                    b.setShooter(king);
                    b.setTarget(p);
                    b.setCurrentMovementDirection(face);
                });
                royalBullets.add(bullet.getUniqueId());
            }
        }
        busy = false;
    }

    // Rayo del Vacío: apunta 1 segundo al jugador (se ve la línea), se queda quieto medio segundo y dispara un rayo
    // recto de 32 bloques que pega 46 y da Wither II
    private void voidRay() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;
        king.setAI(false);
        World world = king.getWorld();
        world.playSound(king.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 3f, 0.5f);

        run(new BukkitRunnable() {
            int t = 0;
            Location aim = target.getLocation().add(0, 1, 0);

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                Location eye = king.getLocation().add(0, 4, 0);
                if (t <= 20 && valid(target)) aim = target.getLocation().add(0, 1, 0);
                Vector dir = aim.toVector().subtract(eye.toVector());
                if (dir.lengthSquared() < 0.01) dir = new Vector(0, 0, 1);
                dir.normalize();
                face(dir.clone().setY(0));

                if (t < 30) {
                    if (t % 2 == 0) {
                        for (double d = 1; d <= 32; d += 1.5) {
                            world.spawnParticle(Particle.DUST, eye.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, t > 20 ? VOID_DUST : WARNING_DUST);
                        }
                    }
                    if (t == 20) world.playSound(eye, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 3f, 0.6f);
                    return;
                }

                cancel();
                world.playSound(eye, Sound.ENTITY_ENDER_DRAGON_SHOOT, 3f, 0.6f);
                world.playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1.4f);
                for (double d = 1; d <= 32; d += 0.6) {
                    Location point = eye.clone().add(dir.clone().multiply(d));
                    world.spawnParticle(Particle.REVERSE_PORTAL, point, 2, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.DUST, point, 1, 0.1, 0.1, 0.1, 0, VOID_DUST);
                }
                for (Player p : activePlayers()) {
                    if (distanceToRay(p.getLocation().add(0, 1, 0).toVector(), eye.toVector(), dir, 32) > 1.4) continue;
                    hurt(p, 46);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                }
                finish();
            }
        }, 0L, 1L);
    }

    private static double distanceToRay(Vector point, Vector origin, Vector dir, double length) {
        Vector rel = point.clone().subtract(origin);
        double along = Math.max(0, Math.min(length, rel.dot(dir)));
        return rel.subtract(dir.clone().multiply(along)).length();
    }

    // Agujero Negro: durante 3 segundos atrae a todos los que estén a 18 bloques y después explota (44 de daño a
    // 6 bloques). Corriendo para afuera se escapa
    private void blackHole() {
        if (activePlayers().isEmpty()) return;
        busy = true;
        king.setAI(false);
        World world = king.getWorld();
        Location center = king.getLocation().add(0, 1.5, 0);
        world.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 3f, 0.5f);

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
                if (t < 60) {
                    for (int i = 0; i < 3; i++) {
                        double a = (t * 0.35) + i * (2 * Math.PI / 3);
                        double r = 6 - (t % 20) * 0.25;
                        world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r), 2, 0.1, 0.1, 0.1, 0, VOID_DUST);
                    }
                    world.spawnParticle(Particle.PORTAL, center, 20, 4, 1.5, 4, 1.2);
                    if (t % 20 == 0) world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 3f, 0.5f);
                    if (t % 2 == 0) {
                        for (Player p : activePlayers()) {
                            Vector to = center.toVector().subtract(p.getLocation().toVector());
                            double d = to.length();
                            if (d > 18 || d < 1) continue;
                            double pull = 0.03 + 0.12 * (1 - d / 18);
                            Vector v = p.getVelocity().add(to.normalize().multiply(pull).setY(0));
                            if (v.clone().setY(0).length() > 0.6) v = v.clone().setY(0).normalize().multiply(0.6).setY(v.getY());
                            p.setVelocity(v);
                        }
                    }
                    return;
                }

                cancel();
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3f, 0.8f);
                world.spawnParticle(Particle.EXPLOSION, center, 6, 2, 1, 2, 0);
                world.spawnParticle(Particle.REVERSE_PORTAL, center, 150, 3, 1.5, 3, 0.3);
                for (Player p : activePlayers()) {
                    Vector away = p.getLocation().toVector().subtract(center.toVector());
                    if (away.length() > 6) continue;
                    away.setY(0);
                    Vector knock = away.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : away.normalize();
                    hurt(p, 44);
                    p.setVelocity(knock.multiply(1.1).setY(0.45));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                }
                finish();
            }
        }, 0L, 1L);
    }

    // Ejército del End: grita y salen Ender Spiders, un Ender Blaze y Ender Insects, hasta 4 vivos a la vez.
    // No sueltan nada y desaparecen cuando muere el rey
    private void endArmy() {
        busy = true;
        World world = king.getWorld();
        world.playSound(king.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 3f, 0.4f);
        world.playSound(king.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3f, 0.6f);

        int amount = Math.min(MAX_MINIONS - minions.size(), 2 + random.nextInt(2));
        List<Player> players = activePlayers();
        for (int i = 0; i < amount; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Location base = king.getLocation().add(Math.cos(a) * (3 + random.nextDouble() * 3), 0, Math.sin(a) * (3 + random.nextDouble() * 3));
            Location spot = ground(base);
            if (spot == null) continue;
            world.spawnParticle(Particle.REVERSE_PORTAL, spot.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
            LivingEntity minion = switch (i) {
                case 0 -> spiderFactory.spawnEnderSpider(spot);
                case 1 -> EnderInsect.spawn(plugin, spot);
                case 2 -> blazeFactory.spawnEnderBlaze(spot.clone().add(0, 1.5, 0));
                default -> EnderInsect.spawn(plugin, spot);
            };
            minion.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);
            minion.setPersistent(false);
            if (minion instanceof Mob mob && !players.isEmpty()) mob.setTarget(players.get(random.nextInt(players.size())));
            minions.add(minion.getUniqueId());
        }
        busy = false;
    }

    // Grieta Dimensional: cambia de lugar con el jugador más lejano, que queda mareado y a oscuras, y medio segundo
    // después golpea el suelo donde apareció
    private void dimensionalRift() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        Player target = players.get(players.size() - 1);
        Location kingSpot = king.getLocation();
        Location playerSpot = target.getLocation();
        if (!roomFor(playerSpot)) {
            voidRay();
            return;
        }
        busy = true;
        World world = king.getWorld();
        world.spawnParticle(Particle.PORTAL, kingSpot.clone().add(0, 2, 0), 80, 0.6, 1.5, 0.6, 0.6);
        world.spawnParticle(Particle.PORTAL, playerSpot.clone().add(0, 1, 0), 80, 0.4, 1, 0.4, 0.6);
        world.playSound(kingSpot, Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.5f);
        world.playSound(playerSpot, Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.5f);

        teleportKing(playerSpot);
        target.teleport(kingSpot.setDirection(playerSpot.toVector().subtract(kingSpot.toVector())));
        hurt(target, 20);
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
        setFocus(target);

        run(new BukkitRunnable() {
            @Override
            public void run() {
                cancel();
                if (alive()) shockwave(king.getLocation(), 4.5, 26, 0.5);
                finish();
            }
        }, 10L, 1L);
    }

    // ---------------------------------------------------------------- Regeneración

    // Trono del Vacío: vuelve sobre el altar, flota y levanta 4 cristales que lo curan mientras estén vivos. Recibe la
    // mitad de daño hasta que se rompan los 4 (un golpe o una flecha cada uno) o pasen 30 segundos
    private void startRegeneration() {
        regenerating = true;
        regenTicks = 0;
        king.getPersistentDataContainer().set(regensKey, PersistentDataType.INTEGER, regens() + 1);
        king.setTarget(null);
        king.setAI(false);
        king.setGravity(false);
        Location throne = spawnLocation.clone().add(0, 3, 0);
        teleportKing(throne);

        World world = king.getWorld();
        world.playSound(throne, Sound.ENTITY_ENDER_DRAGON_GROWL, 4f, 0.6f);
        world.playSound(throne, Sound.BLOCK_END_PORTAL_SPAWN, 2f, 1.2f);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            Location base = spawnLocation.clone().add(Math.cos(a) * 10, 0, Math.sin(a) * 10);
            Location floor = ground(base);
            Location spot = (floor != null ? floor : base).add(0, 2.5, 0);
            EnderCrystal crystal = world.spawn(spot, EnderCrystal.class, c -> {
                c.setShowingBottom(false);
                c.setBeamTarget(throne.clone().add(0, 2, 0));
                c.setPersistent(false);
            });
            crystals.add(crystal);
            world.spawnParticle(Particle.END_ROD, spot, 30, 0.3, 0.6, 0.3, 0.05);
        }

        for (Player p : activePlayers()) {
            p.sendTitle("", ChatColor.of("#D36BFF") + "¡El Rey Ender se regenera! Rompe los cristales", 5, 50, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        }
    }

    private void regenTick() {
        regenTicks++;
        crystals.removeIf(c -> !c.isValid());
        if (king.hasAI()) king.setAI(false);
        World world = king.getWorld();
        Location heart = king.getLocation().add(0, 2.5, 0);

        if (regenTicks % 20 == 0 && !crystals.isEmpty()) {
            double heal = crystals.size() * REGEN_PER_CRYSTAL / SCALE;
            king.setHealth(Math.min(REAL_HEALTH, king.getHealth() + heal));
            world.spawnParticle(Particle.HEART, heart, crystals.size(), 0.8, 0.8, 0.8, 0);
            world.playSound(heart, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
        }
        if (regenTicks % 4 == 0) {
            for (EnderCrystal c : crystals) {
                Vector to = heart.toVector().subtract(c.getLocation().toVector());
                double length = to.length();
                to.normalize();
                for (double d = 0; d < length; d += 1.5) {
                    world.spawnParticle(Particle.END_ROD, c.getLocation().add(to.clone().multiply(d)), 1, 0, 0, 0, 0);
                }
            }
        }
        // Cada 4 segundos empuja a los que se pegan al trono
        if (regenTicks % 80 == 0) {
            world.playSound(heart, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.6f);
            world.spawnParticle(Particle.REVERSE_PORTAL, heart, 80, 2, 1, 2, 0.2);
            for (Player p : activePlayers()) {
                Vector away = p.getLocation().toVector().subtract(heart.toVector());
                if (away.length() > 6) continue;
                away.setY(0);
                Vector knock = away.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : away.normalize();
                hurt(p, 14);
                p.setVelocity(knock.multiply(1.0).setY(0.4));
            }
        }
        if (crystals.isEmpty() || regenTicks >= REGEN_TICKS) endRegeneration();
    }

    private void endRegeneration() {
        regenerating = false;
        crystals.forEach(Entity::remove);
        crystals.clear();
        king.setGravity(true);
        if (!isHibernating()) king.setAI(true);
        World world = king.getWorld();
        world.playSound(king.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 3f, 0.5f);
        world.spawnParticle(Particle.REVERSE_PORTAL, king.getLocation().add(0, 2.5, 0), 120, 1, 2, 1, 0.2);
    }

    // ---------------------------------------------------------------- Eventos

    // El daño que recibe se divide entre 7 (su vida es de 7000). Las armas de Celestita pegan 50% más y mientras se
    // regenera recibe la mitad. No se cae, no se ahoga y lo que invoca no le pega
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKingDamage(EntityDamageEvent event) {
        if (!event.getEntity().equals(king)) return;
        switch (event.getCause()) {
            case FALL, DROWNING, SUFFOCATION, FLY_INTO_WALL, CRAMMING, FIRE, FIRE_TICK, LAVA -> {
                event.setCancelled(true);
                return;
            }
            case VOID -> {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, this::returnToArena);
                return;
            }
            default -> { }
        }

        double damage = event.getDamage();
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            Player attacker = attacker(damager);
            if (attacker != null) {
                addAttacker(attacker);
                if (damager.equals(attacker) && EndItems.isCelestiteWeapon(attacker.getInventory().getItemInMainHand())) {
                    damage *= CELESTITE_BONUS;
                }
            } else if (minions.contains(damager.getUniqueId()) || damager instanceof ShulkerBullet || isMinionShot(damager)) {
                event.setCancelled(true);
                return;
            }
        }
        if (regenerating) damage *= 0.5;
        event.setDamage(damage / SCALE);
    }

    private boolean isMinionShot(Entity damager) {
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                && minions.contains(shooter.getUniqueId());
    }

    // Como enderman las flechas no le harían nada: se cambian por su daño. El tridente rebota como siempre
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrow(ProjectileHitEvent event) {
        if (!king.equals(event.getHitEntity()) || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        double damage = arrow instanceof Trident ? 8 : Math.ceil(arrow.getDamage() * arrow.getVelocity().length());
        if (!(arrow instanceof Trident)) arrow.remove();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (king.isValid() && !king.isDead()) king.damage(damage, shooter);
        });
    }

    // Balas de la Ráfaga Real: 14 de daño y levitación corta
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRoyalBullet(EntityDamageByEntityEvent event) {
        if (!royalBullets.remove(event.getDamager().getUniqueId()) || !(event.getEntity() instanceof Player player)) return;
        event.setDamage(14);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1));
        });
    }

    // Los cristales del trono solo los rompen los jugadores y no explotan
    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal) || !crystals.contains(crystal)) return;
        event.setCancelled(true);
        if (!(event instanceof EntityDamageByEntityEvent byEntity) || attacker(byEntity.getDamager()) == null) return;
        World world = crystal.getWorld();
        Location loc = crystal.getLocation();
        crystal.remove();
        crystals.remove(crystal);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 2f, 0.6f);
        world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 2f, 0.6f);
        world.spawnParticle(Particle.END_ROD, loc, 40, 0.5, 0.5, 0.5, 0.15);
        for (Player p : activePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.of("#D36BFF") + "Cristal roto: quedan " + crystals.size()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity().equals(king) && !selfTeleport) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEscape(EndermanEscapeEvent event) {
        if (event.getEntity().equals(king)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlock(EntityChangeBlockEvent event) {
        if (event.getEntity().equals(king)) event.setCancelled(true);
    }

    // Solo pelea con jugadores, y sus invocados no se pelean entre ellos
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (event.getEntity().equals(king) && event.getTarget() != null && !(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        } else if (minions.contains(event.getEntity().getUniqueId()) && event.getTarget() != null
                && !(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) return p;
        return null;
    }

    // Suelta la EnderKing Pearl (para el Peto de Warden Alado) y mucha experiencia; las DinoCoins las da BossRewards
    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(EntityDeathEvent event) {
        if (!event.getEntity().equals(king)) return;
        event.getDrops().clear();
        event.getDrops().add(EndItems.createEnderKingPearl());
        event.setDroppedExp(3000);
        World world = king.getWorld();
        Location loc = king.getLocation().add(0, 2.5, 0);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 4f, 0.8f);
        world.spawnParticle(Particle.REVERSE_PORTAL, loc, 300, 2, 3, 2, 0.3);
        world.spawnParticle(Particle.END_ROD, loc, 120, 2, 3, 2, 0.2);
    }
}
