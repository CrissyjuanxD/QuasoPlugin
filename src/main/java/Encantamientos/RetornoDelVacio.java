package Encantamientos;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Retorno del Vacío (peto): si el jugador cae al vacío del End vuelve al último suelo firme que pisó. Se recarga en
// 5 minutos y le cuesta un 20% de durabilidad al peto
public class RetornoDelVacio implements Listener {

    private static final long COOLDOWN_MS = 5 * 60 * 1000;

    private final Map<UUID, Location> lastGround = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, Long> lastWarning = new HashMap<>();

    public RetornoDelVacio(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::trackGround, 20L, 10L);
    }

    // Cada medio segundo guarda dónde está parado cada jugador en el End (solo si está sobre un bloque sólido)
    private void trackGround() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.THE_END) continue;
            if (!player.isOnGround() || player.isFlying() || player.isInsideVehicle()) continue;
            Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
            if (below.getType().isSolid()) lastGround.put(player.getUniqueId(), player.getLocation());
        }
    }

    // Va antes que el truco del tótem en el vacío del End (OneChanges); si lo salva, ese ya no hace nada
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVoid(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;
        ItemStack chestplate = player.getInventory().getChestplate();
        if (QuasoEnchant.RETORNO_DEL_VACIO.level(chestplate) <= 0) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long used = lastUse.getOrDefault(id, 0L);
        if (now - used < COOLDOWN_MS) {
            if (now - lastWarning.getOrDefault(id, 0L) > 2000) {
                lastWarning.put(id, now);
                long seconds = (COOLDOWN_MS - (now - used)) / 1000 + 1;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                        ChatColor.of("#B57EDC") + "Retorno del Vacío se recarga en " + seconds + "s"));
            }
            return;
        }

        Location target = lastGround.get(id);
        if (target == null || !target.getWorld().equals(player.getWorld())) target = player.getWorld().getSpawnLocation();

        lastUse.put(id, now);
        e.setCancelled(true);
        Location from = player.getLocation();
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        player.teleport(target);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
        player.damageItemStack(EquipmentSlot.CHEST, Math.max(1, chestplate.getType().getMaxDurability() / 5));

        World world = player.getWorld();
        world.spawnParticle(Particle.REVERSE_PORTAL, from, 60, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(Particle.REVERSE_PORTAL, target.clone().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.1);
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
        world.playSound(target, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 1.2f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                ChatColor.of("#B57EDC") + "¡El Retorno del Vacío te salvó!"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastGround.remove(e.getPlayer().getUniqueId());
        lastWarning.remove(e.getPlayer().getUniqueId());
    }
}
