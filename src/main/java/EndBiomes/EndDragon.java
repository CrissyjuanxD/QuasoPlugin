package EndBiomes;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// El Ender Dragon no está cuando se entra al End: hay que invocarlo poniendo los 4 cristales del End en los costados
// del portal de salida (como para revivirlo en vanilla). El dragón que el juego crea solo se cancela y la pelea queda
// como "dragón muerto, nunca matado", así el primero que se invoque da el huevo y los 12000 de experiencia. Mientras el
// juego hace el ritual de siempre (las columnas, los rayos) acá se le suma una animación de partículas de colores
public class EndDragon implements Listener {

    // Colores del ritual: magenta, violeta, cian, dorado, rosa y blanco
    static final Color[] COLORES = {
            Color.fromRGB(255, 79, 216), Color.fromRGB(155, 92, 255), Color.fromRGB(79, 227, 255),
            Color.fromRGB(255, 210, 90), Color.fromRGB(255, 140, 198), Color.fromRGB(255, 255, 255)};

    private final JavaPlugin plugin;
    private final NamespacedKey invocado;
    private final EndIslaPrincipal isla;
    private BukkitTask task;
    private long tick;
    // Último tick en que se vio el ritual andando y desde cuándo está en la fase actual
    private long ritualVisto = -1000;
    private DragonBattle.RespawnPhase fase = DragonBattle.RespawnPhase.NONE;
    private long faseDesde;

    public EndDragon(JavaPlugin plugin, EndIslaPrincipal isla) {
        this.plugin = plugin;
        this.invocado = new NamespacedKey(plugin, "dragon_invocado");
        this.isla = isla;
    }

    public void start() {
        for (World world : Bukkit.getWorlds()) preparar(world);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    // Si el End ya se había visitado y el dragón que puso el juego sigue vivo, se saca y la pelea queda lista para
    // invocarlo
    private void preparar(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) return;
        EnderDragon dragon = battle.getEnderDragon();
        if (dragon != null && !esInvocado(dragon) && !battle.hasBeenPreviouslyKilled()
                && battle.getRespawnPhase() == DragonBattle.RespawnPhase.NONE && marcarMuerto(battle)) {
            dragon.remove();
            plugin.getLogger().info("Se sacó el Ender Dragon que no fue invocado: ahora hay que invocarlo con los cristales.");
        }
        if (battle.getEndPortalLocation() != null) isla.plaza(world, battle.getEndPortalLocation());
    }

    // El dragón que crea el juego solo (al entrar por primera vez) no sale. El de los cristales sí, con su animación
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof EnderDragon dragon)) return;
        World world = dragon.getWorld();
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) return;
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.COMMAND || reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;

        if (deRitual(battle.getRespawnPhase(), tick - ritualVisto)) {
            dragon.getPersistentDataContainer().set(invocado, PersistentDataType.BYTE, (byte) 1);
            Bukkit.getScheduler().runTask(plugin, () -> despertar(dragon));
            return;
        }
        if (battle.hasBeenPreviouslyKilled() || !marcarMuerto(battle)) return;
        e.setCancelled(true);
        if (battle.getEndPortalLocation() != null) {
            Location portal = battle.getEndPortalLocation();
            Bukkit.getScheduler().runTask(plugin, () -> isla.plaza(world, portal));
        }
    }

    // Un dragón es del ritual si el juego está en una fase del ritual o se vio el ritual hace menos de 10 segundos
    static boolean deRitual(DragonBattle.RespawnPhase phase, long ticksDesdeRitual) {
        return phase != DragonBattle.RespawnPhase.NONE || ticksDesdeRitual < 200;
    }

    // Los cristales nuevos que el ritual pone arriba de cada columna: espiral de colores que sube y estallido arriba
    @EventHandler(ignoreCancelled = true)
    public void onCrystal(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal crystal)) return;
        DragonBattle battle = crystal.getWorld().getEnderDragonBattle();
        if (battle == null || battle.getRespawnPhase() != DragonBattle.RespawnPhase.SUMMONING_PILLARS) return;
        Location top = crystal.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> columna(top));
    }

    private boolean esInvocado(EnderDragon dragon) {
        return dragon.getPersistentDataContainer().has(invocado, PersistentDataType.BYTE);
    }

    // Deja la pelea como "el dragón está muerto" sin contarlo como matado (el huevo y la experiencia quedan para el
    // primero que se invoque). La API no tiene cómo, así que se cambia el campo de la pelea; si en otra versión no
    // existe, el dragón sale como siempre
    private boolean marcarMuerto(DragonBattle battle) {
        try {
            Field handleField = battle.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            Object fight = handleField.get(battle);
            Field killed = fight.getClass().getDeclaredField("dragonKilled");
            killed.setAccessible(true);
            killed.setBoolean(fight, true);
            fight.getClass().getMethod("setDirty").invoke(fight);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("No se pudo dejar el End sin dragón (" + ex + "): el dragón sale como siempre.");
            return false;
        }
    }

    // ---------------------------------------------------------------- Animación del ritual

    private void tick() {
        tick++;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle == null) continue;
            DragonBattle.RespawnPhase phase = battle.getRespawnPhase();
            // Cuando sale el dragón del ritual el juego ya volvió a "sin ritual": por eso se anota cuándo se lo vio
            if (phase != DragonBattle.RespawnPhase.NONE) ritualVisto = tick;
            if (world.getPlayers().isEmpty() || battle.getEndPortalLocation() == null) continue;
            Location portal = battle.getEndPortalLocation().clone().add(0.5, 0, 0.5);
            if (phase != fase) {
                fase = phase;
                faseDesde = tick;
                if (phase == DragonBattle.RespawnPhase.START) inicio(world, portal);
            }
            long t = tick - faseDesde;
            switch (phase) {
                case START -> {
                    pulso(portal, t, 24);
                    cristales(battle.getRespawnCrystals(), t);
                    centro(portal, t);
                }
                case PREPARING_TO_SUMMON_PILLARS -> {
                    pulso(portal, t, 16);
                    rayo(portal, t);
                    if (t % 12 == 0) anillo(portal.clone().add(0, 1.2, 0), 0, 22, t);
                }
                case SUMMONING_PILLARS -> {
                    pulso(portal, t, 10);
                    rayo(portal, t);
                }
                case SUMMONING_DRAGON, END -> {
                    pulso(portal, t, 5);
                    helice(portal, t);
                    vortice(portal.clone().add(0, 128 - portal.getY(), 0), t);
                }
                default -> {
                    if (battle.getEnderDragon() == null && tick % 10 == 0) pistas(world, portal);
                }
            }
        }
    }

    private void inicio(World world, Location portal) {
        world.playSound(portal, Sound.BLOCK_BEACON_ACTIVATE, 3f, 0.6f);
        world.playSound(portal, Sound.BLOCK_END_PORTAL_SPAWN, 2f, 1.4f);
        for (Player p : world.getPlayers()) {
            p.sendTitle(ChatColor.of("#c58cff") + "✦ Ritual del Dragón ✦", ChatColor.of("#ff8cc6") + "Los cristales despiertan al Ender Dragon", 10, 60, 20);
        }
    }

    // Anillos de colores que se cierran hacia el portal; cada fase los manda más seguido
    private void pulso(Location portal, long t, int cada) {
        long fase = t % cada;
        double radius = 18 * (1 - fase / (double) cada);
        if (radius < 0.6) return;
        anilloEn(portal.clone().add(0, 0.6, 0), radius, t);
    }

    private void anilloEn(Location center, double radius, long t) {
        World world = center.getWorld();
        int points = (int) Math.max(12, radius * 4);
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points + t * 0.05;
            Location p = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, polvo(i * COLORES.length / points, 1.4f));
        }
    }

    // Anillo que se abre desde el centro hasta el radio dado
    private void anillo(Location center, double from, double to, long t) {
        new org.bukkit.scheduler.BukkitRunnable() {
            double r = from;

            @Override
            public void run() {
                if (r > to) {
                    cancel();
                    return;
                }
                anilloEn(center, r, t);
                r += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Espirales que suben alrededor de cada cristal del portal
    private void cristales(List<EnderCrystal> crystals, long t) {
        for (EnderCrystal crystal : crystals) {
            Location base = crystal.getLocation();
            World world = base.getWorld();
            for (int s = 0; s < 2; s++) {
                double a = t * 0.35 + s * Math.PI;
                double h = (t % 40) / 40.0 * 3.5;
                Location p = base.clone().add(Math.cos(a) * 0.9, h, Math.sin(a) * 0.9);
                world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, polvo((int) (t / 6 + s * 2), 1.2f));
            }
            if (t % 4 == 0) world.spawnParticle(Particle.END_ROD, base.clone().add(0, 1.2, 0), 1, 0.3, 0.4, 0.3, 0.01);
        }
    }

    private void centro(Location portal, long t) {
        World world = portal.getWorld();
        Location c = portal.clone().add(0, 3, 0);
        world.spawnParticle(Particle.ENCHANT, c, 6, 0.6, 0.6, 0.6, 1.2);
        world.spawnParticle(Particle.REVERSE_PORTAL, c, 3, 0.3, 0.5, 0.3, 0.02);
        if (t % 3 == 0) world.spawnParticle(Particle.PORTAL, c, 10, 0.8, 0.8, 0.8, 0.8);
    }

    // Columna de luz de colores que sube del portal
    private void rayo(Location portal, long t) {
        World world = portal.getWorld();
        for (int i = 0; i < 8; i++) {
            double h = 2 + ((t * 2 + i * 6) % 48);
            double a = t * 0.2 + i;
            Location p = portal.clone().add(Math.cos(a) * 0.5, h, Math.sin(a) * 0.5);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, polvo(i + (int) (t / 5), 1.6f));
        }
        if (t % 5 == 0) world.spawnParticle(Particle.END_ROD, portal.clone().add(0, 3, 0), 2, 0.2, 2, 0.2, 0.05);
    }

    // Cuando el ritual arma una columna: espiral doble que sube de la base al cristal, arco desde el portal y estallido
    private void columna(Location top) {
        World world = top.getWorld();
        int ground = world.getHighestBlockYAt(top.getBlockX() + 6, top.getBlockZ());
        double baseY = Math.min(top.getY() - 4, Math.max(ground, 50));
        DragonBattle battle = world.getEnderDragonBattle();
        Location portal = battle != null && battle.getEndPortalLocation() != null ? battle.getEndPortalLocation().clone().add(0.5, 3, 0.5) : null;
        new org.bukkit.scheduler.BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step > 24) {
                    cancel();
                    estallido(top, 1);
                    return;
                }
                double h = baseY + (top.getY() - baseY) * step / 24.0;
                for (int s = 0; s < 2; s++) {
                    double a = step * 0.6 + s * Math.PI;
                    Location p = new Location(world, top.getX() + Math.cos(a) * 3.2, h, top.getZ() + Math.sin(a) * 3.2);
                    world.spawnParticle(Particle.DUST, p, 2, 0.05, 0.05, 0.05, 0, polvo(step / 4 + s * 3, 1.8f));
                }
                if (portal != null) {
                    double f = step / 24.0;
                    Vector d = top.toVector().subtract(portal.toVector());
                    Location p = portal.clone().add(d.clone().multiply(f)).add(0, Math.sin(f * Math.PI) * 14, 0);
                    world.spawnParticle(Particle.DUST, p, 3, 0.1, 0.1, 0.1, 0, polvo(step / 3, 2f));
                    world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        world.playSound(top, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 4f, 0.8f);
    }

    // Hélice doble que sube del portal hasta donde aparece el dragón
    private void helice(Location portal, long t) {
        World world = portal.getWorld();
        double total = 128 - portal.getY();
        double h = (t * 2.2) % total;
        for (int s = 0; s < 2; s++) {
            double a = t * 0.26 + s * Math.PI;
            Location p = portal.clone().add(Math.cos(a) * 1.4, h + 3, Math.sin(a) * 1.4);
            world.spawnParticle(Particle.DUST, p, 2, 0.05, 0.05, 0.05, 0, polvo((int) (t / 3) + s * 3, 2f));
            world.spawnParticle(Particle.DRAGON_BREATH, p, 1, 0, 0, 0, 0.01, 1.0f);
        }
    }

    // Esfera que gira y se va cerrando donde aparece el dragón
    private void vortice(Location center, long t) {
        if (t % 2 != 0) return;
        World world = center.getWorld();
        double radius = Math.max(1.2, 9 - (t % 100) * 0.08);
        for (int i = 0; i < 26; i++) {
            double phi = Math.acos(1 - 2 * (i + 0.5) / 26);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i + t * 0.12;
            Location p = center.clone().add(Math.cos(theta) * Math.sin(phi) * radius, Math.cos(phi) * radius, Math.sin(theta) * Math.sin(phi) * radius);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, polvo(i, 1.8f));
        }
    }

    // El dragón invocado aparece: explosión de colores, abanico de luces, sonido y aviso a todo el server
    private void despertar(EnderDragon dragon) {
        Location c = dragon.getLocation();
        World world = c.getWorld();
        estallido(c, 3);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, c, 6, 3, 3, 3, 0);
        for (int arm = 0; arm < 10; arm++) {
            for (int i = 0; i < 30; i++) {
                double a = arm * (2 * Math.PI / 10) + i * 0.08;
                double r = i * 0.5;
                Location p = c.clone().add(Math.cos(a) * r, -i * 0.15, Math.sin(a) * r);
                world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, polvo(arm, 2.2f));
                if (i % 5 == 0) world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0.02);
            }
        }
        world.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 10f, 0.7f);
        world.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10f, 0.6f);
        for (Player p : world.getPlayers()) {
            p.sendTitle(ChatColor.of("#9b5cff") + "" + net.md_5.bungee.api.ChatColor.BOLD + "EL ENDER DRAGON DESPERTÓ",
                    ChatColor.of("#ff8cc6") + "Rompe los cristales de las torres", 10, 70, 20);
        }
        Bukkit.broadcastMessage(ChatColor.of("#c58cff") + "۞ " + ChatColor.of("#ff4fd8") + "El Ender Dragon fue invocado en el End.");
    }

    private void estallido(Location c, double size) {
        World world = c.getWorld();
        for (int i = 0; i < 60 * size; i++) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            Vector v = new Vector(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().multiply(r.nextDouble() * 2.5 * size);
            world.spawnParticle(Particle.DUST, c.clone().add(v), 1, 0, 0, 0, 0, polvo(i, 1.8f));
        }
        world.spawnParticle(Particle.FIREWORK, c, (int) (40 * size), 0.6 * size, 0.6 * size, 0.6 * size, 0.15);
        world.spawnParticle(Particle.END_ROD, c, (int) (20 * size), 0.4 * size, 0.4 * size, 0.4 * size, 0.12);
        world.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 4f, 1f);
    }

    // Sin dragón: un brillo en los 4 costados del portal donde van los cristales
    private void pistas(World world, Location portal) {
        boolean cerca = false;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(portal) < 48 * 48) cerca = true;
        }
        if (!cerca) return;
        int[][] lados = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
        for (int i = 0; i < lados.length; i++) {
            Location spot = portal.clone().add(lados[i][0], 1.3, lados[i][1]);
            if (!world.getNearbyEntitiesByType(EnderCrystal.class, spot, 1.2).isEmpty()) continue;
            world.spawnParticle(Particle.DUST, spot, 3, 0.2, 0.15, 0.2, 0, polvo(i + (int) (tick / 10), 1.1f));
            world.spawnParticle(Particle.WAX_OFF, spot, 1, 0.3, 0.1, 0.3, 0.1);
        }
    }

    private static Particle.DustOptions polvo(int index, float size) {
        return new Particle.DustOptions(COLORES[Math.floorMod(index, COLORES.length)], size);
    }
}
