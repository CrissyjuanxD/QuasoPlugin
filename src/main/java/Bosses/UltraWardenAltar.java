package Bosses;

import Dificultades.Features.AltarActivateEvent;
import items.WardenCaveItems;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

// El altar del Ultra Warden es el del sistema de altares (/altarvct create ultrawarden): con un Corazón de Warden
// Boss en la mano (se gasta) se le da click al catalizador y a los 5 segundos sale el boss. Solo puede haber uno
// vivo. También vuelve a activar al Ultra Warden cuando se carga su chunk
public class UltraWardenAltar implements Listener {

    private static final String COLOR = "#29DFEB";
    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(0x29DFEB), 1.3f);
    private static final Particle.DustOptions DARK = new Particle.DustOptions(Color.fromRGB(0x0BB5B5), 1.6f);

    private final JavaPlugin plugin;
    private final NamespacedKey bossIdKey;
    private boolean summoning = false;

    public UltraWardenAltar(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
    }

    @EventHandler
    public void onAltar(AltarActivateEvent event) {
        if (!event.getAltarType().equals("ultra_warden")) return;
        Player player = event.getPlayer();
        ItemStack heart = player.getInventory().getItemInMainHand();
        if (!"corazon_warden_boss".equals(WardenCaveItems.idOf(heart))) {
            player.sendMessage(ChatColor.of(COLOR) + "۞ El altar necesita un Corazón de Warden Boss en la mano.");
            player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 1f, 0.6f);
            return;
        }
        if (summoning || bossNear(event.getLocation())) {
            player.sendMessage(ChatColor.RED + "۞ El Ultra Warden ya está despierto.");
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) heart.setAmount(heart.getAmount() - 1);
        summon(event.getLocation().getBlock());
    }

    private boolean bossNear(Location altar) {
        if (!UltraWardenBoss.ACTIVE_BOSSES.isEmpty()) return true;
        for (Entity entity : altar.getWorld().getNearbyEntities(altar, 64, 40, 64)) {
            if (entity instanceof Warden && UltraWardenBoss.BOSS_ID.equals(
                    entity.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING))) return true;
        }
        return false;
    }

    // 5 segundos de ritual: el corazón late sobre el catalizador, las almas suben en espiral, se cierra un anillo de
    // sculk y el suelo se abre donde va a salir el boss
    private void summon(Block catalyst) {
        summoning = true;
        World world = catalyst.getWorld();
        Location center = catalyst.getLocation().add(0.5, 1, 0.5);
        Location spawn = spawnSpot(catalyst);
        world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 3f, 0.5f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (t <= 60) {
                    double y = t / 60.0 * 4;
                    for (int i = 0; i < 3; i++) {
                        double angle = t * 0.35 + i * 2 * Math.PI / 3;
                        world.spawnParticle(Particle.DUST, center.clone().add(Math.cos(angle) * 1.3, y, Math.sin(angle) * 1.3), 1, 0, 0, 0, 0, DUST);
                    }
                    world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, y, 0), 2, 0.3, 0.2, 0.3, 0.02);
                    if (t % 20 == 0) {
                        world.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 3f, 0.6f + t / 120f);
                        world.spawnParticle(Particle.SCULK_CHARGE_POP, center.clone().add(0, 4, 0), 30, 0.5, 0.5, 0.5, 0.05);
                    }
                    return;
                }
                if (t < 100) {
                    double r = 7 - (t - 60) / 40.0 * 6;
                    int points = (int) (r * 7) + 6;
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        world.spawnParticle(Particle.DUST, spawn.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r), 1, 0, 0, 0, 0, DARK);
                    }
                    world.spawnParticle(Particle.BLOCK, spawn, 8, 1, 0.1, 1, 0, Material.SCULK.createBlockData());
                    if (t == 61) world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 4f, 0.5f);
                    if (t == 80) world.playSound(spawn, Sound.ENTITY_WARDEN_DIG, 4f, 0.6f);
                    return;
                }
                cancel();
                summoning = false;
                UltraWardenBoss.spawn(plugin, spawn);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Sale al lado del altar, donde haya 5 bloques libres y suelo firme; si no hay lugar, arriba del altar
    private static Location spawnSpot(Block catalyst) {
        Location base = catalyst.getLocation().add(0.5, 0, 0.5);
        for (double r = 6; r <= 10; r += 2) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                Location around = base.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                for (int dy = 2; dy >= -3; dy--) {
                    Location feet = around.clone().add(0, dy, 0);
                    if (room(feet)) return feet.getBlock().getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return base.add(0, 1, 0);
    }

    private static boolean room(Location feet) {
        if (!feet.clone().add(0, -1, 0).getBlock().getType().isSolid()) return false;
        for (int h = 0; h < 5; h++) {
            Block b = feet.clone().add(0, h, 0).getBlock();
            if (!b.isPassable() || b.isLiquid()) return false;
        }
        return true;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) restore(entity);
    }

    // Al prender la etapa vuelve a activar a los que ya estén cargados
    public void restoreLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Warden warden : world.getEntitiesByClass(Warden.class)) restore(warden);
        }
    }

    private void restore(Entity entity) {
        if (!(entity instanceof Warden warden)) return;
        if (!UltraWardenBoss.BOSS_ID.equals(warden.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING))) return;
        if (UltraWardenBoss.ACTIVE_BOSSES.containsKey(warden.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (warden.isValid() && !warden.isDead() && !UltraWardenBoss.ACTIVE_BOSSES.containsKey(warden.getUniqueId())) {
                new UltraWardenBoss(plugin, warden);
            }
        }, 1L);
    }
}
