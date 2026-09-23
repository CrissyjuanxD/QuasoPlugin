package Dificultades.CustomMobs;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import Dificultades.Features.InfestedMob;
import Dificultades.Features.MobSoundManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WardenZombie extends InfestedMob implements Listener {

    private static final double ECHO_CHANCE = 0.15;
    private static final double HEARING_RADIUS = 16.0;
    private static final Color SCULK_COLOR = Color.fromRGB(0x0E3B43);

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static boolean eventsRegistered = false;
    private static BukkitTask hearingTask;

    private final Random random = new Random();

    public WardenZombie(JavaPlugin plugin) {
        super(plugin, "warden_zombie");
    }

    @Override
    public void apply() {
        MobSoundManager.register(mobKey, Sound.ENTITY_WARDEN_AMBIENT, Sound.ENTITY_WARDEN_STEP, 1.3f, 0.8f);
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            scanExisting();
            startHearingTask();
        }
    }

    public void revert() {
        if (!eventsRegistered) return;
        HandlerList.unregisterAll(this);
        if (hearingTask != null) {
            hearingTask.cancel();
            hearingTask = null;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (isCustomMob(zombie)) zombie.remove();
            }
        }
        activeMobs.clear();
        eventsRegistered = false;
    }

    public Zombie spawnWardenZombie(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class, this::applyAttributes);
        activeMobs.add(zombie.getUniqueId());
        return zombie;
    }

    // Convierte un zombie que ya existe (por ejemplo el que spawnea el juego) en Warden Zombie
    public void infest(Zombie zombie) {
        applyAttributes(zombie);
        activeMobs.add(zombie.getUniqueId());
    }

    // Lento pero con mucha vida y armadura, un chillador en la cabeza y armadura de cuero color sculk
    private void applyAttributes(Zombie zombie) {
        zombie.setAdult();
        zombie.setShouldBurnInDay(false);
        zombie.setCanPickupItems(false);
        zombie.setCustomName(ChatColor.of("#1f7a86") + "" + ChatColor.BOLD + "Warden Zombie");
        zombie.setCustomNameVisible(false);

        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(60);
        zombie.setHealth(60);
        zombie.getAttribute(Attribute.ARMOR).setBaseValue(6);
        zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.17);
        zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(7);
        zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
        zombie.getAttribute(Attribute.SPAWN_REINFORCEMENTS).setBaseValue(0);

        EntityEquipment equipment = zombie.getEquipment();
        equipment.setHelmet(new ItemStack(Material.SCULK_SHRIEKER));
        equipment.setChestplate(dyed(Material.LEATHER_CHESTPLATE));
        equipment.setLeggings(dyed(Material.LEATHER_LEGGINGS));
        equipment.setBoots(dyed(Material.LEATHER_BOOTS));
        equipment.setItemInMainHand(null);
        equipment.setHelmetDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setLeggingsDropChance(0);
        equipment.setBootsDropChance(0);

        zombie.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    private ItemStack dyed(Material type) {
        ItemStack item = new ItemStack(type);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(SCULK_COLOR);
        item.setItemMeta(meta);
        return item;
    }

    // Su golpe da Oscuridad 4 segundos
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isCustomMob(event.getDamager())) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.8f, 1.3f);
    }

    // 15% de soltar un Fragmento de Eco, que va en la Energía de Warden
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isCustomMob(event.getEntity())) return;
        event.getDrops().clear();
        if (random.nextDouble() < ECHO_CHANCE) {
            event.getDrops().add(new ItemStack(Material.ECHO_SHARD));
        }
        event.setDroppedExp(8);

        Location loc = event.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_DEATH, 1.0f, 1.4f);
        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc.add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
        activeMobs.remove(event.getEntity().getUniqueId());
    }

    // No se convierte en drowned aunque se meta al agua del Pantano Profundo
    @EventHandler
    public void onTransform(EntityTransformEvent event) {
        if (isCustomMob(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        if (isCustomMob(event.getEntity())) activeMobs.add(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        activeMobs.remove(event.getEntity().getUniqueId());
    }

    // Escucha: si no tiene objetivo va por el jugador más cercano que se mueva sin agacharse
    private void startHearingTask() {
        if (hearingTask != null && !hearingTask.isCancelled()) return;
        hearingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : Set.copyOf(activeMobs)) {
                    if (!(Bukkit.getEntity(uuid) instanceof Zombie zombie) || !zombie.isValid()) continue;
                    if (zombie.getTarget() != null) continue;
                    Player heard = listen(zombie);
                    if (heard != null) {
                        zombie.setTarget(heard);
                        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WARDEN_LISTENING_ANGRY, 1.0f, 1.2f);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private Player listen(Zombie zombie) {
        Player nearest = null;
        double best = HEARING_RADIUS * HEARING_RADIUS;
        for (Player player : zombie.getWorld().getPlayers()) {
            if (player.isSneaking() || player.getGameMode() == GameMode.CREATIVE
                    || player.getGameMode() == GameMode.SPECTATOR) continue;
            double dist = player.getLocation().distanceSquared(zombie.getLocation());
            if (dist < best) {
                best = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private void scanExisting() {
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (isCustomMob(zombie)) activeMobs.add(zombie.getUniqueId());
            }
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Zombie
                && entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}
