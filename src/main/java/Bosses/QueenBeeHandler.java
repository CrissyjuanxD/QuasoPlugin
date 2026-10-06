package Bosses;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.boss.BarColor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Combate de la Abeja Floral de OneBlock: 600 HP, cuatro especiales y panales
 * giratorios de regeneración desde 200 HP. Usa chat y bossbars nativas.
 * Conserva el identificador de la Reina para las misiones y recompensas de Quaso.
 */
public class QueenBeeHandler extends BaseBoss implements Listener {

    //  /debugarena
    public static final Map<UUID, QueenBeeHandler> ACTIVE_BOSSES = new HashMap<>();

    /** Vida total del boss. */
    public static final double MAX_HP = 600.0;

    /** Vida a la que puede empezar la fase de regeneracion. */
    private static final double REGEN_TRIGGER_HP = 200.0;

    /** Musica de la pelea. Suena en el centro de la arena, categoria RECORDS. */
    private static final String MUSICA_BOSS = "minecraft:custom.abeja_floral_music";

    private static final long RETRASO_ATAQUE_TICKS = 10L;
    private final Set<CombatTask> combatTasks = new HashSet<>();
    private final Set<Entity> combatEntities = new HashSet<>();
    private boolean disposed;

    private final Bee bee;
    private final Random random = new Random();

    // Estados
    private int globalTick = 0;
    private boolean runningSpecial = false;
    private boolean runningMelee = false;
    private boolean inRegenerationPhase = false;
    /** Medio segundo entre el aviso del Polen Regenerador y la fase en si. */
    private boolean preparandoRegeneracion = false;
    private boolean isDying = false;
    private boolean isFinalDeath = false;
    private Player killer = null;

    private int meleeDoneSinceLastSpecial = 0;
    private int requiredMeleeBetweenSpecials = 1;
    private int regenCooldown = 0;

    /** La primera regeneracion esta escripteada: sale si o si al llegar a 200 HP. */
    private boolean primeraRegeneracionHecha = false;

    // Curación
    private final List<HealTotem> healTotems = new ArrayList<>();
    private BukkitRunnable regenTask;

    private final NamespacedKey bossKey;
    private final NamespacedKey arenaCenterX;
    private final NamespacedKey arenaCenterY;
    private final NamespacedKey arenaCenterZ;

    // ==============================
    //         PALETA FLORAL
    // ==============================

    /** Pasteles y encendidos: naranja, azul, rojo, verde, amarillo, morado y rosa. */
    private static final Color[] FLORAL_COLORS = {
            Color.fromRGB(255, 179, 128), // naranja pastel
            Color.fromRGB(255, 140, 40),  // naranja encendido
            Color.fromRGB(150, 200, 255), // azul pastel
            Color.fromRGB(60, 140, 255),  // azul encendido
            Color.fromRGB(255, 150, 150), // rojo pastel
            Color.fromRGB(255, 60, 60),   // rojo encendido
            Color.fromRGB(160, 240, 170), // verde pastel
            Color.fromRGB(70, 220, 90),   // verde encendido
            Color.fromRGB(255, 245, 160), // amarillo pastel
            Color.fromRGB(255, 225, 45),  // amarillo encendido
            Color.fromRGB(205, 170, 255), // morado pastel
            Color.fromRGB(160, 70, 255),  // morado encendido
            Color.fromRGB(255, 180, 220), // rosa pastel
            Color.fromRGB(255, 90, 180)   // rosa encendido
    };

    private Color randomFloralColor() {
        return FLORAL_COLORS[random.nextInt(FLORAL_COLORS.length)];
    }

    /** Nube de particulas de colores florales. */
    private void floralBurst(Location loc, int amount, double spread, float size) {
        World w = loc.getWorld();
        if (w == null) return;

        for (int i = 0; i < amount; i++) {
            double ox = (random.nextDouble() - 0.5) * spread * 2;
            double oy = (random.nextDouble() - 0.5) * spread * 2;
            double oz = (random.nextDouble() - 0.5) * spread * 2;

            w.spawnParticle(Particle.DUST, loc.clone().add(ox, oy, oz), 1,
                    new Particle.DustOptions(randomFloralColor(), size));
        }
    }

    /** Particula suelta de un color concreto (para las estelas). */
    private void floralDust(Location loc, Color color, float size) {
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.DUST, loc.getX(), loc.getY(), loc.getZ(),
                1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, size));
    }

    public QueenBeeHandler(JavaPlugin plugin, Bee bee) {
        super(plugin, bee);
        this.bee = bee;

        this.bossKey = new NamespacedKey(plugin, "is_queen_bee");
        this.arenaCenterX = new NamespacedKey(plugin, "arena_x");
        this.arenaCenterY = new NamespacedKey(plugin, "arena_y");
        this.arenaCenterZ = new NamespacedKey(plugin, "arena_z");

        // Una Reina guardada adopta el combate floral sin recuperar la vida perdida.
        var health = Objects.requireNonNull(bee.getAttribute(Attribute.MAX_HEALTH));
        double healthRatio = bee.getHealth() / health.getValue();
        health.setBaseValue(MAX_HP);
        bee.setHealth(Math.min(health.getValue(), Math.max(1, healthRatio * health.getValue())));
        bee.setCustomName(ChatColor.of("#ffb3d9") + "" + ChatColor.BOLD + "Abeja Floral");
        bee.setGravity(true);

        // --- LÓGICA DE PERSISTENCIA ---
        PersistentDataContainer pdc = bee.getPersistentDataContainer();

        if (pdc.has(bossKey, PersistentDataType.BYTE)) {
            double x = pdc.getOrDefault(arenaCenterX, PersistentDataType.DOUBLE, spawnLocation.getX());
            double y = pdc.getOrDefault(arenaCenterY, PersistentDataType.DOUBLE, spawnLocation.getY());
            double z = pdc.getOrDefault(arenaCenterZ, PersistentDataType.DOUBLE, spawnLocation.getZ());

            this.spawnLocation.setX(x);
            this.spawnLocation.setY(y);
            this.spawnLocation.setZ(z);

            this.start();

        } else {
            // CASO 2: PRIMER SPAWN
            pdc.set(bossKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(arenaCenterX, PersistentDataType.DOUBLE, spawnLocation.getX());
            pdc.set(arenaCenterY, PersistentDataType.DOUBLE, spawnLocation.getY());
            pdc.set(arenaCenterZ, PersistentDataType.DOUBLE, spawnLocation.getZ());
        }

        ACTIVE_BOSSES.put(bee.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ==============================
    //        SPAWN DEL BOSS
    // ==============================

    public static QueenBeeHandler spawn(JavaPlugin plugin, Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        Bee bee = world.spawn(center, Bee.class, b -> {
            b.setCustomName(ChatColor.of("#ffb3d9") + "" + ChatColor.BOLD + "Abeja Floral");
            b.setCustomNameVisible(true);
            b.setRemoveWhenFarAway(false);
            b.setAnger(999999);
            b.setAI(true);

            Objects.requireNonNull(b.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(MAX_HP);
            Objects.requireNonNull(b.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.35);
            Objects.requireNonNull(b.getAttribute(Attribute.FOLLOW_RANGE)).setBaseValue(50);
            Objects.requireNonNull(b.getAttribute(Attribute.SCALE)).setBaseValue(3);

            b.setHealth(MAX_HP);
            b.setHasStung(false);
            b.setCannotEnterHiveTicks(Integer.MAX_VALUE);

            b.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            b.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        });

        QueenBeeHandler handler = new QueenBeeHandler(plugin, bee);
        handler.start();
        return handler;
    }

    // ==============================
    //       BASEBOSS OVERRIDES
    // ==============================

    @Override
    protected String getBossTitle() {
        return ChatColor.of("#ffb3d9") + "Abeja Floral";
    }

    @Override
    protected int getArenaRadius() {
        return 28;
    }

    @Override
    protected int getArenaHeightUp() {
        return 20;
    }

    @Override
    protected int getArenaHeightDown() {
        return 8;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.CIRCULAR;
    }

    @Override
    protected void onStart() {
        requiredMeleeBetweenSpecials = random.nextInt(3) + 1;
        iniciarMusica();
    }

    @Override
    protected void onTick() {
        if (isDying || isHibernating() || disposed) return;

        globalTick++;

        if (regenCooldown > 0) {
            regenCooldown--;
        }

        bee.setHasStung(false);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setAnger(999999);

        // Aura floral constante alrededor del boss
        if (globalTick % 4 == 0) {
            floralBurst(bee.getLocation().add(0, 1.2, 0), 4, 1.4, 1.1f);
        }

        if (inRegenerationPhase) {
            bee.setTarget(null);
            return;
        }

        if (runningSpecial || runningMelee || preparandoRegeneracion) return;

        double max = Objects.requireNonNull(bee.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        double hp = bee.getHealth();

        boolean enraged = hp < max / 2.0;
        int attackDelay = enraged ? 15 : 30;

        if (globalTick % attackDelay == 0) {
            // La fase de regeneracion solo se puede activar a partir de 200 de vida.
            // La PRIMERA vez esta escripteada (100%); a partir de ahi va al 10%.
            if (hp <= REGEN_TRIGGER_HP && regenTask == null && regenCooldown <= 0) {
                if (!primeraRegeneracionHecha || random.nextDouble() < 0.10) {
                    prepararRegeneracion();
                    return;
                }
            }

            decideNextAttack();
        }
    }

    @Override
    protected void onDeath() {
        bee.getPersistentDataContainer().remove(bossKey);
        cleanupResources();
    }

    @Override
    protected void onUnload() {
        cleanupResources();
    }

    private void cleanupResources() {
        if (disposed) return;
        disposed = true;
        cancelCombat();
        detenerMusica();
        HandlerList.unregisterAll(this);
        ACTIVE_BOSSES.remove(bee.getUniqueId());
    }

    private void cancelCombat() {
        for (CombatTask task : new ArrayList<>(combatTasks)) task.cancel();
        combatTasks.clear();
        regenTask = null;
        for (HealTotem totem : healTotems) totem.remove();
        healTotems.clear();
        for (Entity extra : combatEntities) if (extra.isValid()) extra.remove();
        combatEntities.clear();
    }

    /** Las tareas y entidades de ataque no sobreviven a la muerte o descarga del boss. */
    private abstract class CombatTask extends BukkitRunnable {
        private final boolean repeating;

        CombatTask(boolean repeating) {
            this.repeating = repeating;
            combatTasks.add(this);
        }

        @Override
        public final void run() {
            if (disposed || isDying || !bee.isValid() || bee.isDead()) {
                cancel();
                return;
            }
            try {
                tick();
            } finally {
                if (!repeating) combatTasks.remove(this);
            }
        }

        protected abstract void tick();

        @Override
        public synchronized void cancel() {
            super.cancel();
            combatTasks.remove(this);
        }
    }

    @Override
    protected BarColor getBarColor() {
        return BarColor.PINK;
    }

    // ==============================
    //            MUSICA
    // ==============================

    /**
     * Corta cualquier sonido a los jugadores de la arena y lanza la musica de la
     * pelea EN EL CENTRO de la arena, para que se oiga por cercania: si alguien
     * muere y vuelve, o se aleja y regresa, la sigue escuchando.
     */
    private void iniciarMusica() {
        World w = spawnLocation.getWorld();
        if (w == null) return;

        for (Player p : getActivePlayers()) {
            p.stopAllSounds();
        }

        Location centro = spawnLocation.clone();

        // Pequeño margen para que el stopAllSounds no se coma la propia musica.
        new CombatTask(false) {
            @Override
            protected void tick() {
                w.playSound(centro, MUSICA_BOSS, SoundCategory.RECORDS, 10.0f, 1.0f);
            }
        }.runTaskLater(plugin, 5L);
    }

    /** Al morir el boss la musica se corta a TODOS, esten donde esten. */
    private void detenerMusica() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.stopSound(MUSICA_BOSS, SoundCategory.RECORDS);
        }
    }

    /** Avisa por chat medio segundo antes de ejecutar el especial. */
    private void anunciarAtaque(String nombre, Runnable ataque) {
        for (Player player : getJugadoresArena()) {
            player.sendMessage(ChatColor.of("#e4d5f1") + "✿ "
                    + ChatColor.of("#ffb3d9") + "" + ChatColor.BOLD + "Abeja Floral"
                    + ChatColor.of("#ddb3ff") + " » "
                    + ChatColor.of("#fff5a0") + nombre);
        }
        new CombatTask(false) {
            @Override
            protected void tick() {
                ataque.run();
            }
        }.runTaskLater(plugin, RETRASO_ATAQUE_TICKS);
    }

    // ==============================
    //          UTILIDADES
    // ==============================

    /** Todos los jugadores que estan en la arena (incluidos espectadores). */
    private List<Player> getJugadoresArena() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(bee.getWorld())) {
                list.add(p);
            }
        }
        return list;
    }

    private List<Player> getActivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);

            if (p != null && p.isOnline() && p.getWorld().equals(bee.getWorld())
                    && !p.isDead() && p.getGameMode() != GameMode.CREATIVE
                    && p.getGameMode() != GameMode.SPECTATOR) {
                list.add(p);
            }
        }
        return list;
    }

    private Player getNearestPlayer() {
        return getNearestPlayerTo(bee.getLocation());
    }

    private Player getNearestPlayerTo(Location from) {
        List<Player> p = getActivePlayers();
        if (p.isEmpty()) return null;

        Player near = null;
        double best = Double.MAX_VALUE;

        for (Player pl : p) {
            double d = pl.getLocation().distanceSquared(from);
            if (d < best) {
                best = d;
                near = pl;
            }
        }
        return near;
    }

    // ==============================
    //      TELEPORT VISUAL
    // ==============================

    private void teleportWithVisual(Location from, Location to) {
        showSphere(from, 2.5);
        showSphere(to, 2.5);

        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.1f);

        new CombatTask(false) {
            @Override
            protected void tick() {
                bee.teleport(to);
                to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.6f);
            }
        }.runTaskLater(plugin, 10L);
    }

    private void showSphere(Location center, double radius) {
        World w = Objects.requireNonNull(center.getWorld());

        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.4f);

        for (double phi = 0; phi < Math.PI; phi += Math.PI / 10) {
            double y = radius * Math.cos(phi);
            double r = radius * Math.sin(phi);

            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 10) {
                double x = r * Math.cos(theta);
                double z = r * Math.sin(theta);

                floralDust(center.clone().add(x, y, z), randomFloralColor(), 1.3f);
            }
        }
    }

    // ==============================
    //     SELECCIÓN DE ATAQUES
    // ==============================

    private void decideNextAttack() {
        if (getActivePlayers().isEmpty()) {
            bee.setTarget(null);
            return;
        }

        if (meleeDoneSinceLastSpecial < requiredMeleeBetweenSpecials) {
            startRandomMelee();
            meleeDoneSinceLastSpecial++;
        } else {
            startRandomSpecial();
            meleeDoneSinceLastSpecial = 0;
            requiredMeleeBetweenSpecials = random.nextInt(3) + 1;
        }
    }

    // ==============================
    //    ATAQUES A MELEE (daño -50%)
    // ==============================

    private enum MeleeType { NORMAL, DASH, TP_COMBO }

    private void startRandomMelee() {
        if (runningMelee || runningSpecial || inRegenerationPhase) return;
        runningMelee = true;

        MeleeType t = MeleeType.values()[random.nextInt(MeleeType.values().length)];

        switch (t) {
            case NORMAL -> meleeNormal();
            case DASH -> meleeDash();
            case TP_COMBO -> meleeTPCombo();
        }
    }

    // 1) Ataque normal  (8 -> 4)
    private void meleeNormal() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        bee.setTarget(target);

        new CombatTask(true) {
            int t = 0;

            @Override
            protected void tick() {
                if (!bee.isValid() || bee.isDead() || !getActivePlayers().contains(target)) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                t++;
                if (t > 40) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                if (bee.getLocation().distance(target.getLocation()) <= 2.0) {
                    target.damage(4.0, bee);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                    bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.7f);
                    floralBurst(target.getLocation().add(0, 1, 0), 20, 0.7, 1.2f);
                    cancel();
                    runningMelee = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // 2) Ataque rápido - DASH  (10 -> 5)
    private void meleeDash() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        Location start = bee.getLocation();

        Vector dir = target.getLocation()
                .toVector()
                .subtract(start.toVector())
                .normalize()
                .multiply(1.2);

        bee.getWorld().playSound(start, Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.5f);

        new CombatTask(true) {
            int t = 0;

            @Override
            protected void tick() {
                if (!bee.isValid() || bee.isDead() || !getActivePlayers().contains(target)) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                t++;
                bee.setVelocity(dir);

                floralBurst(bee.getLocation().add(0, 0.8, 0), 6, 0.6, 1.2f);
                bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.4f, 1.5f);

                if (bee.getLocation().distance(target.getLocation()) <= 2.5) {
                    target.damage(5.0, bee);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.4f);
                    floralBurst(target.getLocation().add(0, 1, 0), 25, 0.8, 1.3f);
                    cancel();
                    runningMelee = false;
                }

                if (t > 20) {
                    cancel();
                    runningMelee = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 3) TP Combo: TP al jugador, 4 golpes, vuelve al centro  (6 -> 3)
    private void meleeTPCombo() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        Location start = bee.getLocation();
        Location to = target.getLocation().clone().add(0, 2, 0);
        Location center = spawnLocation.clone().add(0, 4, 0);

        teleportWithVisual(start, to);

        new CombatTask(true) {
            int hits = 0;

            @Override
            protected void tick() {
                if (!bee.isValid() || bee.isDead() || !getActivePlayers().contains(target)) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                if (hits == 0) {
                    bee.teleport(to);
                }

                if (bee.getLocation().distance(target.getLocation()) <= 2.0) {
                    target.damage(3.0, bee);
                    floralBurst(target.getLocation().add(0, 1, 0), 15, 0.5, 1.1f);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BEE_STING, 1f, 1.6f);
                }

                hits++;
                if (hits >= 4) {
                    teleportWithVisual(bee.getLocation(), center);
                    runningMelee = false;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // ==============================
    //   ATAQUES ESPECIALES (daño -50%)
    // ==============================

    /** Nombres legibles sin texturas ni dependencias de interfaz. */
    private enum SpecialType {
        VENOMOUS_STINGS("Aguijones venenosos"),
        EXPLOSIVE_STINGS("Aguijones explosivos"),
        SUMMON_BEES("Refuerzos florales"),
        TOXIC_CLOUD("Nube tóxica");

        private final String nombre;

        SpecialType(String nombre) {
            this.nombre = nombre;
        }
    }

    private void startRandomSpecial() {
        if (runningSpecial || runningMelee || inRegenerationPhase) return;
        runningSpecial = true;

        SpecialType t = SpecialType.values()[random.nextInt(SpecialType.values().length)];

        // runningSpecial ya esta activo, asi que en el medio segundo del aviso no sale otro ataque.
        anunciarAtaque(t.nombre, () -> {
            switch (t) {
                case VENOMOUS_STINGS -> specialVenomousStings();
                case EXPLOSIVE_STINGS -> specialExplosiveStings();
                case SUMMON_BEES -> specialSummonBees();
                case TOXIC_CLOUD -> specialToxicCloud();
            }
        });
    }

    /**
     * Origen de los aguijones (BlockDisplay): 2 bloques mas abajo que antes,
     * para que salgan a la altura del jugador y no por encima del boss.
     */
    private Location getSpikeOrigin() {
        return bee.getLocation().clone().add(0, -0.5, 0);
    }

    // =============================================================
    // 1) Aguijón VENENOSO: 5 en círculo, algunos dirigidos  (11 -> 5.5)
    // =============================================================

    private void specialVenomousStings() {
        World w = bee.getWorld();
        Location origin = getSpikeOrigin();
        List<Player> players = getActivePlayers();

        if (players.isEmpty()) {
            runningSpecial = false;
            return;
        }

        w.playSound(origin, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.4f);

        int count = 5;

        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            baseDirs.add(new Vector(Math.cos(angle), -0.1, Math.sin(angle)).normalize().multiply(0.4));
        }

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        int targetCount = Math.min(shuffled.size(), count);

        for (int i = 0; i < count; i++) {

            Vector dir;
            if (i < targetCount) {
                Player target = shuffled.get(i);
                dir = target.getLocation().add(0, 1, 0)
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .multiply(0.45);
            } else {
                dir = baseDirs.get(i);
            }

            // Cada aguijon tiene su propio color floral, y su estela a juego.
            final Color spikeColor = randomFloralColor();

            BlockDisplay spike = w.spawn(origin, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGlowing(true);
            spike.setGlowColorOverride(spikeColor);
            spike.setGravity(false);
            spike.setPersistent(false);
            combatEntities.removeIf(entity -> !entity.isValid());
            combatEntities.add(spike);

            new CombatTask(true) {
                int life = 0;

                @Override
                protected void tick() {
                    if (!spike.isValid()) {
                        cancel();
                        return;
                    }
                    life++;
                    if (life > 80) {
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLoc = spike.getLocation().add(dir);

                    if (newLoc.getBlock().getType().isSolid()) {
                        createPoisonSphere(newLoc, spikeColor);
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLoc);

                    // ESTELA del color del propio aguijon
                    Location trail = newLoc.clone().add(0.3, 0.3, 0.3);
                    floralDust(trail, spikeColor, 1.2f);
                    floralDust(trail.clone().subtract(dir.clone().multiply(0.5)), spikeColor, 1.0f);
                    floralDust(trail.clone().subtract(dir.clone().multiply(1.0)), spikeColor, 0.8f);

                    for (Entity e : w.getNearbyEntities(newLoc, 1, 1, 1)) {
                        if (e instanceof Player p && getActivePlayers().contains(p)) {
                            p.damage(5.5, bee);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                            floralBurst(newLoc, 20, 0.6, 1.2f);
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new CombatTask(false) {
            @Override
            protected void tick() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    private void createPoisonSphere(Location center, Color color) {
        World w = center.getWorld();
        double radius = 3.0;

        new CombatTask(true) {
            int ticks = 0;

            @Override
            protected void tick() {
                if (ticks++ > 40) {
                    cancel();
                    return;
                }

                w.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 0.7f);

                for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    floralDust(center.clone().add(x, 0.1, z), color, 1.3f);
                }

                for (Entity e : w.getNearbyEntities(center, radius, 1.5, radius)) {
                    if (e instanceof Player p && getActivePlayers().contains(p)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    // =============================================================
    // 2) Aguijón EXPLOSIVO - power 2  (9 -> 4.5 / area 6 -> 3)
    //    Se teletransporta al centro de la arena antes de disparar.
    // =============================================================

    private void specialExplosiveStings() {
        if (getActivePlayers().isEmpty()) {
            runningSpecial = false;
            return;
        }

        Location center = spawnLocation.clone();

        teleportWithVisual(bee.getLocation(), center);
        bee.teleport(center);

        // Ya centrada, lanza la rafaga.
        new CombatTask(false) {
            @Override
            protected void tick() {
                if (!bee.isValid() || bee.isDead()) {
                    runningSpecial = false;
                    return;
                }
                dispararAguijonesExplosivos();
            }
        }.runTaskLater(plugin, 14L);
    }

    private void dispararAguijonesExplosivos() {
        World w = bee.getWorld();
        Location origin = getSpikeOrigin();
        List<Player> players = getActivePlayers();

        if (players.isEmpty()) {
            runningSpecial = false;
            return;
        }

        w.playSound(origin, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.4f, 0.6f);

        int count = 5;

        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            baseDirs.add(new Vector(Math.cos(angle), -0.05, Math.sin(angle)).normalize().multiply(0.5));
        }

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        int targetCount = Math.min(shuffled.size(), count);

        for (int i = 0; i < count; i++) {
            Vector dir;
            if (i < targetCount) {
                Player target = shuffled.get(i);
                dir = target.getLocation().add(0, 1, 0)
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .multiply(0.50);
            } else {
                dir = baseDirs.get(i);
            }

            final Color spikeColor = randomFloralColor();

            BlockDisplay spike = w.spawn(origin, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGlowing(true);
            spike.setGlowColorOverride(spikeColor);
            spike.setGravity(false);
            spike.setPersistent(false);
            combatEntities.removeIf(entity -> !entity.isValid());
            combatEntities.add(spike);

            new CombatTask(true) {
                int life = 0;

                @Override
                protected void tick() {
                    if (!spike.isValid()) {
                        cancel();
                        return;
                    }
                    life++;
                    if (life > 80) {
                        explodeSpike(spike.getLocation(), spikeColor);
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLoc = spike.getLocation().add(dir);

                    if (newLoc.getBlock().getType().isSolid()) {
                        explodeSpike(newLoc, spikeColor);
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLoc);

                    // ESTELA del color del propio aguijon
                    Location trail = newLoc.clone().add(0.3, 0.3, 0.3);
                    floralDust(trail, spikeColor, 1.3f);
                    floralDust(trail.clone().subtract(dir.clone().multiply(0.5)), spikeColor, 1.1f);
                    floralDust(trail.clone().subtract(dir.clone().multiply(1.0)), spikeColor, 0.9f);

                    for (Entity e : w.getNearbyEntities(newLoc, 1, 1, 1)) {
                        if (e instanceof Player p && getActivePlayers().contains(p)) {
                            explodeSpike(newLoc, spikeColor);
                            p.damage(4.5, bee);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new CombatTask(false) {
            @Override
            protected void tick() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    private void explodeSpike(Location loc, Color color) {
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.EXPLOSION, loc, 1);
        floralBurst(loc, 40, 1.2, 1.4f);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.7f);

        // Power 2, sin fuego y sin romper bloques.
        w.createExplosion(loc.getX(), loc.getY(), loc.getZ(), 2.0f, false, false, bee);

        for (Entity e : w.getNearbyEntities(loc, 4, 3, 4)) {
            if (e instanceof Player p && getActivePlayers().contains(p)) {
                p.damage(3.0, bee);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                floralDust(p.getLocation().add(0, 1, 0), color, 1.2f);
            }
        }
    }

    // =============================================================
    // 3) Refuerzos: abejas VANILLA que siempre pueden atacar
    // =============================================================

    private void specialSummonBees() {
        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.5f, 0.7f);

        int count = 8 + random.nextInt(9);

        for (int i = 0; i < count; i++) {
            double dx = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();
            double dz = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();

            Location spawnLoc = spawnLocation.clone().add(dx, 1, dz);

            new CombatTask(true) {
                int y = 0;

                @Override
                protected void tick() {
                    if (y++ > 10) {
                        cancel();
                        return;
                    }
                    w.playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.8f);
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 12) {
                        double x = Math.cos(angle) * 2;
                        double z = Math.sin(angle) * 2;
                        floralDust(spawnLoc.clone().add(x, y * 0.3, z), randomFloralColor(), 1.2f);
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L);

            new CombatTask(false) {
                @Override
                protected void tick() {
                    Bee minion = spawnFloralBee(spawnLoc);
                    if (minion == null) return;

                    new CombatTask(false) {
                        @Override
                        protected void tick() {
                            if (minion.isValid()) {
                                floralBurst(minion.getLocation(), 15, 0.5, 1.0f);
                                minion.remove();
                            }
                        }
                    }.runTaskLater(plugin, 20L * 30);
                }
            }.runTaskLater(plugin, 25L);
        }

        new CombatTask(false) {
            @Override
            protected void tick() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    /**
     * Abeja vanilla que NUNCA pierde el aguijon: puede picar siempre y no muere
     * despues de atacar.
     */
    private Bee spawnFloralBee(Location loc) {
        World w = loc.getWorld();
        if (w == null) return null;

        Bee minion = w.spawn(loc, Bee.class, b -> {
            b.setCustomName(ChatColor.of("#ffd6f0") + "Guardian Abeja Floral");
            b.setCustomNameVisible(false);
            b.setRemoveWhenFarAway(true);
            b.setPersistent(false);
            b.setHasStung(false);
            b.setAnger(999999);
            b.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        });

        combatEntities.removeIf(entity -> !entity.isValid());
        combatEntities.add(minion);

        Player near = getNearestPlayerTo(loc);
        if (near != null) minion.setTarget(near);

        // Mientras viva: aguijon intacto, siempre enfadada y siempre con objetivo.
        new CombatTask(true) {
            int t = 0;

            @Override
            protected void tick() {
                if (!minion.isValid() || minion.isDead()) {
                    cancel();
                    return;
                }

                minion.setHasStung(false);
                minion.setAnger(999999);
                minion.setCannotEnterHiveTicks(Integer.MAX_VALUE);

                if (t % 20 == 0) {
                    LivingEntity current = minion.getTarget();
                    if (current == null || !current.isValid() || current.isDead()) {
                        Player target = getNearestPlayerTo(minion.getLocation());
                        if (target != null) minion.setTarget(target);
                    }

                    floralBurst(minion.getLocation().add(0, 0.4, 0), 3, 0.4, 0.9f);
                }

                t++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return minion;
    }

    // =============================================================
    // 4) Nube tóxica
    // =============================================================

    private void specialToxicCloud() {
        World w = bee.getWorld();

        Location center = spawnLocation.clone().add(0, 0, 0);
        teleportWithVisual(bee.getLocation(), center);
        bee.teleport(center);

        w.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.6f);

        double[] yOffsets = {-6, -3, 1};

        new CombatTask(true) {
            double radius = 3;

            @Override
            protected void tick() {
                if (radius > getArenaRadius()) {
                    cancel();
                    runningSpecial = false;
                    return;
                }

                w.playSound(center, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.7f, 0.6f);

                for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 28) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    for (double oy : yOffsets) {
                        floralDust(center.clone().add(x, oy, z), randomFloralColor(), 1.4f);
                    }
                }

                for (Entity e : w.getNearbyEntities(center, radius, 6, radius)) {
                    if (e instanceof Player p && getActivePlayers().contains(p)) {
                        if (areaZone.isInside(p.getLocation())) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        }
                    }
                }

                radius += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    // =============================================================
    // 5) Regeneración: a partir de 200 HP, con panales giratorios
    // =============================================================

    /**
     * Panal de curacion: un BlockDisplay girando (no es una entidad viva, asi
     * que no se le puede hacer daño normal) con una Interaction de hitbox.
     * Estalla de un solo golpe o de un flechazo.
     */
    private class HealTotem {
        private final BlockDisplay display;
        private final Interaction hitbox;
        private final Location center;
        private final long creado;

        private float angle;
        private boolean roto = false;

        HealTotem(Location loc) {
            this.center = loc.clone();
            World w = Objects.requireNonNull(loc.getWorld());

            this.display = w.spawn(loc, BlockDisplay.class, d -> {
                d.setBlock(Bukkit.createBlockData(Material.HONEYCOMB_BLOCK));
                d.setGlowing(true);
                d.setGlowColorOverride(Color.fromRGB(255, 225, 45));
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(2);
            });

            // La hitbox se baja media altura para quedar centrada en el panal.
            this.hitbox = w.spawn(loc.clone().add(0, -0.8, 0), Interaction.class, i -> {
                i.setInteractionWidth(1.6f);
                i.setInteractionHeight(1.6f);
                i.setResponsive(true);
                i.setPersistent(false);
            });

            this.creado = w.getGameTime();
            this.angle = random.nextFloat() * 6.28f;
            girar();
        }

        Location getCenter() {
            return center.clone();
        }

        boolean isValid() {
            return !roto && display.isValid();
        }

        /** Gira el panal sobre si mismo. */
        void girar() {
            if (!display.isValid()) return;

            angle += 0.16f;

            Quaternionf rot = new Quaternionf().rotateY(angle).rotateX(0.4f);
            float scale = 0.9f;

            // Traslacion para que el bloque gire sobre su propio centro.
            Vector3f trans = rot.transform(new Vector3f(-0.5f * scale, -0.5f * scale, -0.5f * scale));

            display.setInterpolationDelay(0);
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(
                    trans, rot, new Vector3f(scale, scale, scale), new Quaternionf()));
        }

        /** true si un jugador le ha pegado desde que existe. */
        boolean fueGolpeado() {
            if (!hitbox.isValid()) return false;

            Interaction.PreviousInteraction ataque = hitbox.getLastAttack();
            return ataque != null && ataque.getTimestamp() > creado;
        }

        /**
         * Los proyectiles atraviesan los Display, asi que comprobamos a mano si
         * alguna flecha va a pasar por encima del panal en este tick.
         */
        boolean fueDisparado() {
            World w = center.getWorld();
            if (w == null) return false;

            for (Entity e : w.getNearbyEntities(center, 4, 4, 4)) {
                if (!(e instanceof Projectile proj)) continue;
                if (!(proj.getShooter() instanceof Player)) continue;
                if (proj instanceof AbstractArrow arrow && arrow.isInBlock()) continue;

                Vector desde = proj.getLocation().toVector();
                Vector hasta = desde.clone().add(proj.getVelocity());

                if (distanciaASegmento(center.toVector(), desde, hasta) <= 1.2) {
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

    /** Distancia del punto al segmento a-b (para cazar flechas rapidas). */
    private double distanciaASegmento(Vector punto, Vector a, Vector b) {
        Vector ab = b.clone().subtract(a);
        double longitud = ab.lengthSquared();

        if (longitud < 1.0E-6) return punto.distance(a);

        double t = punto.clone().subtract(a).dot(ab) / longitud;
        t = Math.max(0, Math.min(1, t));

        return punto.distance(a.clone().add(ab.multiply(t)));
    }

    /** Aviso del Polen Regenerador y, medio segundo despues, la fase. */
    private void prepararRegeneracion() {
        preparandoRegeneracion = true;

        anunciarAtaque("Polen regenerador", () -> {
            preparandoRegeneracion = false;
            startRegenerationPhase();
        });
    }

    private void startRegenerationPhase() {
        if (inRegenerationPhase) return;
        inRegenerationPhase = true;
        primeraRegeneracionHecha = true;

        runningSpecial = false;
        runningMelee = false;

        World w = bee.getWorld();
        Location center = spawnLocation.clone().add(0, 4, 0);

        bee.teleport(center);
        bee.setAI(false);
        bee.setTarget(null);
        bee.setVelocity(new Vector(0, 0, 0));
        bee.setGravity(false);
        w.playSound(center, Sound.ITEM_TOTEM_USE, 1f, 0.5f);

        for (Player p : getActivePlayers()) {
            p.sendMessage("\n" +
                    ChatColor.of("#ffb3d9") + "۞" +
                    ChatColor.of("#ffcfa8") + " Destruye los" +
                    ChatColor.of("#fff5a0") + ChatColor.BOLD + " 4 panales giratorios" +
                    ChatColor.of("#ffcfa8") + " para que la Abeja Floral deje de curarse.");
        }

        healTotems.clear();
        double radius = 8;

        // --- Spawnear los 4 panales ---
        for (int i = 0; i < 4; i++) {
            Location l = center.clone().add(
                    Math.cos(i * Math.PI / 2) * radius,
                    -2,
                    Math.sin(i * Math.PI / 2) * radius
            );

            healTotems.add(new HealTotem(l));

            floralBurst(l, 30, 0.8, 1.3f);
            w.playSound(l, Sound.BLOCK_BEEHIVE_ENTER, 1.2f, 0.8f);
        }

        // --- Tarea de Regeneración ---
        regenTask = new CombatTask(true) {
            int t = 0;

            @Override
            protected void tick() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    return;
                }

                if (bee.getLocation().distanceSquared(center) > 1) {
                    bee.teleport(center);
                }
                bee.setAI(false);
                bee.setVelocity(new Vector(0, 0, 0));

                // Giro + deteccion de golpes y flechazos
                for (HealTotem totem : new ArrayList<>(healTotems)) {
                    if (!totem.isValid()) continue;

                    if (t % 2 == 0) totem.girar();

                    if (totem.fueGolpeado() || totem.fueDisparado()) {
                        breakHealTotem(totem);
                    }
                }

                long alive = healTotems.stream().filter(HealTotem::isValid).count();

                if (alive == 0) {
                    finishRegenerationPhase();
                    cancel();
                    return;
                }

                for (HealTotem totem : healTotems) {
                    if (totem.isValid()) {
                        drawBeam(totem.getCenter(), bee.getLocation().add(0, 0.5, 0));
                    }
                }

                // Curacion mucho mas lenta: cada 3 s y solo 2 HP por panal vivo
                // (4 panales = 8 HP cada 3 segundos sobre un total de 600).
                if (t % 60 == 0 && t > 0) {
                    double max = Objects.requireNonNull(bee.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
                    double current = bee.getHealth();

                    double healAmount = alive * 2.0;

                    double newHealth = Math.min(max, current + healAmount);
                    bee.setHealth(newHealth);

                    w.playSound(bee.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
                    floralBurst(bee.getLocation().add(0, 1, 0), 20, 1.0, 1.2f);

                    for (Player p : getJugadoresArena()) {
                        p.sendMessage(ChatColor.of("#ffb3d9") + "" + ChatColor.BOLD + "LA ABEJA FLORAL SE REGENERA: "
                                + ChatColor.of("#a0f0b0") + "+" + (int) healAmount + " HP");
                    }

                    if (newHealth >= max) {
                        finishRegenerationPhase();
                        cancel();
                        return;
                    }
                }

                t++;
            }
        };
        regenTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishRegenerationPhase() {
        // Puede llegar por dos vias a la vez (ultimo panal roto + comprobacion
        // de la tarea), asi que la hacemos idempotente.
        if (!inRegenerationPhase) return;

        inRegenerationPhase = false;
        regenCooldown = 1200;

        if (regenTask != null && !regenTask.isCancelled()) {
            regenTask.cancel();
        }
        regenTask = null;

        for (HealTotem totem : healTotems) {
            if (totem.isValid()) {
                floralBurst(totem.getCenter(), 15, 0.5, 1.1f);
            }
            totem.remove();
        }
        healTotems.clear();

        bee.setAI(true);
        bee.setGravity(true);
        bee.setTarget(getNearestPlayer());

        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.8f);
        floralBurst(bee.getLocation().add(0, 1, 0), 50, 1.2, 1.3f);
        globalTick = 0;
    }

    private void drawBeam(Location from, Location to) {
        World w = from.getWorld();
        if (w == null || !Objects.equals(w, to.getWorld())) return;

        Vector diff = to.toVector().subtract(from.toVector());
        double length = diff.length();
        Vector step = diff.normalize().multiply(0.25);

        Location loc = from.clone();
        for (double d = 0; d < length; d += 0.25) {
            floralDust(loc, randomFloralColor(), 1.1f);
            loc.add(step);
        }
    }

    /** El panal estalla: no aguanta ni un golpe ni un flechazo. */
    private void breakHealTotem(HealTotem totem) {
        Location loc = totem.getCenter();
        World w = loc.getWorld();

        if (w != null) {
            w.spawnParticle(Particle.EXPLOSION, loc, 2);
            floralBurst(loc, 40, 1.0, 1.3f);
            w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
            w.playSound(loc, Sound.BLOCK_BEEHIVE_SHEAR, 1f, 0.8f);
        }

        totem.remove();
        healTotems.remove(totem);

        if (healTotems.isEmpty() && regenTask != null) {
            regenTask.cancel();
            finishRegenerationPhase();
        }
    }

    // ==============================
    //          EVENTOS
    // ==============================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGenericDamage(EntityDamageEvent e) {
        if (!e.getEntity().equals(bee)) return;

        if (isFinalDeath) {
            return;
        }

        if (isDying || isHibernating() || inRegenerationPhase) {
            e.setCancelled(true);
            return;
        }

        if (e.isCancelled()) return;

        if (bee.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);

            if (e instanceof EntityDamageByEntityEvent eventByEntity) {
                if (eventByEntity.getDamager() instanceof Player p) {
                    this.killer = p;
                } else if (eventByEntity.getDamager() instanceof Projectile proj
                        && proj.getShooter() instanceof Player p) {
                    this.killer = p;
                }
            }

            bee.setHealth(1);
            startDeathAnimation();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity damaged = e.getEntity();
        Entity damager = e.getDamager();

        if (!damaged.equals(bee)) return;

        if (damager instanceof Player p) {
            addAttacker(p);
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            addAttacker(p);
        }

        if (isFinalDeath) return;

        if (isDying) {
            e.setCancelled(true);
            return;
        }

        if (damager instanceof Player player) {
            if (player.getInventory().getItemInMainHand().getType() == Material.MACE) {
                e.setCancelled(true);
                player.playSound(bee.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
                floralBurst(bee.getLocation().add(0, 1, 0), 10, 0.6, 1.0f);
                return;
            }
        }

        // Los proyectiles mantienen su daño completo, como en OneBlock.
    }

    private void startDeathAnimation() {
        if (isDying) return;
        isDying = true;

        cancelCombat();

        // La musica se corta a todo el mundo en cuanto empieza a morir.
        detenerMusica();

        // OJO: el nombre NO se toca. El modelo va por el nombre y si se cambia
        // se ve como una abeja vanilla.
        bee.setAI(false);
        bee.setInvulnerable(true);
        bee.setGravity(false);
        bee.setGlowing(true);

        mainBar.removeAll();
        staticBar.removeAll();
        mainBar.setVisible(false);
        staticBar.setVisible(false);

        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.RECORDS, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!bee.isValid()) {
                    cancel();
                    onDeath();
                    return;
                }

                if (ticks >= 60) {
                    try {
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, bee.getLocation(), 5);
                        w.spawnParticle(Particle.FLASH, bee.getLocation(), 1, Color.WHITE);
                        floralBurst(bee.getLocation().add(0, 1, 0), 120, 2.0, 1.5f);
                        w.playSound(bee.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.6f);
                    } catch (Exception ignored) {
                    }

                    isFinalDeath = true;
                    bee.setInvulnerable(false);

                    if (killer != null && killer.isOnline()) {
                        bee.damage(10000, killer);
                    } else {
                        bee.setHealth(0);
                    }

                    finalizeDeath();
                    cancel();
                    return;
                }

                Location loc = bee.getLocation().add(0, 0.15, 0);
                double offsetX = (random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.2;
                loc.add(offsetX, 0, offsetZ);
                bee.teleport(loc);

                try {
                    floralBurst(bee.getLocation().add(0, 0.5, 0), 8, 0.5, 1.2f);
                } catch (Exception ignored) {
                }

                if (ticks % 10 == 0) {
                    w.playSound(bee.getLocation(), Sound.ENTITY_BEE_HURT, 2.0f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Las monedas y extras siguen en BossRewards bajo el ID histórico abeja_reina.
    private void finalizeDeath() {
        onDeath();
        ExperienceOrb orb = (ExperienceOrb) bee.getWorld().spawnEntity(
                bee.getLocation(), EntityType.EXPERIENCE_ORB);
        orb.setExperience(3500);
    }

    @EventHandler
    public void onEntityDamageExplosions(EntityDamageEvent e) {
        if (!e.getEntity().equals(bee)) return;

        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            e.setCancelled(true);
        }
    }
}