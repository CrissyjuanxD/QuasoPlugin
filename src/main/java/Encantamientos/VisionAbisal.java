package Encantamientos;

import InfestedCaves.DarknessShield;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Visión Abisal (casco): protege de la Oscuridad 5 minutos, después se recarga 2 minutos y vuelve a proteger, y así.
// Si se saca el casco 2 minutos o más, al ponérselo empieza de nuevo con los 5 minutos
public class VisionAbisal {

    private static final long ACTIVE_MS = 5 * 60 * 1000;
    private static final long COOLDOWN_MS = 2 * 60 * 1000;
    private static final String COLOR = "#3FD0D4";

    private record Phase(boolean active, long endsAt) {}

    private final Map<UUID, Phase> phases = new HashMap<>();
    private final Map<UUID, Long> lastSeen = new HashMap<>();

    public VisionAbisal(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (QuasoEnchant.VISION_ABISAL.level(player.getInventory().getHelmet()) <= 0) continue;
            UUID id = player.getUniqueId();

            Phase phase = phases.get(id);
            long seen = lastSeen.getOrDefault(id, now);
            if (phase == null || now - seen > COOLDOWN_MS) {
                phase = start(player, true, now);
            } else if (now >= phase.endsAt()) {
                phase = start(player, !phase.active(), now);
            }
            lastSeen.put(id, now);

            if (phase.active()) DarknessShield.protect(player, 1500);
        }
    }

    private Phase start(Player player, boolean active, long now) {
        Phase phase = new Phase(active, now + (active ? ACTIVE_MS : COOLDOWN_MS));
        phases.put(player.getUniqueId(), phase);
        String text = active
                ? "Visión Abisal: protegido de la Oscuridad por 5 minutos"
                : "Visión Abisal se recarga: 2 minutos sin protección";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.of(COLOR) + text));
        player.playSound(player.getLocation(), active ? Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.6f);
        return phase;
    }
}
