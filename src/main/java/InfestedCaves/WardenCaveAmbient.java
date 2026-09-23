package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WardenCaveAmbient implements Listener {

    private final JavaPlugin plugin;

    private final Map<UUID, Long> nextAmbientTrigger = new HashMap<>();

    private static final long AMBIENT_MIN = 1 * 60 * 1000;
    private static final long AMBIENT_MAX = 5 * 60 * 1000;

    private static final int DARKNESS_DURATION_TICKS = 20 * 20;

    private static final Sound[] AMBIENT_SOUNDS = new Sound[] {
            Sound.AMBIENT_CAVE,
            Sound.ENTITY_WARDEN_AMBIENT,
            Sound.ENTITY_WARDEN_HEARTBEAT,
            Sound.ENTITY_WARDEN_NEARBY_CLOSE,
            Sound.ENTITY_WARDEN_LISTENING
    };

    public WardenCaveAmbient(JavaPlugin plugin) {
        this.plugin = plugin;
        startAmbientLoop();
    }

    private void startAmbientLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) continue;

                    long nextTrigger = nextAmbientTrigger.computeIfAbsent(p.getUniqueId(),
                            k -> now + randomCooldown());

                    if (now >= nextTrigger) {
                        triggerAmbient(p);
                        nextAmbientTrigger.put(p.getUniqueId(), now + randomCooldown());
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    private long randomCooldown() {
        return ThreadLocalRandom.current().nextLong(AMBIENT_MIN, AMBIENT_MAX);
    }

    private void triggerAmbient(Player p) {
        Sound sound = AMBIENT_SOUNDS[ThreadLocalRandom.current().nextInt(AMBIENT_SOUNDS.length)];
        p.playSound(p.getLocation(), sound, SoundCategory.AMBIENT, 1.0f, 1.0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DARKNESS_DURATION_TICKS, 1));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        nextAmbientTrigger.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (e.getFrom().getName().equals(QuasoPlugin.WORLD_NAME)) {
            nextAmbientTrigger.remove(e.getPlayer().getUniqueId());
        }
    }
}