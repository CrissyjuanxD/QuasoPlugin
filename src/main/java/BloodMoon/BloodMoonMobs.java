package BloodMoon;

import net.md_5.bungee.api.ChatColor;
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
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Los 4 mobs de BloodMoon: durante la BloodMoon parte de los zombies, esqueletos, arañas y creepers que salen solos (y
// de las hordas) salen como mobs de BloodMoon según la dificultad (5%, 25% o 50%). Todos hacen Sangrado (1 de daño por
// segundo que atraviesa la armadura). Son los que sueltan los fragmentos de BloodMoon (OneChanges)
public final class BloodMoonMobs implements Listener {

    public static final NamespacedKey KEY = new NamespacedKey("quasoplugin", "bloodmoon_mob");
    private static final NamespacedKey ARROW_KEY = new NamespacedKey("quasoplugin", "bloodmoon_flecha");
    static final String COLOR = "#E05555";
    private static final Color SANGRE = Color.fromRGB(0x9E1B1B);
    private static final Particle.DustOptions GOTA = new Particle.DustOptions(SANGRE, 1.1f);
    private static final Particle.DustOptions GOTA_GRANDE = new Particle.DustOptions(Color.fromRGB(0xC22B2B), 1.6f);

    public enum Tipo {
        ZOMBIE("zombiebloodmoon", "Zombie BloodMoon", EntityType.ZOMBIE),
        ESQUELETO("esqueletobloodmoon", "Esqueleto BloodMoon", EntityType.SKELETON),
        ARANA("aranabloodmoon", "Araña BloodMoon", EntityType.SPIDER),
        CREEPER("creeperbloodmoon", "Creeper BloodMoon", EntityType.CREEPER);

        private final String id;
        private final String nombre;
        private final EntityType base;

        Tipo(String id, String nombre, EntityType base) {
            this.id = id;
            this.nombre = nombre;
            this.base = base;
        }

        public String id() {
            return id;
        }

        public String nombre() {
            return nombre;
        }

        public EntityType base() {
            return base;
        }

        // El tipo de BloodMoon de un mob vanilla, o null si no tiene
        public static Tipo de(EntityType type) {
            for (Tipo tipo : values()) if (tipo.base == type) return tipo;
            return null;
        }

        public static Tipo porId(String id) {
            for (Tipo tipo : values()) if (tipo.id.equalsIgnoreCase(id)) return tipo;
            return null;
        }
    }

    // Cada cuánto (en ticks) pueden usar su ataque especial
    static final long RAFAGA_TICKS = 140;
    static final long SALTO_TICKS = 100;
    // El zombie entra en frenesí cuando le queda menos de este tanto de vida
    static final double FRENESI = 0.4;

    private static BloodMoonMobs instancia;

    private final JavaPlugin plugin;
    private final Map<UUID, LivingEntity> activos = new HashMap<>();
    private final Map<UUID, Long> proximo = new HashMap<>();
    private final Set<UUID> frenesi = new HashSet<>();
    private final Map<UUID, Integer> sangrado = new HashMap<>();
    private BukkitTask task;
    private long tick;

    public BloodMoonMobs(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static BloodMoonMobs instancia() {
        return instancia;
    }

    public void start() {
        instancia = this;
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) if (es(entity)) activos.put(entity.getUniqueId(), entity);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 10L, 10L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        activos.clear();
        proximo.clear();
        frenesi.clear();
        sangrado.clear();
        if (instancia == this) instancia = null;
    }

    // ---------------------------------------------------------------- Quién es mob de BloodMoon

    public static Tipo tipo(Entity entity) {
        if (entity == null) return null;
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (data == null) return null;
        String id = data.get(KEY, PersistentDataType.STRING);
        return id == null ? null : Tipo.porId(id);
    }

    public static boolean es(Entity entity) {
        return tipo(entity) != null;
    }

    // Lo usa /spawnqp: sale ya convertido
    public static LivingEntity spawn(Tipo tipo, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, tipo.base());
        convertir(entity, tipo);
        return entity;
    }

    // Tirada de la dificultad para un mob que acaba de salir. Solo zombies adultos, esqueletos, arañas y creepers
    // vanilla (sin datos de otro mob custom, por ejemplo los florales del cambio extra)
    public boolean quizas(LivingEntity entity, BloodMoonDificultad dificultad) {
        Tipo tipo = Tipo.de(entity.getType());
        if (tipo == null || dificultad == null) return false;
        if (entity instanceof Zombie zombie && !zombie.isAdult()) return false;
        if (!entity.getPersistentDataContainer().isEmpty()) return false;
        if (ThreadLocalRandom.current().nextInt(100) >= dificultad.probabilidadMob()) return false;
        convertir(entity, tipo);
        return true;
    }

    // Vida, daño, velocidad, nombre y ropa de cada uno. Se convierte ahí mismo, así la mobcap cuenta igual
    public static void convertir(LivingEntity entity, Tipo tipo) {
        entity.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, tipo.id());
        entity.setCustomName(ChatColor.of(COLOR) + "" + ChatColor.BOLD + tipo.nombre());
        entity.setCustomNameVisible(false);
        switch (tipo) {
            case ZOMBIE -> {
                stats(entity, 36, 6, 0.27);
                atributo(entity, Attribute.ARMOR, 4);
                atributo(entity, Attribute.KNOCKBACK_RESISTANCE, 0.3);
                ropa(entity, true);
            }
            case ESQUELETO -> {
                stats(entity, 30, 0, 0.27);
                ropa(entity, false);
            }
            case ARANA -> {
                stats(entity, 30, 5, 0.34);
                atributo(entity, Attribute.SCALE, 1.15);
            }
            case CREEPER -> {
                stats(entity, 28, 0, 0.3);
                Creeper creeper = (Creeper) entity;
                creeper.setMaxFuseTicks(22);
                creeper.setExplosionRadius(4);
            }
        }
        atributo(entity, Attribute.FOLLOW_RANGE, 40);
        if (instancia != null) instancia.activos.put(entity.getUniqueId(), entity);
    }

    private static void stats(LivingEntity entity, double vida, double dano, double velocidad) {
        atributo(entity, Attribute.MAX_HEALTH, vida);
        entity.setHealth(vida);
        if (dano > 0) atributo(entity, Attribute.ATTACK_DAMAGE, dano);
        atributo(entity, Attribute.MOVEMENT_SPEED, velocidad);
    }

    private static void atributo(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    // Casco (y peto el zombie) de cuero rojo sangre que no se suelta
    private static void ropa(LivingEntity entity, boolean peto) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(cuero(Material.LEATHER_HELMET));
        equipment.setHelmetDropChance(0f);
        if (peto) {
            equipment.setChestplate(cuero(Material.LEATHER_CHESTPLATE));
            equipment.setChestplateDropChance(0f);
        }
    }

    private static ItemStack cuero(Material material) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(0x7A0F12));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ---------------------------------------------------------------- Spawn natural

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        BloodMoonDificultad dificultad = dificultadActiva(event.getLocation().getWorld());
        if (dificultad != null) quizas(event.getEntity(), dificultad);
    }

    // La dificultad del mundo si tiene una BloodMoon activa; si no, null
    static BloodMoonDificultad dificultadActiva(World world) {
        BloodMoon bloodMoon = BloodMoon.GetInstance();
        if (world == null || bloodMoon == null || !bloodMoon.isActive(world)) return null;
        ConfigReader config = bloodMoon.getConfigReader(world);
        return config == null ? null : config.GetDifficultyConfig();
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living && es(living)) activos.put(living.getUniqueId(), living);
        }
    }

    // ---------------------------------------------------------------- Ataques

    // Cada medio segundo: gotas de sangre, frenesí del zombie, ráfaga del esqueleto y salto de la araña. Cada segundo
    // el Sangrado de los jugadores
    private void update() {
        tick += 10;
        Iterator<Map.Entry<UUID, LivingEntity>> it = activos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            LivingEntity mob = entry.getValue();
            if (!mob.isValid() || mob.isDead()) {
                it.remove();
                proximo.remove(entry.getKey());
                frenesi.remove(entry.getKey());
                continue;
            }
            if (tick % 20 == 0) mob.getWorld().spawnParticle(Particle.DUST, mob.getLocation().add(0, mob.getHeight() * 0.6, 0), 2, 0.25, 0.3, 0.25, 0, GOTA);
            Tipo tipo = tipo(mob);
            if (tipo == null) continue;
            switch (tipo) {
                case ZOMBIE -> {
                    double max = mob.getAttribute(Attribute.MAX_HEALTH).getValue();
                    if (mob.getHealth() < max * FRENESI && frenesi.add(mob.getUniqueId())) frenesi(mob);
                }
                case ESQUELETO -> {
                    Player target = objetivo(mob, 24);
                    if (target != null && tick >= proximo.getOrDefault(mob.getUniqueId(), 0L) && mob.hasLineOfSight(target)) {
                        proximo.put(mob.getUniqueId(), tick + RAFAGA_TICKS);
                        rafaga((Skeleton) mob, target);
                    }
                }
                case ARANA -> {
                    Player target = objetivo(mob, 12);
                    if (target == null || !mob.isOnGround() || tick < proximo.getOrDefault(mob.getUniqueId(), 0L)) continue;
                    if (mob.getLocation().distanceSquared(target.getLocation()) < 9) continue;
                    proximo.put(mob.getUniqueId(), tick + SALTO_TICKS);
                    salto((Spider) mob, target);
                }
                default -> { }
            }
        }
        if (tick % 20 == 0) sangrar();
    }

    private static Player objetivo(LivingEntity mob, double rango) {
        if (!(mob instanceof Mob m) || !(m.getTarget() instanceof Player player) || !jugable(player)) return null;
        if (!player.getWorld().equals(mob.getWorld()) || player.getLocation().distanceSquared(mob.getLocation()) > rango * rango) return null;
        return player;
    }

    private static boolean jugable(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    // Zombie con poca vida: ruge, se pone rojo y queda con Velocidad II y Fuerza I hasta que muere
    private void frenesi(LivingEntity zombie) {
        World world = zombie.getWorld();
        Location at = zombie.getLocation().add(0, 1, 0);
        world.playSound(at, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 1.5f);
        world.spawnParticle(Particle.DUST, at, 30, 0.5, 0.7, 0.5, 0, GOTA_GRANDE);
        world.spawnParticle(Particle.ANGRY_VILLAGER, at.add(0, 1, 0), 3, 0.3, 0.2, 0.3, 0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, true));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, true));
    }

    // Esqueleto: tres flechas en abanico que hacen Sangrado
    private void rafaga(Skeleton skeleton, Player target) {
        Location eye = skeleton.getEyeLocation();
        Vector base = target.getEyeLocation().toVector().subtract(eye.toVector());
        double distance = base.length();
        base.normalize();
        skeleton.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 1.2f, 0.7f);
        skeleton.getWorld().playSound(eye, Sound.ITEM_CROSSBOW_SHOOT, 0.8f, 0.6f);
        for (int i = -1; i <= 1; i++) {
            Vector dir = base.clone().rotateAroundY(Math.toRadians(i * 9)).multiply(1.7).add(new Vector(0, 0.012 * distance, 0));
            Arrow arrow = skeleton.launchProjectile(Arrow.class, dir);
            arrow.setDamage(2.5);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            marcar(arrow);
        }
    }

    private static void marcar(Arrow arrow) {
        arrow.getPersistentDataContainer().set(ARROW_KEY, PersistentDataType.BYTE, (byte) 1);
        arrow.setColor(SANGRE);
    }

    // Araña: salta hacia su objetivo; si cae encima le pega, le hace Sangrado y lo deja lento
    private void salto(Spider spider, Player target) {
        Vector dir = target.getLocation().toVector().subtract(spider.getLocation().toVector()).setY(0);
        double distance = dir.length();
        dir.normalize().multiply(Math.min(1.5, 0.45 + distance * 0.11)).setY(0.48);
        spider.setVelocity(dir);
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.2f, 0.6f);
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (!spider.isValid() || spider.isDead() || !target.isOnline() || t > 30) {
                    cancel();
                    return;
                }
                spider.getWorld().spawnParticle(Particle.DUST, spider.getLocation().add(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0, GOTA);
                if (t > 3 && spider.getLocation().distanceSquared(target.getLocation()) < 2.6 * 2.6) {
                    target.damage(5, spider);
                    sangrado(target, 3);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPIDER_HURT, 1f, 0.5f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Golpes: el zombie se cura 3 al morder y hace Sangrado 4 s; la araña Sangrado 3 s; las flechas del esqueleto 4 s
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !jugable(player)) return;
        Entity damager = event.getDamager();
        if (damager instanceof Arrow arrow && arrow.getPersistentDataContainer().has(ARROW_KEY, PersistentDataType.BYTE)) {
            sangrado(player, 4);
            return;
        }
        Tipo tipo = tipo(damager);
        if (tipo == Tipo.ZOMBIE && damager instanceof LivingEntity zombie) {
            sangrado(player, 4);
            double max = zombie.getAttribute(Attribute.MAX_HEALTH).getValue();
            zombie.setHealth(Math.min(max, zombie.getHealth() + 3));
            zombie.getWorld().spawnParticle(Particle.DUST, zombie.getLocation().add(0, 1.2, 0), 8, 0.3, 0.4, 0.3, 0, GOTA_GRANDE);
        } else if (tipo == Tipo.ARANA) {
            sangrado(player, 3);
        }
    }

    // Todas las flechas que tira el esqueleto con su arco también hacen Sangrado
    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (tipo(event.getEntity()) == Tipo.ESQUELETO && event.getProjectile() instanceof Arrow arrow) marcar(arrow);
    }

    // Creeper: explota más grande pero sin romper bloques, y deja un charco de sangre que hace Sangrado 6 s, Debilidad
    // y Lentitud a los que estén cerca
    @EventHandler(ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (tipo(event.getEntity()) == Tipo.CREEPER) event.setFire(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (tipo(event.getEntity()) != Tipo.CREEPER) return;
        event.blockList().clear();
        Location at = event.getLocation();
        World world = at.getWorld();
        world.spawnParticle(Particle.DUST, at.clone().add(0, 1, 0), 120, 2.2, 1.2, 2.2, 0, GOTA_GRANDE);
        world.spawnParticle(Particle.BLOCK, at, 60, 1.5, 0.3, 1.5, 0, Material.REDSTONE_BLOCK.createBlockData());
        world.playSound(at, Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.6f);
        for (Player player : world.getPlayers()) {
            if (!jugable(player) || player.getLocation().distanceSquared(at) > 36) continue;
            sangrado(player, 6);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
        }
        world.spawn(at, AreaEffectCloud.class, cloud -> {
            cloud.setRadius(3f);
            cloud.setDuration(100);
            cloud.setRadiusPerTick(-0.01f);
            cloud.setWaitTime(0);
            cloud.setReapplicationDelay(20);
            cloud.setColor(SANGRE);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0), true);
        });
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (!es(mob)) return;
        activos.remove(mob.getUniqueId());
        proximo.remove(mob.getUniqueId());
        frenesi.remove(mob.getUniqueId());
        mob.getWorld().spawnParticle(Particle.DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 25, 0.4, 0.5, 0.4, 0, GOTA_GRANDE);
        mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, 1f, 0.5f);
    }

    // ---------------------------------------------------------------- Sangrado

    // Si ya sangra se queda con la duración más larga
    public void sangrado(Player player, int segundos) {
        if (!jugable(player)) return;
        boolean nuevo = !sangrado.containsKey(player.getUniqueId());
        sangrado.merge(player.getUniqueId(), segundos, Math::max);
        if (nuevo) player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1f, 0.6f);
    }

    public int sangrado(Player player) {
        return sangrado.getOrDefault(player.getUniqueId(), 0);
    }

    // 1 de daño mágico por segundo (la armadura no lo para, Protección sí un poco) con gotas que caen
    private void sangrar() {
        Iterator<Map.Entry<UUID, Integer>> it = sangrado.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || player.isDead() || !jugable(player) || entry.getValue() <= 0) {
                it.remove();
                continue;
            }
            player.damage(1.0, DamageSource.builder(DamageType.MAGIC).build());
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 6, 0.25, 0.4, 0.25, 0, GOTA);
            entry.setValue(entry.getValue() - 1);
        }
    }
}
