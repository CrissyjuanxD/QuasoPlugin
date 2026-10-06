package Events.MissionSystem;

import InfestedCaves.WardenBiome;
import InfestedCaves.WardenBiomeMap;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import imp.crissyjuanxd.QuasoPlugin;
import items.InfinitePearl;
import items.WardenCaveItems;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import imp.crissyjuanxd.bloodmoon.BloodMoonActuator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

// Cosas que usan muchas misiones: mobs custom, élites, BloodMoon, inventario y quién peleó contra cada jefe
public final class MissionUtils implements Listener {
    public static final String INFESTED_SKELETON = "infested_skeleton";
    public static final String INFESTED_CAVE_SPIDER = "infested_cave_spider";
    public static final String INFESTED_GHAST = "infested_ghast";
    public static final String INFESTED_CREEPER = "infested_creeper";
    public static final String WARDEN_ZOMBIE = "warden_zombie";
    public static final String ENDER_BLAZE = "ender_blaze";
    public static final String ENDER_CREEPER = "ender_creeper";
    public static final String ENDER_SPIDER = "ender_spider";
    public static final String CORRUPTED_ZOMBIE = "corrupted_zombie";
    public static final String CORRUPTED_SPIDER = "corruptedspider";
    public static final String CORRUPTED_BEE = "corrupted_bee";
    public static final String BOMBITA = "bombita";
    public static final String ICEOLOGER = "iceologer";

    private static JavaPlugin plugin;
    private static final Map<String, NamespacedKey> KEYS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> BOSS_DAMAGERS = new HashMap<>();

    private MissionUtils() {}

    static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        javaPlugin.getServer().getPluginManager().registerEvents(new MissionUtils(), javaPlugin);
    }

    public static NamespacedKey key(String name) {
        return KEYS.computeIfAbsent(name, k -> new NamespacedKey(plugin, k));
    }

    // Los mobs custom del plugin se marcan con una PDC BYTE con su nombre
    public static boolean isMob(Entity entity, String mobKey) {
        return entity != null && entity.getPersistentDataContainer().has(key(mobKey), PersistentDataType.BYTE);
    }

    public static boolean isElite(LivingEntity entity) {
        try {
            if (EntityTracker.getEliteMobEntity(entity) != null) return true;
        } catch (Throwable ignored) {
        }
        return entity.hasMetadata("EliteMob")
                || entity.getScoreboardTags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains("elitemob"));
    }

    // El que lo mató, o el último jugador que le pegó (también con flechas)
    public static Player killer(LivingEntity entity) {
        Player killer = entity.getKiller();
        if (killer != null) return killer;
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent damage) {
            return attacker(damage.getDamager());
        }
        return null;
    }

    public static Player attacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    public static boolean isBloodMoon(World world) {
        try {
            BloodMoonActuator actuator = BloodMoonActuator.GetActuator(world);
            return actuator != null && actuator.isInProgress();
        } catch (Throwable ignored) {
            return false;
        }
    }

    // Una noche entera cae dentro del mismo día de Minecraft, así que el día sirve para saber de qué BloodMoon es
    public static long dayId(World world) {
        return world.getFullTime() / 24000L;
    }

    public static boolean isNight(World world) {
        long time = world.getTime();
        return world.getEnvironment() == World.Environment.NORMAL && time >= 13000 && time <= 23000;
    }

    public static boolean inWardenCave(Entity entity) {
        return entity.getWorld().getName().equals(QuasoPlugin.WORLD_NAME);
    }

    public static WardenBiome wardenBiome(Location location) {
        return WardenBiomeMap.forSeed(location.getWorld().getSeed()).biomeAt(location.getBlockX(), location.getBlockZ());
    }

    // Id de los items custom (los de la Warden Cave o los que usen la PDC "custom_item")
    public static String customId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = WardenCaveItems.idOf(item);
        if (id != null) return id;
        return item.getItemMeta().getPersistentDataContainer().get(key("custom_item"), PersistentDataType.STRING);
    }

    public static int count(Player player, Predicate<ItemStack> filter) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && filter.test(item)) total += item.getAmount();
        }
        return total;
    }

    public static int count(Player player, Material material) {
        return count(player, item -> item.getType() == material);
    }

    public static boolean has(Player player, Predicate<ItemStack> filter) {
        return count(player, filter) > 0;
    }

    public static boolean isWardenArmor(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key("warden_armor"), PersistentDataType.BYTE);
    }

    public static boolean fullWardenArmor(Player player) {
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (!isWardenArmor(piece)) return false;
        }
        return true;
    }

    public static boolean noArmor(Player player) {
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && !piece.getType().isAir()) return false;
        }
        return true;
    }

    public static boolean isSpear(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SPEAR");
    }

    public static boolean isExcavator(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key("la_excavadora"), PersistentDataType.BYTE);
    }

    public static boolean isInfinitePearl(ItemStack item) {
        return InfinitePearl.isPearl(item);
    }

    public static boolean isWardenGun(ItemStack item) {
        return "warden_gun".equals(customId(item));
    }

    public static boolean isShopVillager(Entity entity) {
        return entity.getPersistentDataContainer().has(key("shop_type"), PersistentDataType.STRING);
    }

    public static boolean isSurvival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    // Nombre del jefe para las misiones, o null si no es un jefe
    public static String bossId(Entity entity) {
        String custom = entity.getPersistentDataContainer().get(key("boss_id"), PersistentDataType.STRING);
        if (custom != null) return custom;
        if (entity.getPersistentDataContainer().has(key("is_queen_bee"), PersistentDataType.BYTE)) return "abeja_reina";
        if (entity.getCustomName() != null && entity.getCustomName().contains("Abeja Reina")) return "abeja_reina";
        return switch (entity.getType()) {
            case ENDER_DRAGON -> "ender_dragon";
            case WITHER -> "wither";
            case ELDER_GUARDIAN -> "elder_guardian";
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss) || bossId(boss) == null) return;
        Player player = attacker(event.getDamager());
        if (player == null) return;

        if (BOSS_DAMAGERS.size() > 200) BOSS_DAMAGERS.keySet().removeIf(id -> Bukkit.getEntity(id) == null);
        BOSS_DAMAGERS.computeIfAbsent(boss.getUniqueId(), k -> new HashSet<>()).add(player.getUniqueId());
    }

    // Cuentan los que le pegaron, el que lo mató y los que estaban peleando cerca
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity boss = event.getEntity();
        Set<UUID> damagers = BOSS_DAMAGERS.remove(boss.getUniqueId());
        String id = bossId(boss);
        if (id == null) return;

        Set<Player> players = new LinkedHashSet<>();
        if (damagers != null) {
            for (UUID uuid : damagers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) players.add(player);
            }
        }
        Player killer = killer(boss);
        if (killer != null) players.add(killer);

        double radius = id.equals("ender_dragon") ? 150 : 48;
        for (Player near : boss.getWorld().getPlayers()) {
            if (isSurvival(near) && near.getLocation().distanceSquared(boss.getLocation()) <= radius * radius) players.add(near);
        }

        Bukkit.getPluginManager().callEvent(new BossDefeatedEvent(id, boss, players));
    }
}
