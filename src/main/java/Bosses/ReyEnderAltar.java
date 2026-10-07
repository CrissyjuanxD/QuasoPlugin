package Bosses;

import EndBiomes.EndBiome;
import items.EndItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

// El altar del Rey Ender es el centro de cualquier Santuario Marchito del Páramo: la vara del End sobre las dos
// obsidianas llorosas, encima de la plataforma de obsidiana. Con el Ojo del Rey Ender en la mano (se gasta) se le da
// clic derecho y a los 5 segundos sale el boss. También vuelve a activar al Rey Ender cuando se carga su chunk
public class ReyEnderAltar implements Listener {

    private static final String COLOR = "#D36BFF";

    private final JavaPlugin plugin;
    private final NamespacedKey bossIdKey;
    private boolean summoning = false;

    public ReyEnderAltar(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack eye = event.getItem();
        if (!"ojo_rey_ender".equals(EndItems.idOf(eye))) return;
        event.setCancelled(true);
        Player player = event.getPlayer();

        Block rod = altarRod(event.getClickedBlock());
        if (rod == null) {
            player.sendMessage(ChatColor.of(COLOR) + "۞ El Ojo solo despierta en el altar de un Santuario Marchito: "
                    + "la vara del End sobre las obsidianas llorosas, en el Páramo Marchito.");
            return;
        }
        if (summoning || !ReyEnderBoss.ACTIVE_BOSSES.isEmpty()) {
            player.sendMessage(ChatColor.RED + "۞ Ya hay un Rey Ender despierto.");
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) eye.setAmount(eye.getAmount() - 1);
        summon(rod);
    }

    // La vara del End del centro del santuario (se puede clickear la vara o las obsidianas llorosas de abajo)
    private static Block altarRod(Block clicked) {
        if (clicked == null) return null;
        Block rod = null;
        for (int up = 0; up <= 2 && rod == null; up++) {
            Block b = clicked.getRelative(0, up, 0);
            if (b.getType() == Material.END_ROD) rod = b;
        }
        if (rod == null) return null;
        if (rod.getRelative(0, -1, 0).getType() != Material.CRYING_OBSIDIAN
                || rod.getRelative(0, -2, 0).getType() != Material.CRYING_OBSIDIAN) return null;
        if (!EndBiome.isParamo(rod.getBiome())) return null;

        int floor = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Material type = rod.getRelative(dx, -3, dz).getType();
                if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) floor++;
            }
        }
        return floor >= 20 ? rod : null;
    }

    // 5 segundos de animación: el ojo sube en espiral sobre la vara, el portal se abre y aparece el rey
    private void summon(Block rod) {
        summoning = true;
        World world = rod.getWorld();
        Location center = rod.getLocation().add(0.5, 0.5, 0.5);
        world.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 3f, 0.6f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (t <= 60) {
                    double a = t * 0.4;
                    double y = t / 60.0 * 5;
                    for (int i = 0; i < 2; i++) {
                        double angle = a + i * Math.PI;
                        world.spawnParticle(Particle.END_ROD, center.clone().add(Math.cos(angle) * 1.2, y, Math.sin(angle) * 1.2), 1, 0, 0, 0, 0);
                    }
                    world.spawnParticle(Particle.PORTAL, center.clone().add(0, y, 0), 10, 0.5, 0.5, 0.5, 0.8);
                    if (t % 20 == 0) world.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 3f, 0.6f + t / 100f);
                    return;
                }
                if (t < 100) {
                    double r = (t - 60) / 40.0 * 6;
                    int points = (int) (r * 8) + 4;
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r), 1, 0, 0, 0, 0);
                    }
                    if (t == 61) world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 4f, 0.5f);
                    if (t == 80) world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 3f, 0.8f);
                    return;
                }
                cancel();
                summoning = false;
                ReyEnderBoss.spawn(plugin, rod.getLocation().add(0.5, 1, 0.5));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) restore(entity);
    }

    // Al prender la etapa del End vuelve a activar a los reyes que ya estén cargados
    public void restoreLoaded() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) continue;
            for (Enderman enderman : world.getEntitiesByClass(Enderman.class)) restore(enderman);
        }
    }

    private void restore(Entity entity) {
        if (!(entity instanceof Enderman king)) return;
        if (!ReyEnderBoss.BOSS_ID.equals(king.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING))) return;
        if (ReyEnderBoss.ACTIVE_BOSSES.containsKey(king.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (king.isValid() && !king.isDead() && !ReyEnderBoss.ACTIVE_BOSSES.containsKey(king.getUniqueId())) {
                new ReyEnderBoss(plugin, king);
            }
        }, 1L);
    }
}
