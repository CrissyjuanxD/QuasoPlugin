package Dificultades.CustomMobs;

import Dificultades.Features.EnderMobs;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class EnderSpider extends EnderMobs implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    // Proyectiles de teletransporte
    private static final List<TeleportProjectileData> activeProjectiles = new ArrayList<>();
    private static BukkitTask projectileTask;
    private final Random random = new Random();

    private static class TeleportProjectileData {
        final BlockDisplay display;
        final Vector direction;
        final Spider shooter;
        int ticksAlive = 0;

        TeleportProjectileData(BlockDisplay display, Vector direction, Spider shooter) {
            this.display = display;
            this.direction = direction;
            this.shooter = shooter;
        }
    }

    public EnderSpider(JavaPlugin plugin) {
        super(plugin, "ender_spider");
    }

    @Override
    public void apply() {
        super.apply(); // Teleport al recibir daño (hereda lógica segura de EnderMobs)
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
            startProjectileTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isCustomMob(entity)) entity.remove();
                }
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            for (TeleportProjectileData pd : activeProjectiles) {
                pd.display.remove();
            }
            activeProjectiles.clear();
            if (projectileTask != null) {
                projectileTask.cancel();
                projectileTask = null;
            }
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Spider spawnEnderSpider(Location location) {
        Spider spider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyAttributes(spider);
        activeMobs.add(spider.getUniqueId());
        startGlobalParticleTask();
        return spider;
    }

    private void applyAttributes(Spider spider) {
        spider.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Spider");
        spider.setCustomNameVisible(false);

        // Araña normal tiene 8 salud y 2 de ataque; triplicamos el daño
        spider.getAttribute(Attribute.MAX_HEALTH).setBaseValue(50);
        spider.setHealth(50);
        spider.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(6.0);
        spider.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(48);

        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));
        spider.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));

        spider.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    // Al apuntar a un jugador: 25% chance de lanzar proyectil de teletransporte
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Spider spider)) return;
        if (!isCustomMob(spider)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        if (Math.random() < 0.25) {
            launchTeleportProjectile(spider, player);
        }
    }

    private void launchTeleportProjectile(Spider spider, Player target) {
        BlockDisplay projectile = (BlockDisplay) spider.getWorld().spawnEntity(
                spider.getEyeLocation(), EntityType.BLOCK_DISPLAY
        );
        projectile.setBlock(Material.PURPLE_STAINED_GLASS.createBlockData());
        projectile.setGlowing(true);
        projectile.setGlowColorOverride(Color.PURPLE);
        projectile.setInvulnerable(true);

        Transformation transformation = projectile.getTransformation();
        transformation.getScale().set(0.4f, 0.4f, 0.4f);
        projectile.setTransformation(transformation);

        Vector direction = target.getEyeLocation().toVector()
                .subtract(spider.getEyeLocation().toVector())
                .normalize()
                .multiply(0.8);

        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        spider.getWorld().spawnParticle(Particle.PORTAL, spider.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);

        activeProjectiles.add(new TeleportProjectileData(projectile, direction, spider));
    }

    private void startProjectileTask() {
        if (projectileTask != null && !projectileTask.isCancelled()) return;

        projectileTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeProjectiles.isEmpty()) return;

                Iterator<TeleportProjectileData> it = activeProjectiles.iterator();
                while (it.hasNext()) {
                    TeleportProjectileData pd = it.next();

                    if (pd.display.isDead() || !pd.display.isValid() || pd.ticksAlive >= 100) {
                        pd.display.remove();
                        it.remove();
                        continue;
                    }

                    pd.display.teleport(pd.display.getLocation().add(pd.direction));
                    pd.ticksAlive++;

                    // Trail portal
                    pd.display.getWorld().spawnParticle(Particle.PORTAL,
                            pd.display.getLocation(), 3, 0.1, 0.1, 0.1, 0.05);

                    // Colisión con jugadores
                    for (Entity nearby : pd.display.getNearbyEntities(1.0, 1.0, 1.0)) {
                        if (!(nearby instanceof Player hitPlayer)) continue;
                        if (hitPlayer.getGameMode() == GameMode.CREATIVE) continue;

                        handleTeleportHit(hitPlayer, pd.shooter);
                        pd.display.remove();
                        it.remove();
                        break; // Solo afecta a 1 jugador
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Al impactar: teletransporta al jugador a una posición segura
     * en un radio de 15 bloques alrededor de su ubicación actual.
     */
    private void handleTeleportHit(Player player, Spider spider) {
        if (!spider.isValid() || spider.isDead()) return;

        Location originalLoc = player.getLocation();
        World world = player.getWorld();

        // Buscar un sitio seguro para el jugador en un radio de 15
        Location targetLoc = findSafePlayerLocation(originalLoc, 15);

        // Si se encuentra un lugar seguro, teletransportarlo
        if (targetLoc != null) {
            // Efectos en posición original
            world.playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.0f);
            world.spawnParticle(Particle.PORTAL, originalLoc, 40, 0.5, 1, 0.5, 0.3);

            player.teleport(targetLoc);

            // Efectos en posición nueva
            world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.0f);
            world.spawnParticle(Particle.PORTAL, targetLoc, 40, 0.5, 1, 0.5, 0.3);
        }

        // Desorientación (aplica incluso si falló el TP)
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
    }

    // Busca un lugar seguro cercano para no ahogar al jugador ni enviarlo encima de la bedrock
    private Location findSafePlayerLocation(Location currentLoc, int radius) {
        World world = currentLoc.getWorld();

        for (int i = 0; i < 15; i++) { // Intenta encontrar un spot válido 15 veces
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetY = random.nextInt(radius) - (radius / 2); // Busca un poco arriba y abajo
            int offsetZ = random.nextInt(radius * 2) - radius;

            Location target = currentLoc.clone().add(offsetX, offsetY, offsetZ);

            // Previene que se salga de los límites del mundo
            if (target.getY() < world.getMinHeight() || target.getY() >= world.getMaxHeight()) {
                continue;
            }

            Block ground = target.clone().subtract(0, 1, 0).getBlock();
            Block feet = target.getBlock();
            Block head = target.clone().add(0, 1, 0).getBlock();

            // Verifica que el suelo sea sólido, QUE NO SEA BEDROCK, y que el cuerpo/cabeza estén libres (sin lava/agua)
            if (ground.getType().isSolid() && ground.getType() != Material.BEDROCK &&
                    feet.isPassable() && feet.getType() != Material.WATER && feet.getType() != Material.LAVA &&
                    head.isPassable() && head.getType() != Material.WATER && head.getType() != Material.LAVA) {

                // Centramos al jugador en el bloque respetando su cámara
                return new Location(world, target.getBlockX() + 0.5, target.getBlockY(), target.getBlockZ() + 0.5, currentLoc.getYaw(), currentLoc.getPitch());
            }
        }
        return null; // Si no encuentra un lugar seguro después de 15 intentos, devuelve null
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_SPIDER_HURT, 1.5f, 0.7f);
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.8f);
        event.getEntity().getWorld().spawnParticle(Particle.PORTAL,
                event.getEntity().getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Spider spider = (Spider) event.getEntity();
        event.getDrops().clear();

        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 2.0f, 0.7f);
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.7f);
        spider.getWorld().spawnParticle(Particle.PORTAL, spider.getLocation(), 80, 1, 1, 1, 0.4);

        activeMobs.remove(spider.getUniqueId());
    }

    private void startGlobalParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) return;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMobs.isEmpty()) return;
                Iterator<UUID> it = activeMobs.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }
                    entity.getWorld().spawnParticle(Particle.PORTAL,
                            entity.getLocation().add(0, 0.5, 0), 4, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Spider &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}