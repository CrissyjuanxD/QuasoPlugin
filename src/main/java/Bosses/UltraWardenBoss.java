package Bosses;

import Dificultades.CustomMobs.InfestedSkeleton;
import Dificultades.CustomMobs.WardenZombie;
import io.papermc.paper.event.entity.WardenAngerChangeEvent;
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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// Ultra Warden: el boss de la Warden Cave. Se invoca con un Corazón de Warden en su altar (UltraWardenAltar).
// Pelea como el Rey Ender: de 1 a 3 ataques cuerpo a cuerpo (Embestida Sónica, Zarpazo Doble y Puño Sísmico) y
// después uno de sus 5 especiales (Silencio Profundo, Esfera Resonante, Lluvia Sónica, Llamado de la Infestación y
// Cacería en la Oscuridad). Al bajar del 50% y del 25% se regenera con el Corazón de Sculk: 4 catalizadores que hay
// que romper. Tiene 2000 de vida (el mob tiene 1000 y el daño que recibe se divide entre 2). Cada especial se avisa
// en el chat. El daño está pensado para la armadura floral con Protección IV
public class UltraWardenBoss extends BaseBoss implements Listener {

    public static final String BOSS_ID = "ultra_warden";
    public static final Map<UUID, UltraWardenBoss> ACTIVE_BOSSES = new HashMap<>();

    public static final double HEALTH = 2000;
    private static final double REAL_HEALTH = 1000;
    private static final double SCALE = HEALTH / REAL_HEALTH;
    private static final double ATTACK_DAMAGE = 20;
    private static final double SIZE = 1.6;
    private static final int MAX_MINIONS = 5;
    private static final double[] REGEN_AT = {0.5, 0.25};
    private static final int REGEN_TICKS = 600;
    // Vida por segundo que cura cada catalizador (en la vida de 2000)
    private static final double REGEN_PER_CATALYST = 5;
    private static final int EMERGE_TICKS = 120;
    private static final String TITLE = ChatColor.of("#29DFEB") + "" + ChatColor.BOLD + "Ultra Warden";

    private static final Color CIAN = Color.fromRGB(0x29DFEB);
    private static final Particle.DustOptions SCULK_DUST = new Particle.DustOptions(Color.fromRGB(0x0BB5B5), 1.6f);
    private static final Particle.DustOptions LIGHT_DUST = new Particle.DustOptions(CIAN, 1.2f);
    private static final Particle.DustOptions DARK_DUST = new Particle.DustOptions(Color.fromRGB(0x0A3D4A), 1.8f);
    private static final Particle.DustOptions DANGER_DUST = new Particle.DustOptions(Color.fromRGB(0xE8FBFF), 1.4f);

    private final Warden warden;
    private final Random random = new Random();
    private final WardenZombie zombieFactory;
    private final InfestedSkeleton skeletonFactory;
    private final List<UUID> minions = new ArrayList<>();
    private final List<Catalizador> catalysts = new ArrayList<>();
    private final List<Pilar> pillars = new ArrayList<>();
    private final Map<UUID, Integer> pillarHits = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    private final NamespacedKey arenaX;
    private final NamespacedKey arenaY;
    private final NamespacedKey arenaZ;
    private final NamespacedKey regensKey;

    private int globalTick = 0;
    private boolean busy = false;
    private boolean regenerating = false;
    private boolean customBoom = false;
    private int regenTicks = 0;
    private int stunnedUntil = 0;
    private int emergingUntil = 0;
    private int meleeSinceSpecial = 0;
    private int meleeBeforeSpecial = 2;
    private int lastSpecial = -1;
    private UUID focus;
    private int focusUntil = 0;
    private BukkitTask pillarTask;

    // Si el warden ya era boss (chunk recargado) recupera el centro de la arena de su PDC
    public UltraWardenBoss(JavaPlugin plugin, Warden warden) {
        super(plugin, warden);
        this.warden = warden;
        this.zombieFactory = new WardenZombie(plugin);
        this.skeletonFactory = new InfestedSkeleton(plugin);
        this.arenaX = new NamespacedKey(plugin, "arena_x");
        this.arenaY = new NamespacedKey(plugin, "arena_y");
        this.arenaZ = new NamespacedKey(plugin, "arena_z");
        this.regensKey = new NamespacedKey(plugin, "ultra_warden_regens");

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
        // Si se descargó a mitad de la Cacería quedaba invisible, o a mitad de emerger
        warden.setInvisible(false);
        if (warden.getPose() == Pose.EMERGING) warden.setPose(Pose.STANDING, false);
        warden.setCustomNameVisible(true);

        ACTIVE_BOSSES.put(warden.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        start();
    }

    // Sale del suelo con la animación de emerger del warden (6 segundos en los que no recibe daño)
    public static UltraWardenBoss spawn(JavaPlugin plugin, Location location) {
        NamespacedKey bossIdKey = new NamespacedKey(plugin, "boss_id");
        Warden warden = location.getWorld().spawn(location, Warden.class, w -> {
            w.setCustomName(TITLE);
            w.setCustomNameVisible(true);
            w.setRemoveWhenFarAway(false);
            w.setPersistent(true);
            setAttribute(w.getAttribute(Attribute.MAX_HEALTH), REAL_HEALTH);
            w.setHealth(REAL_HEALTH);
            setAttribute(w.getAttribute(Attribute.ATTACK_DAMAGE), ATTACK_DAMAGE);
            setAttribute(w.getAttribute(Attribute.KNOCKBACK_RESISTANCE), 1);
            setAttribute(w.getAttribute(Attribute.FOLLOW_RANGE), 48);
            setAttribute(w.getAttribute(Attribute.SCALE), SIZE);
            w.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, BOSS_ID);
        });

        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_WARDEN_EMERGE, 4f, 0.6f);
        world.playSound(location, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 4f, 0.5f);
        world.spawnParticle(Particle.SCULK_SOUL, location.clone().add(0, 1, 0), 60, 1.6, 1.5, 1.6, 0.06);
        world.spawnParticle(Particle.BLOCK, location, 120, 2, 0.3, 2, 0, sculk());
        UltraWardenBoss boss = new UltraWardenBoss(plugin, warden);
        boss.emerge();
        return boss;
    }

    // Se crea al usarlo: como constante no deja cargar la clase sin servidor (tests)
    private static BlockData sculk() {
        return Material.SCULK.createBlockData();
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
        return BarColor.BLUE;
    }

    @Override
    protected double healthScale() {
        return SCALE;
    }

    // La arena es el templo del altar
    @Override
    protected int getArenaRadius() {
        return 34;
    }

    @Override
    protected int getArenaHeightUp() {
        return 24;
    }

    @Override
    protected int getArenaHeightDown() {
        return 12;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.CIRCULAR;
    }

    @Override
    protected void onStart() {
        meleeBeforeSpecial = random.nextInt(3) + 1;
    }

    // Cada segundo se enoja con su jugador y se olvida del resto (así no se mete bajo tierra); cada 50 ticks (35 con
    // menos del 25% de vida) elige el siguiente ataque y al cruzar el 50% y el 25% se regenera
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
            for (Player p : warden.getWorld().getPlayers()) {
                if (target != null && p.equals(target) && !regenerating) warden.setAnger(p, 150);
                else if (warden.getAnger(p) > 0) warden.clearAnger(p);
            }
        }
        if (globalTick % 5 == 0) ambient();
        if (globalTick < emergingUntil) return;
        if (globalTick == emergingUntil) endEmerge();

        if (regenerating) {
            regenTick();
            return;
        }
        if (busy) return;
        if (shouldRegenerate()) {
            startRegeneration();
            return;
        }
        int delay = ratio() < 0.25 ? 35 : 50;
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
        freezeMinions(false);
        cleanup();
    }

    private void cleanup() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        if (pillarTask != null) pillarTask.cancel();
        pillars.clear();
        catalysts.forEach(Catalizador::remove);
        catalysts.clear();
        HandlerList.unregisterAll(this);
        ACTIVE_BOSSES.remove(warden.getUniqueId());
    }

    private double ratio() {
        return warden.getHealth() / REAL_HEALTH;
    }

    private boolean enraged() {
        return ratio() < 0.5;
    }

    private boolean alive() {
        return warden.isValid() && !warden.isDead() && !isHibernating();
    }

    private int regens() {
        return warden.getPersistentDataContainer().getOrDefault(regensKey, PersistentDataType.INTEGER, 0);
    }

    private boolean shouldRegenerate() {
        int done = regens();
        return done < REGEN_AT.length && ratio() < REGEN_AT[done];
    }

    // Almas de sculk que suben por el cuerpo y un anillo oscuro a los pies; enojado suelta chispas
    private void ambient() {
        World world = warden.getWorld();
        Location loc = warden.getLocation();
        if (warden.isInvisible()) return;
        world.spawnParticle(Particle.SCULK_SOUL, loc.clone().add(0, 2.5, 0), 2, 0.7, 1.4, 0.7, 0.02);
        double a = globalTick * 0.15;
        for (int i = 0; i < 3; i++) {
            double angle = a + i * 2 * Math.PI / 3;
            world.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(angle) * 1.6, 0.15, Math.sin(angle) * 1.6), 1, 0, 0, 0, 0, SCULK_DUST);
        }
        if (enraged()) world.spawnParticle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 3.2, 0), 3, 0.5, 0.8, 0.5, 0.01);
    }

    // Emerge del suelo con la animación del warden; mientras tanto no se mueve ni recibe daño
    private void emerge() {
        emergingUntil = globalTick + EMERGE_TICKS;
        busy = true;
        warden.setAI(false);
        warden.setPose(Pose.EMERGING, true);
        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!warden.isValid() || ++t > EMERGE_TICKS) {
                    cancel();
                    return;
                }
                Location loc = warden.getLocation();
                warden.getWorld().spawnParticle(Particle.BLOCK, loc, 10, 1.4, 0.1, 1.4, 0, sculk());
                if (t % 4 == 0) warden.getWorld().spawnParticle(Particle.SCULK_SOUL, loc.clone().add(0, 0.5, 0), 4, 1.2, 0.3, 1.2, 0.04);
                if (t % 30 == 0) warden.getWorld().playSound(loc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 3f, 0.6f);
            }
        }, 0L, 1L);
    }

    private void endEmerge() {
        warden.setPose(Pose.STANDING, false);
        World world = warden.getWorld();
        Location loc = warden.getLocation();
        world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 4f, 0.7f);
        world.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 2.5, 0), 1);
        ring(loc.clone().add(0, 0.2, 0), 4, 40, SCULK_DUST);
        finish();
    }

    private List<Player> activePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(warden.getWorld()) && !p.isDead()
                    && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                list.add(p);
            }
        }
        list.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(warden.getLocation())));
        return list;
    }

    // A todos los de la arena (también espectadores), como la Abeja Floral
    private List<Player> arenaPlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(warden.getWorld())) list.add(p);
        }
        return list;
    }

    // Avisa en el chat el especial que viene
    private void anunciar(String ataque) {
        String mensaje = ChatColor.of("#0BB5B5") + "۞ " + TITLE + ChatColor.of("#0BB5B5") + " » " + ChatColor.of("#B8F4F7") + ataque;
        for (Player p : arenaPlayers()) p.sendMessage(mensaje);
    }

    private void titulo(String titulo, String subtitulo, int stay) {
        for (Player p : arenaPlayers()) p.sendTitle(titulo, subtitulo, 4, stay, 8);
    }

    private void actionBar(Player p, String text) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
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
        return target.isValid() && !target.isDead() && target.getWorld().equals(warden.getWorld());
    }

    private void decideNextAttack() {
        if (nearestPlayer() == null) return;

        if (meleeSinceSpecial < meleeBeforeSpecial) {
            meleeSinceSpecial++;
            switch (random.nextInt(3)) {
                case 0 -> sonicCharge();
                case 1 -> doubleClaw();
                default -> seismicFist();
            }
            return;
        }

        meleeSinceSpecial = 0;
        meleeBeforeSpecial = enraged() ? random.nextInt(2) + 1 : random.nextInt(3) + 1;
        int special;
        int tries = 0;
        do {
            special = random.nextInt(5);
        } while (++tries < 20 && (special == lastSpecial || (special == 2 && !pillars.isEmpty())
                || (special == 3 && minions.size() >= MAX_MINIONS)));
        lastSpecial = special;
        switch (special) {
            case 0 -> deepSilence();
            case 1 -> resonantSphere();
            case 2 -> sonicRain();
            case 3 -> infestationCall();
            default -> darkHunt();
        }
    }

    private void run(BukkitRunnable runnable, long delay, long period) {
        tasks.removeIf(BukkitTask::isCancelled);
        tasks.add(runnable.runTaskTimer(plugin, delay, period));
    }

    private void finish() {
        busy = false;
        if (warden.isValid() && !isHibernating() && !regenerating && globalTick >= emergingUntil) warden.setAI(true);
    }

    private Vector horizontalTo(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector()).setY(0);
        return dir.lengthSquared() < 0.01 ? new Vector(0, 0, 1) : dir.normalize();
    }

    private void face(Vector dir) {
        if (dir.lengthSquared() < 0.01) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        warden.setRotation(yaw, 0);
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

    // El juego escala el daño sónico con la dificultad (en Fácil la mitad + 1, en Difícil 1.5 veces): este es el daño
    // que hay que pedir para que llegue justo "objetivo"
    static double sinDificultad(org.bukkit.Difficulty difficulty, double objetivo) {
        return switch (difficulty) {
            case EASY -> Math.max(objetivo, (objetivo - 1) * 2);
            case HARD -> objetivo / 1.5;
            default -> objetivo;
        };
    }

    // Mide unos 4.7 bloques: hace falta ese espacio libre y suelo firme
    private boolean roomFor(Location feet) {
        for (int h = 0; h < 5; h++) {
            Block b = feet.clone().add(0, h, 0).getBlock();
            if (!b.isPassable() || b.isLiquid()) return false;
        }
        return feet.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }

    // Un lugar con suelo firme y 3 bloques libres a esa distancia, dentro de la arena
    private Location groundNear(Location from, double distance) {
        double a = random.nextDouble() * Math.PI * 2;
        Location base = from.clone().add(Math.cos(a) * distance, 0, Math.sin(a) * distance);
        if (!areaZone.isInside(base)) return null;
        return ground(base, from.getBlockY());
    }

    private Location ground(Location base, int y) {
        World world = base.getWorld();
        for (int dy = 4; dy >= -6; dy--) {
            Block feet = world.getBlockAt(base.getBlockX(), y + dy, base.getBlockZ());
            if (!feet.getRelative(0, -1, 0).getType().isSolid()) continue;
            if (feet.isPassable() && !feet.isLiquid()
                    && feet.getRelative(0, 1, 0).isPassable() && feet.getRelative(0, 2, 0).isPassable()) {
                return feet.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Partículas

    private void ring(Location center, double radius, int points, Particle.DustOptions dust) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 1, 0, 0, 0, 0, dust);
        }
    }

    private void sphere(Location center, double radius, double step, Particle.DustOptions dust) {
        World world = center.getWorld();
        for (double phi = 0; phi <= Math.PI; phi += step) {
            double y = radius * Math.cos(phi);
            double r = radius * Math.sin(phi);
            int points = Math.max(1, (int) Math.round(2 * Math.PI * r / (radius * step)));
            for (int i = 0; i < points; i++) {
                double theta = 2 * Math.PI * i / points + phi;
                world.spawnParticle(Particle.DUST, center.clone().add(r * Math.cos(theta), y, r * Math.sin(theta)), 1, 0, 0, 0, 0, dust);
            }
        }
    }

    // Línea de partículas de un punto a otro
    private void line(Location from, Location to, double spacing, Particle.DustOptions dust) {
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length < 0.01) return;
        dir.normalize();
        for (double d = 0; d <= length; d += spacing) {
            from.getWorld().spawnParticle(Particle.DUST, from.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, dust);
        }
    }

    // Teleport con aviso: una esfera donde está, un anillo que se cierra donde va a aparecer y un arco de almas que
    // une los dos puntos; medio segundo después aparece con un sonic boom
    private void teleportWithVisual(Location to, Runnable after) {
        World world = warden.getWorld();
        Location from = warden.getLocation();
        sphere(from.clone().add(0, 2.2, 0), 3, Math.PI / 9, SCULK_DUST);
        world.spawnParticle(Particle.SCULK_SOUL, from.clone().add(0, 2, 0), 25, 1.2, 1.6, 1.2, 0.04);
        world.playSound(from, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.5f, 1.3f);
        world.playSound(to, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 2f, 1.6f);

        run(new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t <= 10) {
                    double radius = 3.2 - t * 0.27;
                    ring(to.clone().add(0, 0.15, 0), radius, 24, LIGHT_DUST);
                    ring(to.clone().add(0, 0.15 + t * 0.3, 0), radius * 0.6, 12, SCULK_DUST);
                    // El arco se dibuja de a poco desde el boss hasta el destino
                    double progress = t / 10.0;
                    for (double f = Math.max(0, progress - 0.2); f <= progress; f += 0.04) {
                        Vector straight = to.toVector().subtract(from.toVector()).multiply(f);
                        double height = 2.5 + Math.sin(Math.PI * f) * 4;
                        Location point = from.clone().add(straight).add(0, height, 0);
                        world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, LIGHT_DUST);
                        world.spawnParticle(Particle.SCULK_SOUL, point, 1, 0.05, 0.05, 0.05, 0);
                    }
                    return;
                }
                cancel();
                to.setYaw(warden.getLocation().getYaw());
                warden.teleport(to);
                world.playSound(to, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.6f);
                world.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.5f);
                world.spawnParticle(Particle.SONIC_BOOM, to.clone().add(0, 2.2, 0), 1);
                world.spawnParticle(Particle.SCULK_SOUL, to.clone().add(0, 1.5, 0), 25, 1, 1.2, 1, 0.06);
                ring(to.clone().add(0, 0.2, 0), 3.5, 36, SCULK_DUST);
                if (after != null) after.run();
            }
        }, 0L, 1L);
    }

    // Si se sale de la arena vuelve al centro con el mismo efecto
    @Override
    protected void returnToArena() {
        if (busy || regenerating) return;
        busy = true;
        teleportWithVisual(spawnLocation.clone(), this::finish);
    }

    // ---------------------------------------------------------------- Cuerpo a cuerpo

    // Embestida Sónica: marca en el suelo el camino hacia su jugador y sale disparado 16 bloques; todo lo que está en
    // el camino recibe el golpe y sale volando hacia arriba
    private void sonicCharge() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;
        warden.setAI(false);

        World world = warden.getWorld();
        Vector dir = horizontalTo(warden.getLocation(), target.getLocation());
        face(dir);
        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 3f, 0.7f);
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 3f, 0.6f);

        run(new BukkitRunnable() {
            int t = 0;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                Location start = warden.getLocation();
                if (t <= 16) {
                    if (t % 2 == 0) {
                        for (double d = 1; d <= 16; d += 0.8) {
                            Location point = start.clone().add(dir.clone().multiply(d)).add(0, 0.15, 0);
                            Vector side = new Vector(-dir.getZ(), 0, dir.getX()).multiply(1.4);
                            world.spawnParticle(Particle.DUST, point.clone().add(side), 1, 0, 0, 0, 0, LIGHT_DUST);
                            world.spawnParticle(Particle.DUST, point.clone().subtract(side), 1, 0, 0, 0, 0, LIGHT_DUST);
                        }
                    }
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, start.clone().add(0, 1, 0), 4, 0.8, 0.8, 0.8, 0.02);
                    return;
                }
                if (t == 17) world.playSound(start, Sound.ENTITY_WARDEN_ROAR, 3f, 1.4f);

                // 12 ticks de embestida, 1.35 bloques por tick; frena si choca contra una pared
                Location next = start.clone().add(dir.clone().multiply(1.35));
                next.setDirection(dir);
                boolean blocked = !next.clone().add(0, 0.5, 0).getBlock().isPassable()
                        || !next.clone().add(0, 1.5, 0).getBlock().isPassable();
                if (!blocked) {
                    Location floor = ground(next, next.getBlockY());
                    if (floor != null) {
                        floor.setDirection(dir);
                        warden.teleport(floor);
                    } else {
                        blocked = true;
                    }
                }
                Location now = warden.getLocation();
                world.spawnParticle(Particle.CLOUD, now.clone().add(0, 0.3, 0), 6, 0.6, 0.1, 0.6, 0.02);
                world.spawnParticle(Particle.SCULK_SOUL, now.clone().add(0, 2, 0), 4, 0.8, 1, 0.8, 0.02);
                if (t % 3 == 0) world.spawnParticle(Particle.SONIC_BOOM, now.clone().add(0, 2.2, 0), 1);

                for (Entity e : world.getNearbyEntities(now.clone().add(0, 1.5, 0), 2.6, 3, 2.6)) {
                    if (!(e instanceof LivingEntity victim) || e.equals(warden) || minions.contains(e.getUniqueId())) continue;
                    if (!hit.add(e.getUniqueId())) continue;
                    if (victim instanceof Player p) {
                        if (!activePlayers().contains(p)) continue;
                        p.damage(24, warden);
                    } else {
                        victim.damage(10, warden);
                    }
                    victim.setVelocity(dir.clone().multiply(0.6).setY(1.35));
                    world.spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 1);
                    world.playSound(victim.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2f, 0.8f);
                }

                if (blocked || t >= 28) {
                    cancel();
                    if (blocked) {
                        world.playSound(now, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                        world.spawnParticle(Particle.BLOCK, now, 60, 1.5, 0.5, 1.5, 0, sculk());
                    }
                    finish();
                }
            }
        }, 0L, 1L);
    }

    // Zarpazo Doble: se acerca y barre dos veces delante suyo, la segunda más lejos y con oscuridad
    private void doubleClaw() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;

        run(new BukkitRunnable() {
            int t = 0;
            int phase = 0;

            @Override
            public void run() {
                if (!alive() || !valid(target)) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (phase == 0) {
                    double dist = warden.getLocation().distance(target.getLocation());
                    if (dist > 4 && t < 30) {
                        if (t % 5 == 1) warden.setVelocity(horizontalTo(warden.getLocation(), target.getLocation()).multiply(1.0).setY(0.15));
                        return;
                    }
                    warden.setAI(false);
                    claw(horizontalTo(warden.getLocation(), target.getLocation()), 5, 18, false);
                    phase = 1;
                    t = 0;
                    return;
                }
                if (t < 10) return;
                cancel();
                claw(horizontalTo(warden.getLocation(), target.getLocation()), 6.5, 18, true);
                finish();
            }
        }, 0L, 1L);
    }

    private void claw(Vector dir, double reach, double damage, boolean second) {
        World world = warden.getWorld();
        Location origin = warden.getLocation().add(0, 1.8, 0);
        face(dir);
        warden.playEffect(EntityEffect.WARDEN_ATTACK);
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, second ? 0.4f : 0.6f);
        world.playSound(origin, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2f, second ? 0.6f : 0.9f);

        // El arco va inclinado: el primero de derecha a izquierda y el segundo al revés
        for (int angle = -70; angle <= 70; angle += 10) {
            Vector arc = dir.clone().rotateAroundY(Math.toRadians(angle));
            double tilt = (second ? -angle : angle) / 70.0 * 0.9;
            for (double r = 1.5; r <= reach; r += 1) {
                Location point = origin.clone().add(arc.clone().multiply(r)).add(0, tilt, 0);
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, r > reach - 1.2 ? LIGHT_DUST : SCULK_DUST);
                if (angle % 20 == 0) world.spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
            }
        }
        world.spawnParticle(Particle.SCULK_CHARGE_POP, origin.clone().add(dir.clone().multiply(2)), 12, 1, 0.5, 1, 0.02);

        for (Player p : activePlayers()) {
            Vector to = p.getLocation().toVector().subtract(warden.getLocation().toVector());
            double dy = Math.abs(to.getY());
            to.setY(0);
            if (to.length() > reach + 0.5 || dy > 3.5) continue;
            if (to.lengthSquared() > 0.01 && Math.toDegrees(dir.angle(to.clone().normalize())) > 75) continue;
            p.damage(damage, warden);
            p.setVelocity(dir.clone().multiply(second ? 1.4 : 0.8).setY(0.35));
            if (second) p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
        }
    }

    // Puño Sísmico: golpea el suelo y una grieta de sculk corre 15 bloques hacia su jugador; el que está cerca del
    // puño o sobre la grieta sale volando
    private void seismicFist() {
        Player target = focusTarget();
        if (target == null) return;
        busy = true;
        warden.setAI(false);

        World world = warden.getWorld();
        Vector dir = horizontalTo(warden.getLocation(), target.getLocation());
        face(dir);
        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_AGITATED, 3f, 0.6f);

        run(new BukkitRunnable() {
            int t = 0;
            Location center;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t < 12) {
                    world.spawnParticle(Particle.SCULK_SOUL, warden.getLocation().add(dir.clone().multiply(2)).add(0, 3, 0), 3, 0.3, 0.3, 0.3, 0.01);
                    return;
                }
                if (t == 12) {
                    center = warden.getLocation();
                    warden.playEffect(EntityEffect.WARDEN_ATTACK);
                    world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 3f, 0.5f);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                    world.spawnParticle(Particle.BLOCK, center, 80, 2.5, 0.2, 2.5, 0, sculk());
                    ring(center.clone().add(0, 0.2, 0), 3, 30, LIGHT_DUST);
                    for (Player p : activePlayers()) {
                        Vector away = p.getLocation().toVector().subtract(center.toVector());
                        if (away.length() > 3.5 || Math.abs(away.getY()) > 3) continue;
                        hit.add(p.getUniqueId());
                        p.damage(16, warden);
                        p.setVelocity(away.setY(0).normalize().multiply(0.5).setY(0.8));
                    }
                    return;
                }
                // La grieta avanza un bloque por tick
                double d = t - 12 + 2;
                Location point = center.clone().add(dir.clone().multiply(d));
                Location floor = ground(point, point.getBlockY());
                if (floor == null || d > 16) {
                    cancel();
                    finish();
                    return;
                }
                world.spawnParticle(Particle.BLOCK, floor, 25, 0.5, 0.2, 0.5, 0, sculk());
                world.spawnParticle(Particle.SCULK_CHARGE_POP, floor.clone().add(0, 0.3, 0), 6, 0.4, 0.3, 0.4, 0.03);
                for (double y = 0.3; y <= 2.4; y += 0.7) {
                    world.spawnParticle(Particle.DUST, floor.clone().add(0, y, 0), 2, 0.25, 0.1, 0.25, 0, SCULK_DUST);
                }
                if (t % 2 == 0) world.playSound(floor, Sound.BLOCK_SCULK_SPREAD, 2f, 0.6f);
                for (Player p : activePlayers()) {
                    if (hit.contains(p.getUniqueId())) continue;
                    Location loc = p.getLocation();
                    if (Math.hypot(loc.getX() - floor.getX(), loc.getZ() - floor.getZ()) > 1.6 || Math.abs(loc.getY() - floor.getY()) > 2.5) continue;
                    hit.add(p.getUniqueId());
                    p.damage(22, warden);
                    p.setVelocity(new Vector(0, 1.0, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
            }
        }, 0L, 1L);
    }

    // ---------------------------------------------------------------- Especiales

    // Silencio Profundo: avisa con un título y a los 3 segundos empieza el silencio. Durante 8 segundos el que se mueve
    // recibe un sonic boom que le saca el 90% de la vida (una sola vez). Después queda 4 segundos aturdido y recibe
    // 50% más de daño. Dura 15 segundos
    private void deepSilence() {
        if (activePlayers().isEmpty()) return;
        busy = true;
        warden.setAI(false);
        anunciar("Silencio Profundo");
        titulo(ChatColor.of("#29DFEB") + "" + ChatColor.BOLD + "Silencio Profundo", ChatColor.of("#B8F4F7") + "No te muevas", 50);
        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 3f, 0.5f);

        run(new BukkitRunnable() {
            int t = 0;
            final Map<UUID, Location> anchors = new HashMap<>();
            final Set<UUID> punished = new HashSet<>();

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    freezeMinions(false);
                    finish();
                    return;
                }
                t++;
                Location head = warden.getLocation().add(0, 3.6, 0);

                if (t <= 60) {
                    double radius = 6 - t / 12.0;
                    if (t % 2 == 0) ring(warden.getLocation().add(0, 0.2, 0), radius, 30, LIGHT_DUST);
                    if (t % 20 == 0) {
                        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3f, 0.8f);
                        for (Player p : activePlayers()) actionBar(p, ChatColor.of("#29DFEB") + "Quédate quieto en " + (3 - t / 20));
                    }
                    return;
                }

                if (t <= 220) {
                    if (t == 61) {
                        titulo("", ChatColor.of("#E8FBFF") + "" + ChatColor.BOLD + "...", 160);
                        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_SNIFF, 3f, 0.6f);
                        // Los minions también se quedan quietos, así nadie se mueve por un empujón
                        freezeMinions(true);
                        for (Player p : activePlayers()) anchors.put(p.getUniqueId(), p.getLocation());
                    }
                    if (t % 20 == 1) world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 3f, 1f);
                    if (t % 4 == 0) {
                        world.spawnParticle(Particle.SCULK_SOUL, head, 3, 0.6, 0.4, 0.6, 0.01);
                        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);
                    }
                    int left = (220 - t) / 20 + 1;
                    for (Player p : activePlayers()) {
                        UUID id = p.getUniqueId();
                        if (punished.contains(id)) continue;
                        if (t % 20 == 1) actionBar(p, ChatColor.of("#B8F4F7") + "Silencio: " + left + "s");
                        Location anchor = anchors.get(id);
                        if (anchor == null || !anchor.getWorld().equals(p.getWorld())) {
                            anchors.put(id, p.getLocation());
                            continue;
                        }
                        // Caer no cuenta (el que estaba en el aire baja solo); moverse de costado o saltar sí
                        Location now = p.getLocation();
                        if (now.getY() < anchor.getY()) anchor.setY(now.getY());
                        double moved = Math.hypot(now.getX() - anchor.getX(), now.getZ() - anchor.getZ());
                        if (moved <= 0.4 && now.getY() - anchor.getY() <= 0.6) continue;
                        punished.add(id);
                        silenceBoom(p);
                    }
                    return;
                }

                if (t == 221) {
                    freezeMinions(false);
                    stunnedUntil = globalTick + 80;
                    titulo("", ChatColor.of("#29DFEB") + "¡Está aturdido, pégale!", 50);
                    world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_DIG, 3f, 1.4f);
                }
                if (t % 3 == 0) {
                    double a = t * 0.5;
                    for (int i = 0; i < 3; i++) {
                        double angle = a + i * 2 * Math.PI / 3;
                        world.spawnParticle(Particle.ELECTRIC_SPARK, head.clone().add(Math.cos(angle) * 1.3, 0.6, Math.sin(angle) * 1.3), 2, 0, 0, 0, 0);
                    }
                }
                if (t >= 300) {
                    cancel();
                    finish();
                }
            }
        }, 0L, 1L);
    }

    private void freezeMinions(boolean frozen) {
        for (UUID id : minions) {
            if (Bukkit.getEntity(id) instanceof Mob minion && minion.isValid()) minion.setAI(!frozen);
        }
    }

    // El sonic boom del Silencio: un rayo del boss al jugador que se movió
    private void silenceBoom(Player p) {
        World world = warden.getWorld();
        Location eye = warden.getLocation().add(0, 3.4, 0);
        Location body = p.getLocation().add(0, 1, 0);
        face(horizontalTo(warden.getLocation(), p.getLocation()));
        warden.playEffect(EntityEffect.WARDEN_SONIC_ATTACK);
        Vector dir = body.toVector().subtract(eye.toVector());
        double length = dir.length();
        dir.normalize();
        for (double d = 1; d <= length; d += 1.2) {
            world.spawnParticle(Particle.SONIC_BOOM, eye.clone().add(dir.clone().multiply(d)), 1);
        }
        world.playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 0.8f);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        sonicDamage(p, sinDificultad(p.getWorld().getDifficulty(), (max != null ? max.getValue() : 20) * 0.9));
        actionBar(p, ChatColor.of("#29DFEB") + "Te escuchó...");
    }

    // Esfera Resonante: se queda quieto y lo rodea una esfera gigante; hay 3 segundos para salir y después adentro
    // empiezan las explosiones y todo el que sigue ahí recibe daño sónico cada medio segundo
    private void resonantSphere() {
        if (activePlayers().isEmpty()) return;
        busy = true;
        warden.setAI(false);
        anunciar("Esfera Resonante");
        World world = warden.getWorld();
        final double radius = 13;
        Location center = warden.getLocation().add(0, 2, 0);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 3f, 0.5f);
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 3f, 0.5f);

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
                if (t <= 60) {
                    // La esfera crece en el primer medio segundo y después late
                    double r = Math.min(radius, radius * t / 10.0);
                    if (t % 4 == 0 || t <= 10) sphere(center, r, Math.PI / 14, t > 40 ? DANGER_DUST : SCULK_DUST);
                    if (t % 20 == 0) {
                        int left = 3 - t / 20;
                        world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 3f, 0.5f + t / 60f);
                        for (Player p : activePlayers()) {
                            if (p.getLocation().add(0, 1, 0).distance(center) > radius) continue;
                            p.sendTitle(left > 0 ? ChatColor.of("#E8FBFF") + "" + left : "", ChatColor.of("#29DFEB") + "¡Sal de la esfera!", 0, 22, 4);
                        }
                    }
                    world.spawnParticle(Particle.SCULK_SOUL, center, 2, 1, 1, 1, 0.02);
                    return;
                }
                if (t == 61) {
                    warden.playEffect(EntityEffect.WARDEN_SONIC_ATTACK);
                    world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 4f, 0.5f);
                    world.spawnParticle(Particle.SONIC_BOOM, center, 1);
                }
                if (t % 5 == 0) sphere(center, radius, Math.PI / 12, DANGER_DUST);
                // Explosiones al azar dentro de la esfera
                for (int i = 0; i < 4; i++) {
                    Vector offset = new Vector(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
                    if (offset.lengthSquared() > 1) offset.normalize();
                    Location point = center.clone().add(offset.multiply(radius * 0.9));
                    world.spawnParticle(t % 2 == 0 ? Particle.EXPLOSION : Particle.SCULK_CHARGE_POP, point, 1, 0, 0, 0, 0);
                }
                if (t % 6 == 0) world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f + random.nextFloat() * 0.4f);
                if (t % 10 == 0) {
                    for (Player p : activePlayers()) {
                        if (p.getLocation().add(0, 1, 0).distance(center) > radius) continue;
                        sonicDamage(p, 6);
                        world.spawnParticle(Particle.EXPLOSION, p.getLocation().add(0, 1, 0), 1);
                    }
                }
                if (t >= 120) {
                    cancel();
                    finish();
                }
            }
        }, 0L, 1L);
    }

    // Lluvia Sónica: caen del techo pilares de sonic boom por toda la arena (3 por jugador, mínimo 5) y se quedan un
    // minuto. El que toca uno recibe 12 de daño sónico. El boss sigue peleando mientras están
    private void sonicRain() {
        List<Player> players = activePlayers();
        if (players.isEmpty()) return;
        busy = true;
        anunciar("Lluvia Sónica");
        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ROAR, 3f, 0.8f);
        warden.playEffect(EntityEffect.WARDEN_SONIC_ATTACK);

        int count = Math.min(15, Math.max(5, players.size() * 3));
        List<Location> spots = new ArrayList<>();
        // Uno cerca de cada jugador y el resto por la arena
        for (Player p : players) {
            if (spots.size() >= count) break;
            Location near = groundNear(p.getLocation(), 2 + random.nextDouble() * 3);
            if (near != null) spots.add(near);
        }
        for (int i = 0; i < count * 6 && spots.size() < count; i++) {
            Location spot = groundNear(spawnLocation, 3 + random.nextDouble() * (getArenaRadius() - 6));
            if (spot != null && spots.stream().noneMatch(s -> s.distanceSquared(spot) < 9)) spots.add(spot);
        }

        run(new BukkitRunnable() {
            int t = 0;
            int created = 0;

            @Override
            public void run() {
                if (!alive()) {
                    cancel();
                    finish();
                    return;
                }
                t++;
                if (t % 4 == 1 && created < spots.size()) {
                    pillars.add(new Pilar(spots.get(created++), globalTick));
                    startPillarTask();
                }
                if (created >= spots.size() && t > created * 4 + 20) {
                    cancel();
                    finish();
                }
            }
        }, 0L, 1L);
    }

    // Un pilar: 20 ticks cayendo de arriba hacia abajo y después 60 segundos en el suelo
    private static final class Pilar {
        final Location base;
        final int born;

        Pilar(Location base, int born) {
            this.base = base;
            this.born = born;
        }
    }

    private void startPillarTask() {
        if (pillarTask != null && !pillarTask.isCancelled()) return;
        pillarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel();
                    pillars.clear();
                    return;
                }
                if (isHibernating()) return;
                World world = warden.getWorld();
                pillars.removeIf(p -> globalTick - p.born > 20 + 1200);
                if (pillars.isEmpty()) {
                    cancel();
                    return;
                }
                for (Pilar pillar : pillars) {
                    int age = globalTick - pillar.born;
                    if (age <= 20) {
                        double headY = 16 - age * 0.8;
                        Location head = pillar.base.clone().add(0, headY, 0);
                        if (age % 2 == 0) world.spawnParticle(Particle.SONIC_BOOM, head, 1);
                        for (double y = headY; y <= 16; y += 1.5) {
                            world.spawnParticle(Particle.DUST, pillar.base.clone().add(0, y, 0), 1, 0.15, 0.2, 0.15, 0, LIGHT_DUST);
                        }
                        if (age == 20) {
                            world.playSound(pillar.base, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1.2f);
                            world.spawnParticle(Particle.BLOCK, pillar.base, 20, 0.6, 0.1, 0.6, 0, sculk());
                            ring(pillar.base.clone().add(0, 0.1, 0), 1.2, 14, SCULK_DUST);
                        }
                        continue;
                    }
                    if (age % 4 == 0) {
                        double spin = age * 0.25;
                        for (double y = 0.2; y <= 5; y += 0.6) {
                            double a = spin + y;
                            world.spawnParticle(Particle.DUST, pillar.base.clone().add(Math.cos(a) * 0.55, y, Math.sin(a) * 0.55), 1, 0, 0, 0, 0, LIGHT_DUST);
                            world.spawnParticle(Particle.DUST, pillar.base.clone().add(-Math.cos(a) * 0.55, y, -Math.sin(a) * 0.55), 1, 0, 0, 0, 0, SCULK_DUST);
                        }
                    }
                    if (age % 16 == 0) world.spawnParticle(Particle.SONIC_BOOM, pillar.base.clone().add(0, 1 + random.nextDouble() * 3, 0), 1);
                    if (age % 40 == 0) world.playSound(pillar.base, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 1.6f);
                }
                for (Player p : activePlayers()) {
                    Integer last = pillarHits.get(p.getUniqueId());
                    if (last != null && globalTick - last < 20) continue;
                    Location loc = p.getLocation();
                    for (Pilar pillar : pillars) {
                        if (globalTick - pillar.born <= 20) continue;
                        double dy = loc.getY() - pillar.base.getY();
                        if (dy < -1 || dy > 5) continue;
                        if (Math.hypot(loc.getX() - pillar.base.getX(), loc.getZ() - pillar.base.getZ()) > 1.0) continue;
                        pillarHits.put(p.getUniqueId(), globalTick);
                        sonicDamage(p, 12);
                        world.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 1);
                        break;
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Llamado de la Infestación: de la tierra salen Warden Zombies y Esqueletos Infestados (2, o 3 con menos de la
    // mitad de vida), hasta 5 vivos
    private void infestationCall() {
        if (minions.size() >= MAX_MINIONS) {
            resonantSphere();
            return;
        }
        busy = true;
        warden.setAI(false);
        anunciar("Llamado de la Infestación");

        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ROAR, 3f, 1.1f);
        world.playSound(warden.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 3f, 0.7f);
        warden.playEffect(EntityEffect.WARDEN_TENDRIL_SHAKE);

        int count = Math.min(enraged() ? 3 : 2, MAX_MINIONS - minions.size());
        List<Location> spots = new ArrayList<>();
        for (int i = 0; i < count * 5 && spots.size() < count; i++) {
            Location spot = groundNear(warden.getLocation(), 4 + random.nextDouble() * 6);
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
                    double r = 1.6 - t / 25.0;
                    ring(spot.clone().add(0, 0.1, 0), Math.max(0.2, r), 12, SCULK_DUST);
                    world.spawnParticle(Particle.BLOCK, spot, 6, 0.4, 0.1, 0.4, 0, sculk());
                    if (t % 5 == 0) line(warden.getLocation().add(0, 2.5, 0), spot.clone().add(0, 0.5, 0), 0.8, DARK_DUST);
                }
                if (t < 30) return;

                cancel();
                Player target = nearestPlayer();
                int i = 0;
                for (Location spot : spots) {
                    world.playSound(spot, Sound.BLOCK_SCULK_CATALYST_BLOOM, 2f, 0.8f);
                    world.spawnParticle(Particle.SCULK_SOUL, spot.clone().add(0, 1, 0), 15, 0.4, 0.8, 0.4, 0.05);
                    Mob minion = i++ % 2 == 0 ? zombieFactory.spawnWardenZombie(spot) : skeletonFactory.spawnInfestedSkeleton(spot);
                    marcarMinion(minion);
                    if (target != null) minion.setTarget(target);
                    minions.add(minion.getUniqueId());
                }
                finish();
            }
        }, 0L, 1L);
    }

    // Cacería en la Oscuridad: apaga la luz (Oscuridad a todos), se vuelve invisible y aparece 3 veces detrás de
    // jugadores al azar para atacarlos
    private void darkHunt() {
        if (activePlayers().isEmpty()) return;
        busy = true;
        warden.setAI(false);
        anunciar("Cacería en la Oscuridad");
        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSEST, 3f, 0.6f);
        for (Player p : activePlayers()) p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0));
        sphere(warden.getLocation().add(0, 2.2, 0), 3, Math.PI / 9, DARK_DUST);
        world.spawnParticle(Particle.SCULK_SOUL, warden.getLocation().add(0, 2, 0), 40, 1.2, 1.6, 1.2, 0.05);
        warden.setInvisible(true);
        warden.setCustomNameVisible(false);
        huntStep(3);
    }

    private void huntStep(int left) {
        if (!alive() || left <= 0) {
            endHunt();
            return;
        }
        List<Player> players = activePlayers();
        if (players.isEmpty()) {
            endHunt();
            return;
        }
        Player target = players.get(random.nextInt(players.size()));
        Vector back = target.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 0.01) back = new Vector(0, 0, 1);
        Location behind = target.getLocation().subtract(back.normalize().multiply(2.8));
        behind.setY(target.getLocation().getY());
        if (!roomFor(behind)) behind = target.getLocation().clone();
        if (!roomFor(behind)) {
            run(delayed(() -> huntStep(left - 1)), 10L, 1L);
            return;
        }
        Location destination = behind;
        teleportWithVisual(destination, () -> {
            if (!alive() || !valid(target)) {
                endHunt();
                return;
            }
            warden.setInvisible(false);
            setFocus(target);
            Vector dir = horizontalTo(warden.getLocation(), target.getLocation());
            claw(dir, 5, 18, false);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
            run(delayed(() -> {
                if (left > 1) {
                    warden.setInvisible(true);
                    warden.getWorld().spawnParticle(Particle.SCULK_SOUL, warden.getLocation().add(0, 2, 0), 25, 1, 1.4, 1, 0.04);
                }
                huntStep(left - 1);
            }), 15L, 1L);
        });
    }

    private void endHunt() {
        warden.setInvisible(false);
        warden.setCustomNameVisible(true);
        finish();
    }

    private BukkitRunnable delayed(Runnable action) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                cancel();
                action.run();
            }
        };
    }

    // ---------------------------------------------------------------- Regeneración

    // Corazón de Sculk: vuelve al centro de la arena y levanta 4 catalizadores que lo curan mientras estén. Recibe la
    // mitad de daño hasta que se rompan los 4 (un golpe o una flecha cada uno) o pasen 30 segundos
    private void startRegeneration() {
        regenerating = true;
        regenTicks = -20;
        warden.getPersistentDataContainer().set(regensKey, PersistentDataType.INTEGER, regens() + 1);
        warden.setAI(false);
        anunciar("Corazón de Sculk");
        titulo("", ChatColor.of("#29DFEB") + "¡El Ultra Warden se regenera! Rompe los catalizadores", 50);
        for (Player p : arenaPlayers()) p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
        teleportWithVisual(spawnLocation.clone(), () -> {
            if (!alive()) return;
            World world = warden.getWorld();
            world.playSound(spawnLocation, Sound.ENTITY_WARDEN_ROAR, 4f, 0.5f);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + i * Math.PI / 2;
                Location base = spawnLocation.clone().add(Math.cos(a) * 11, 0, Math.sin(a) * 11);
                Location floor = ground(base, spawnLocation.getBlockY());
                catalysts.add(new Catalizador((floor != null ? floor : base).add(0, 1.6, 0)));
            }
        });
    }

    private void regenTick() {
        regenTicks++;
        if (regenTicks <= 0) return;
        if (warden.hasAI()) warden.setAI(false);
        World world = warden.getWorld();
        Location heart = warden.getLocation().add(0, 2.6, 0);

        for (Catalizador c : new ArrayList<>(catalysts)) {
            if (!c.isValid()) {
                catalysts.remove(c);
                continue;
            }
            c.girar();
            if (c.fueGolpeado() || c.fueDisparado()) breakCatalyst(c);
        }

        if (regenTicks % 20 == 0 && !catalysts.isEmpty()) {
            double heal = catalysts.size() * REGEN_PER_CATALYST / SCALE;
            warden.setHealth(Math.min(REAL_HEALTH, warden.getHealth() + heal));
            world.spawnParticle(Particle.HEART, heart, catalysts.size(), 0.8, 0.8, 0.8, 0);
            world.playSound(heart, Sound.ENTITY_WARDEN_HEARTBEAT, 3f, 1.2f);
        }
        if (regenTicks % 3 == 0) {
            for (Catalizador c : catalysts) {
                Location from = c.center.clone();
                Vector to = heart.toVector().subtract(from.toVector());
                double length = to.length();
                to.normalize();
                double offset = (regenTicks % 12) / 12.0 * 1.5;
                for (double d = offset; d < length; d += 1.5) {
                    Location point = from.clone().add(to.clone().multiply(d));
                    world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, LIGHT_DUST);
                    world.spawnParticle(Particle.SCULK_SOUL, point, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
        // Cada 4 segundos empuja a los que se pegan al corazón
        if (regenTicks % 80 == 0) {
            world.playSound(heart, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1.4f);
            ring(warden.getLocation().add(0, 0.3, 0), 6, 40, LIGHT_DUST);
            for (Player p : activePlayers()) {
                Vector away = p.getLocation().toVector().subtract(warden.getLocation().toVector());
                if (away.length() > 6) continue;
                away.setY(0);
                Vector knock = away.lengthSquared() < 0.01 ? new Vector(1, 0, 0) : away.normalize();
                sonicDamage(p, 8);
                p.setVelocity(knock.multiply(1.2).setY(0.45));
            }
        }
        if (catalysts.isEmpty() || regenTicks >= REGEN_TICKS) endRegeneration();
    }

    private void breakCatalyst(Catalizador c) {
        World world = c.center.getWorld();
        Location loc = c.center;
        c.remove();
        catalysts.remove(c);
        world.playSound(loc, Sound.BLOCK_SCULK_CATALYST_BREAK, 3f, 0.6f);
        world.playSound(loc, Sound.ENTITY_WARDEN_HURT, 2f, 1.4f);
        world.spawnParticle(Particle.SCULK_SOUL, loc, 30, 0.5, 0.5, 0.5, 0.08);
        world.spawnParticle(Particle.BLOCK, loc, 40, 0.5, 0.5, 0.5, 0, Material.SCULK_CATALYST.createBlockData());
        sphere(loc, 1.4, Math.PI / 7, LIGHT_DUST);
        for (Player p : arenaPlayers()) actionBar(p, ChatColor.of("#29DFEB") + "Catalizador roto: quedan " + catalysts.size());
    }

    private void endRegeneration() {
        regenerating = false;
        catalysts.forEach(Catalizador::remove);
        catalysts.clear();
        if (!isHibernating()) warden.setAI(true);
        World world = warden.getWorld();
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_ROAR, 4f, 0.8f);
        world.spawnParticle(Particle.SONIC_BOOM, warden.getLocation().add(0, 2.4, 0), 1);
        world.spawnParticle(Particle.SCULK_SOUL, warden.getLocation().add(0, 2.4, 0), 60, 1.2, 1.8, 1.2, 0.08);
    }

    // Un catalizador de sculk que gira flotando (BlockDisplay) con una hitbox para pegarle, como el panal de la
    // Abeja Floral. Las flechas atraviesan los Display, así que se revisan a mano
    private class Catalizador {
        private final BlockDisplay display;
        private final Interaction hitbox;
        private final Location center;
        private final long creado;
        private float angle;
        private boolean roto = false;

        Catalizador(Location loc) {
            this.center = loc.clone();
            World w = loc.getWorld();
            this.display = w.spawn(loc, BlockDisplay.class, d -> {
                d.setBlock(Material.SCULK_CATALYST.createBlockData());
                d.setGlowing(true);
                d.setGlowColorOverride(CIAN);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(2);
            });
            this.hitbox = w.spawn(loc.clone().add(0, -0.7, 0), Interaction.class, i -> {
                i.setInteractionWidth(1.4f);
                i.setInteractionHeight(1.4f);
                i.setResponsive(true);
                i.setPersistent(false);
            });
            this.creado = w.getGameTime();
            this.angle = random.nextFloat() * 6.28f;
            w.playSound(loc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 2f, 0.7f);
            w.spawnParticle(Particle.SCULK_SOUL, loc, 20, 0.4, 0.6, 0.4, 0.05);
            girar();
        }

        boolean isValid() {
            return !roto && display.isValid();
        }

        void girar() {
            if (!display.isValid()) return;
            angle += 0.12f;
            float bob = (float) Math.sin(angle * 1.5) * 0.12f;
            Quaternionf rot = new Quaternionf().rotateY(angle).rotateX(0.35f);
            float scale = 0.9f;
            Vector3f trans = rot.transform(new Vector3f(-0.5f * scale, -0.5f * scale, -0.5f * scale)).add(0, bob, 0);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(trans, rot, new Vector3f(scale, scale, scale), new Quaternionf()));
        }

        boolean fueGolpeado() {
            if (!hitbox.isValid()) return false;
            Interaction.PreviousInteraction ataque = hitbox.getLastAttack();
            return ataque != null && ataque.getTimestamp() > creado;
        }

        boolean fueDisparado() {
            for (Entity e : center.getWorld().getNearbyEntities(center, 4, 4, 4)) {
                if (!(e instanceof Projectile proj) || !(proj.getShooter() instanceof Player)) continue;
                if (proj instanceof AbstractArrow arrow && arrow.isInBlock()) continue;
                Vector desde = proj.getLocation().toVector();
                Vector hasta = desde.clone().add(proj.getVelocity());
                if (distanciaASegmento(center.toVector(), desde, hasta) <= 1.0) {
                    proj.remove();
                    return true;
                }
            }
            return false;
        }

        void remove() {
            roto = true;
            if (display.isValid()) display.remove();
            if (hitbox.isValid()) hitbox.remove();
        }
    }

    static double distanciaASegmento(Vector punto, Vector a, Vector b) {
        Vector ab = b.clone().subtract(a);
        double longitud = ab.lengthSquared();
        if (longitud < 1.0E-6) return punto.distance(a);
        double t = punto.clone().subtract(a).dot(ab) / longitud;
        t = Math.max(0, Math.min(1, t));
        return punto.distance(a.clone().add(ab.multiply(t)));
    }

    // ---------------------------------------------------------------- Eventos

    @EventHandler(ignoreCancelled = true)
    public void onAnger(WardenAngerChangeEvent event) {
        if (!event.getEntity().equals(warden) || event.getNewAnger() <= event.getOldAnger()) return;
        if (!(event.getTarget() instanceof Player player) || !inArena(player.getLocation())) event.setCancelled(true);
    }

    // El daño que recibe se divide entre 2 (su vida es de 2000). Mientras se regenera recibe la mitad y aturdido
    // (después del Silencio Profundo) 50% más. Mientras emerge no recibe daño, no se cae ni se ahoga y lo que invoca
    // no le pega
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWardenDamage(EntityDamageEvent event) {
        if (!event.getEntity().equals(warden)) return;
        switch (event.getCause()) {
            case FALL, DROWNING, SUFFOCATION, FLY_INTO_WALL, CRAMMING, FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> {
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
        if (globalTick < emergingUntil) {
            event.setCancelled(true);
            return;
        }

        double damage = event.getDamage();
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            Player attacker = attacker(damager);
            if (attacker != null) {
                addAttacker(attacker);
            } else if (minions.contains(damager.getUniqueId()) || isMinionShot(damager)) {
                event.setCancelled(true);
                return;
            }
        }
        if (regenerating) damage *= 0.5;
        if (globalTick < stunnedUntil) damage *= 1.5;
        event.setDamage(damage / SCALE);
    }

    // El sonic boom normal del warden pega menos, los fuertes son los de sus ataques
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().equals(warden) && event.getCause() == EntityDamageEvent.DamageCause.SONIC_BOOM && !customBoom) {
            event.setDamage(8);
        }
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) return p;
        return null;
    }

    private boolean isMinionShot(Entity damager) {
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                && minions.contains(shooter.getUniqueId());
    }

    // No suelta el catalizador del warden: el premio (Núcleos Sónicos) lo da BossRewards a cada uno que peleó
    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(EntityDeathEvent event) {
        if (!event.getEntity().equals(warden)) return;
        event.getDrops().clear();
        event.setDroppedExp(1500);
        World world = warden.getWorld();
        Location loc = warden.getLocation().add(0, 2.4, 0);
        world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, 4f, 0.5f);
        world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 4f, 0.5f);
        world.spawnParticle(Particle.SONIC_BOOM, loc, 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.SCULK_SOUL, loc, 120, 1.8, 2, 1.8, 0.1);
        sphere(loc, 4, Math.PI / 10, LIGHT_DUST);
        ring(warden.getLocation().add(0, 0.2, 0), 6, 50, SCULK_DUST);
    }
}
