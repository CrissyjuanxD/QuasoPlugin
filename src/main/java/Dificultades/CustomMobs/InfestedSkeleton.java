package Dificultades.CustomMobs;

import Dificultades.Features.InfestedMob;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InfestedSkeleton extends InfestedMob implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;
    private static boolean eventsRegistered = false;

    public InfestedSkeleton(JavaPlugin plugin) {
        super(plugin, "infested_skeleton");
    }

    @Override
    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
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
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Skeleton spawnInfestedSkeleton(Location location) {
        Skeleton skeleton = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
        applyAttributes(skeleton);
        activeMobs.add(skeleton.getUniqueId());
        startGlobalParticleTask();
        return skeleton;
    }

    private void applyAttributes(Skeleton skeleton) {
        skeleton.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Infested Skeleton");
        skeleton.setCustomNameVisible(false);

        skeleton.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
        skeleton.setHealth(100);

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.POWER, 7);
        bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        bow.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
        skeleton.getEquipment().setItemInMainHand(bow);
        skeleton.getEquipment().setItemInMainHandDropChance(0);

        skeleton.getEquipment().setItemInOffHand(new ItemStack(Material.ARROW, 1));
        skeleton.getEquipment().setItemInOffHandDropChance(0);

        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1, false, false));
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));

        skeleton.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onSkeletonShoot(EntityShootBowEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || !arrow.isValid()) {
                    this.cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.SONIC_BOOM, arrow.getLocation(), 1, 0, 0, 0, 0);
                arrow.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, arrow.getLocation(), 1, 0.05, 0.05, 0.05, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        if (Math.random() < 0.3) {
            LivingEntity shooter = (LivingEntity) event.getEntity();
            Player nearest = findNearestPlayer(shooter.getLocation(), 35);
            if (nearest != null) {
                launchSonicBoom(shooter, nearest);
            }
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Skeleton shooter)) return;
        if (!isCustomMob(shooter)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1, false, true));

        target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.5f);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_WARDEN_HURT, 1.2f, 1.0f);
        event.getEntity().getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                event.getEntity().getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        Skeleton skeleton = (Skeleton) event.getEntity();
        event.getDrops().clear();

        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 1.0f);
        skeleton.getWorld().spawnParticle(Particle.SOUL, skeleton.getLocation(), 40, 0.8, 0.8, 0.8, 0.3);
        skeleton.getWorld().spawnParticle(Particle.SONIC_BOOM, skeleton.getLocation(), 5, 0.5, 0.5, 0.5, 0);

        activeMobs.remove(skeleton.getUniqueId());
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
                    entity.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                            entity.getLocation().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 0.02);
                    entity.getWorld().spawnParticle(Particle.SONIC_BOOM,
                            entity.getLocation(), 1, 0.2, 0.2, 0.2, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Skeleton &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}