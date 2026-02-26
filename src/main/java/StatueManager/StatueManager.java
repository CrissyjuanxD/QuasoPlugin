package StatueManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatueManager {

    private final JavaPlugin plugin;
    private final Map<UUID, ArmorStand> activeStatues = new HashMap<>();

    public StatueManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startEffectLoop();
    }

    public void loadStatues() {
        // Ejecutamos esto un tick después para asegurar que los mundos y scoreboard estén listos
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof ArmorStand && StatueData.isStatue((ArmorStand) entity)) {
                        ArmorStand stand = (ArmorStand) entity;
                        activeStatues.put(entity.getUniqueId(), stand);

                        // RESTAURAR PROPIEDADES VISUALES
                        StatueData data = new StatueData(stand);
                        stand.setVisible(data.isVisible());
                        updateGlowingColor(stand);
                    }
                }
            }
        });
    }

    public void registerStatue(ArmorStand stand) {
        activeStatues.put(stand.getUniqueId(), stand);
        updateGlowingColor(stand);
    }

    public void unregisterStatue(ArmorStand stand) {
        activeStatues.remove(stand.getUniqueId());
        removeEffectFromPlayers(stand);
    }

    private void startEffectLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : new ArrayList<>(activeStatues.values())) {
                    if (!stand.isValid() || !stand.getChunk().isLoaded()) {
                        if (stand.isDead()) activeStatues.remove(stand.getUniqueId());
                        continue;
                    }

                    StatueData data = new StatueData(stand);
                    double radiusX = data.getRadiusX();
                    double radiusY = data.getRadiusY();
                    PotionEffectType type = data.getEffectType();
                    int amp = data.getEffectAmplifier();

                    if (type == null) continue;

                    for (Entity ent : stand.getNearbyEntities(radiusX, radiusY, radiusX)) {
                        if (ent instanceof Player) {
                            Player p = (Player) ent;
                            applySmartEffect(p, type, amp);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void applySmartEffect(Player p, PotionEffectType type, int amplifier) {
        PotionEffect current = p.getPotionEffect(type);
        if (current != null) {
            // Si tiene el mismo efecto con mayor o igual nivel y duración decente, no hacer nada
            if (current.getDuration() > 40 && current.getAmplifier() >= amplifier) {
                return;
            }
        }
        p.addPotionEffect(new PotionEffect(type, 200, amplifier, true, true));
    }

    private void removeEffectFromPlayers(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        PotionEffectType type = data.getEffectType();
        if (type == null) return;
        double rX = data.getRadiusX();
        double rY = data.getRadiusY();

        for (Entity ent : stand.getNearbyEntities(rX, rY, rX)) {
            if (ent instanceof Player) {
                Player p = (Player) ent;
                PotionEffect current = p.getPotionEffect(type);
                // Solo lo quitamos si parece que fue dado por la estatua (duración baja restante)
                if (current != null && current.getDuration() <= 205) {
                    p.removePotionEffect(type);
                }
            }
        }
    }

    public void updateGlowingColor(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        ChatColor color = data.getGlowColor();

        // Si el color es NULL (OFF)
        if (color == null) {
            stand.setGlowing(false);
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "SE_" + color.name();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(color);
        }

        // Aseguramos que la entidad esté en el equipo
        if (!team.hasEntry(stand.getUniqueId().toString())) {
            team.addEntry(stand.getUniqueId().toString());
        }

        stand.setGlowing(true);
    }
}