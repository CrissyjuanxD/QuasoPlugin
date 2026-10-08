package items;

import Events.MissionSystem.MissionUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// La Warden Gun: dispara el sonic boom del Warden. Para que no esté rota tiene una carga corta, 5 segundos de
// recarga, 16 bloques de alcance, no atraviesa paredes, pega a 3 como mucho y a los jugadores y jefes les pega menos.
// No gasta Energía de Warden: el límite es la recarga. El golpe da Lentitud I 3 segundos y un empuje chico
public class WardenGun implements Listener {

    public static final String ID = "warden_gun";
    static final NamespacedKey GRUPO = new NamespacedKey("quasoplugin", ID);

    static final int RECARGA = 5 * 20;
    static final int CARGA = 12;
    static final double ALCANCE = 16;
    static final int MAX_GOLPES = 3;
    static final double DANO_MOB = 10;
    static final double DANO_JEFE = 6;
    static final double DANO_JUGADOR = 5;
    static final int LENTITUD = 3 * 20;

    private final JavaPlugin plugin;

    public WardenGun(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Warden Gun");
        meta.setCustomModelData(713);
        meta.getPersistentDataContainer().set(WardenCaveItems.ITEM_KEY, PersistentDataType.STRING, ID);
        meta.setItemModel(NamespacedKey.minecraft(ID));
        meta.setMaxStackSize(1);
        meta.setRarity(ItemRarity.EPIC);

        // El grupo hace que la recarga se vea solo en la Warden Gun y no en los demás echo shards
        UseCooldownComponent cooldown = meta.getUseCooldown();
        cooldown.setCooldownSeconds(RECARGA / 20f);
        cooldown.setCooldownGroup(GRUPO);
        meta.setUseCooldown(cooldown);

        meta.setLore(List.of(
                "",
                ChatColor.of("#008b8b") + "Un cañón que guarda el rugido",
                ChatColor.of("#008b8b") + "sónico del Ultra Warden.",
                "",
                ChatColor.of("#006666") + "Daño: " + ChatColor.WHITE + (int) DANO_MOB + ChatColor.of("#006666") + " a los mobs, "
                        + ChatColor.WHITE + (int) DANO_JUGADOR + ChatColor.of("#006666") + " a los jugadores",
                ChatColor.of("#006666") + "Alcance: " + ChatColor.WHITE + (int) ALCANCE + " bloques" + ChatColor.of("#006666")
                        + ", hasta " + MAX_GOLPES + " en línea",
                ChatColor.of("#006666") + "Aturde: " + ChatColor.WHITE + "Lentitud I " + (LENTITUD / 20) + " segundos",
                ChatColor.of("#006666") + "Recarga: " + ChatColor.WHITE + (RECARGA / 20) + " segundos",
                "",
                ChatColor.GRAY + "Uso:",
                ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho",
                ""
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGun(ItemStack item) {
        return ID.equals(WardenCaveItems.idOf(item));
    }

    // Click derecho: carga y dispara. Los bloques que se usan (cofres, puertas...) se abren normal si no te agachas
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!isGun(item) || event.useItemInHand() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable() && !player.isSneaking()) return;

        event.setCancelled(true);
        if (player.getGameMode() == GameMode.SPECTATOR || player.hasCooldown(item)) return;
        player.setCooldown(item, RECARGA);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1f, 1.6f);
        world.spawnParticle(Particle.SCULK_CHARGE_POP, player.getEyeLocation().add(player.getLocation().getDirection()), 8, 0.2, 0.2, 0.2, 0.02);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead() && isGun(player.getInventory().getItemInMainHand())) disparar(player);
        }, CARGA);
    }

    private void disparar(Player player) {
        World world = player.getWorld();
        Location ojo = player.getEyeLocation();
        Vector origen = ojo.toVector();
        Vector dir = ojo.getDirection().normalize();

        // El rayo se corta en la primera pared
        RayTraceResult pared = world.rayTraceBlocks(ojo, dir, ALCANCE, FluidCollisionMode.NEVER, true);
        double largo = pared != null ? pared.getHitPosition().distance(origen) : ALCANCE;

        for (double d = 1.5; d <= largo; d += 1.5) {
            world.spawnParticle(Particle.SONIC_BOOM, ojo.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
        world.playSound(ojo, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2f, 1.3f);

        BoundingBox zona = BoundingBox.of(origen, origen.clone().add(dir.clone().multiply(largo))).expand(2);
        List<LivingEntity> golpeados = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(zona)) {
            if (entity instanceof LivingEntity target && puedeGolpear(player, target)
                    && pasaPor(target.getBoundingBox(), origen, dir, largo)) golpeados.add(target);
        }
        golpeados.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(ojo)));

        DamageSource fuente = DamageSource.builder(DamageType.SONIC_BOOM).withCausingEntity(player).withDirectEntity(player).build();
        for (LivingEntity target : golpeados.subList(0, Math.min(MAX_GOLPES, golpeados.size()))) {
            double vida = target.getHealth();
            target.damage(dano(target instanceof Player, MissionUtils.bossId(target) != null), fuente);
            if (target.isDead() || target.getHealth() < vida) {
                Vector empuje = dir.clone().setY(0);
                if (empuje.lengthSquared() > 0) empuje.normalize().multiply(0.2);
                target.setVelocity(target.getVelocity().add(empuje.setY(0.2)));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, LENTITUD, 0));
            }
        }
    }

    // A quién no le pega: al que dispara, a sus mascotas, a los soportes de armadura y a los que no se pueden dañar
    private static boolean puedeGolpear(Player player, LivingEntity target) {
        if (target.equals(player) || target.isDead() || target.isInvulnerable() || target instanceof ArmorStand) return false;
        if (target instanceof Player other && (other.getGameMode() == GameMode.CREATIVE || other.getGameMode() == GameMode.SPECTATOR)) return false;
        return !(target instanceof Tameable pet && player.equals(pet.getOwner()));
    }

    // El rayo le pega si pasa a medio bloque de su hitbox
    static boolean pasaPor(BoundingBox caja, Vector origen, Vector dir, double largo) {
        return caja.clone().expand(0.5).rayTrace(origen, dir, largo) != null;
    }

    static double dano(boolean jugador, boolean jefe) {
        if (jugador) return DANO_JUGADOR;
        return jefe ? DANO_JEFE : DANO_MOB;
    }
}
