package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

// Arco de Sculk (Núcleo Sónico + arco + Energía de Warden en la herrería): sus flechas llevan un eco que pega 4 de daño
// extra y que los Endermen no pueden esquivar, así sirve para el End. Acepta Perforación y Poder VI como el Arco Mejorado
public class ArcoSculk implements Listener {

    public static final String ID = "arco_sculk";
    static final double DANO_SONICO = 4;
    private static final String COLOR = "#29DFEB";
    private static final NamespacedKey FLECHA = new NamespacedKey("quasoplugin", "flecha_sculk");
    private static final NamespacedKey BOSS_ID = new NamespacedKey("quasoplugin", "boss_id");
    private static final Particle.DustOptions POLVO = new Particle.DustOptions(Color.fromRGB(0x29DFEB), 0.9f);

    private final JavaPlugin plugin;

    public ArcoSculk(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack create() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        meta.setDisplayName(ChatColor.of(COLOR) + "" + ChatColor.BOLD + "Arco de Sculk");
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, ID);
        meta.setRarity(ItemRarity.EPIC);
        meta.setItemModel(NamespacedKey.minecraft(ID));
        ChatColor oscuro = ChatColor.of(EquipoLore.tono(COLOR, 0.75));
        ChatColor claro = ChatColor.of(EquipoLore.tono(COLOR, 1.45));
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(oscuro + "Tensado con el eco del");
        lore.add(oscuro + "Ultra Warden.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Las flechas:");
        lore.add(ChatColor.GRAY + "> " + claro + "+" + (int) DANO_SONICO + " de daño sónico");
        lore.add(ChatColor.GRAY + "> " + claro + "Los Endermen no las esquivan");
        lore.add("");
        lore.add(ChatColor.GRAY + "En el yunque:");
        lore.add(ChatColor.GRAY + "> " + claro + "Acepta Perforación");
        lore.add(ChatColor.GRAY + "> " + claro + "Poder hasta VI (Poder V + libro V)");
        lore.add("");
        meta.setLore(lore);
        bow.setItemMeta(meta);
        return bow;
    }

    public static boolean es(ItemStack item) {
        return ID.equals(EndItems.idOf(item));
    }

    // Sale de un arco normal o del Arco Mejorado (no de otros arcos custom como el Explosivo)
    public static boolean puedeMejorar(ItemStack base) {
        if (base == null || base.getType() != Material.BOW || es(base)) return false;
        return ArcoMejorado.es(base) || !ArcoMejorado.esCustom(base);
    }

    // Marca la flecha y le deja una estela de sculk mientras vuela
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShoot(EntityShootBowEvent event) {
        if (!es(event.getBow()) || !(event.getProjectile() instanceof AbstractArrow arrow)) return;
        arrow.getPersistentDataContainer().set(FLECHA, PersistentDataType.BYTE, (byte) 1);
        Location loc = arrow.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 1.8f);
        new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isInBlock() || ++ticks > 100) {
                    cancel();
                    return;
                }
                Location at = arrow.getLocation();
                at.getWorld().spawnParticle(Particle.DUST, at, 2, 0.04, 0.04, 0.04, 0, POLVO);
                if (ticks % 3 == 0) at.getWorld().spawnParticle(Particle.SCULK_SOUL, at, 1, 0, 0, 0, 0.01);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    static boolean esFlecha(Entity entity) {
        return entity instanceof AbstractArrow && entity.getPersistentDataContainer().has(FLECHA, PersistentDataType.BYTE);
    }

    // El Enderman esquiva toda flecha: acá se cancela el choque y se le pega con daño sónico (que no esquiva), con lo
    // mismo que habría hecho la flecha. Los bosses quedan como están
    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!esFlecha(event.getEntity()) || !(event.getHitEntity() instanceof Enderman enderman)) return;
        if (enderman.getPersistentDataContainer().has(BOSS_ID, PersistentDataType.STRING)) return;
        AbstractArrow arrow = (AbstractArrow) event.getEntity();
        event.setCancelled(true);
        double dano = Math.ceil(arrow.getVelocity().length() * (arrow.getDamage() + bonoPoder(arrow.getWeapon()))) + DANO_SONICO;
        Entity shooter = arrow.getShooter() instanceof Entity e ? e : arrow;
        enderman.damage(dano, DamageSource.builder(DamageType.SONIC_BOOM).withCausingEntity(shooter).withDirectEntity(shooter).build());
        eco(enderman);
        arrow.remove();
    }

    // El Poder se suma al pegar (no viene en la flecha): +1 con Poder I y +0.5 por cada nivel más, como en el juego
    static double bonoPoder(ItemStack arco) {
        int poder = arco == null ? 0 : arco.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.POWER);
        return poder <= 0 ? 0 : 1 + 0.5 * (poder - 1);
    }

    // Al resto de los mobs y jugadores les suma el daño sónico
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!esFlecha(event.getDamager()) || !(event.getEntity() instanceof LivingEntity target)) return;
        event.setDamage(event.getDamage() + DANO_SONICO);
        eco(target);
    }

    private static void eco(LivingEntity target) {
        Location loc = target.getLocation().add(0, target.getHeight() / 2, 0);
        loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);
        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 6, 0.3, 0.4, 0.3, 0.03);
        loc.getWorld().playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 1.9f);
    }
}
