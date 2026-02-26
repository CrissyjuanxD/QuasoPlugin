package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ConfusionEffect implements CustomEffect {
    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> activeEffects = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Float> originalYaw = new HashMap<>();
    private final Map<UUID, Float> originalPitch = new HashMap<>();
    private final Map<UUID, Integer> shakePatterns = new HashMap<>();

    public ConfusionEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {

        removeEffect(player);

        // Guardamos la rotación original (opcional, aunque con movimiento constante es difícil de mantener)
        originalYaw.put(player.getUniqueId(), player.getLocation().getYaw());
        originalPitch.put(player.getUniqueId(), player.getLocation().getPitch());
        shakePatterns.put(player.getUniqueId(), random.nextInt(4));

        // Calculamos intensidad basada en el nivel (amplifier)
        // Nivel 1 (Amp 0) = 1.0 (Normal)
        // Nivel 2 (Amp 1) = 1.5 (Más fuerte)
        final float intensityMultiplier = 1.0f + (amplifier * 0.5f);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;
            int pattern = shakePatterns.get(player.getUniqueId());

            // Nota: Usar yaw/pitch fijo base puede ser molesto si el jugador intenta moverse.
            // Para un efecto de "borrachera/mareo", a veces es mejor sumar al yaw actual,
            // pero mantendremos tu lógica original aquí.
            float baseYaw = originalYaw.get(player.getUniqueId());
            float basePitch = originalPitch.get(player.getUniqueId());

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !player.hasPotionEffect(getTriggerEffectType())) {
                    removeEffect(player);
                    return;
                }

                // Actualizamos la base si el jugador se mueve mucho (opcional para hacerlo jugable)
                // baseYaw = player.getLocation().getYaw();

                applySmoothCameraShake(player, ticks, pattern, baseYaw, basePitch, intensityMultiplier);
                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        activeEffects.put(player.getUniqueId(), task);
    }

    @Override
    public void removeEffect(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = activeEffects.get(playerId);

        if (task != null) {
            task.cancel();
            activeEffects.remove(playerId);
            // Restaurar rotación puede ser brusco si el jugador se movió, a veces es mejor no hacerlo.
            // restoreOriginalRotation(player);
            originalYaw.remove(playerId);
            originalPitch.remove(playerId);
            shakePatterns.remove(playerId);
        }
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.UNLUCK; // Mala suerte activa Confusión
    }

    @Override
    public boolean isEffectActive(Player player) {
        return activeEffects.containsKey(player.getUniqueId());
    }

    @Override
    public void cleanup() {
        for (Map.Entry<UUID, BukkitRunnable> entry : activeEffects.entrySet()) {
            entry.getValue().cancel();
        }
        activeEffects.clear();
        originalYaw.clear();
        originalPitch.clear();
        shakePatterns.clear();
    }

    private void applySmoothCameraShake(Player player, int tick, int pattern, float baseYaw, float basePitch, float intensity) {
        if (!player.isOnline()) return;

        float frequency = 0.3f;
        // Aplicamos el multiplicador de intensidad (Nivel de la poción)
        float amplitudeYaw = 8.0f * intensity;
        float amplitudePitch = 5.0f * intensity;

        float yawVariation = calculateShake(tick, pattern, frequency, amplitudeYaw, 0);
        float pitchVariation = calculateShake(tick, pattern, frequency, amplitudePitch, 1);

        // Opción A: Forzar la cámara a un punto fijo + vibración (Tu lógica original)
        float newYaw = baseYaw + yawVariation;
        float newPitch = Math.max(-90, Math.min(90, basePitch + pitchVariation));

        // Opción B (Alternativa más jugable): Sumar vibración a la vista actual del jugador
        // float newYaw = player.getLocation().getYaw() + (yawVariation * 0.1f); // Más sutil

        setPlayerRotation(player, newYaw, newPitch);
    }

    private float calculateShake(int tick, int pattern, float frequency, float amplitude, int offset) {
        float time = tick * frequency + offset * 2.0f;

        switch (pattern) {
            case 0:
                return (float) (Math.sin(time) * amplitude * 0.7f + Math.cos(time * 0.8f) * amplitude * 0.3f);
            case 1:
                return (float) (Math.sin(time) * amplitude * (1.0f - Math.abs(Math.sin(time * 0.5f))));
            case 2:
                return (float) ((Math.sin(time) + 0.5f * Math.sin(time * 2.3f) + 0.3f * Math.sin(time * 3.7f)) * amplitude * 0.4f);
            case 3:
                return (float) (Math.sin(time) * Math.cos(time * 0.7f) * amplitude);
            default:
                return (float) (Math.sin(time) * amplitude * 0.5f);
        }
    }

    private void setPlayerRotation(Player player, float yaw, float pitch) {
        try {
            // Ubicación actual con nueva rotación
            org.bukkit.Location loc = player.getLocation();
            loc.setYaw(yaw);
            loc.setPitch(pitch);
            player.teleport(loc);
            // Nota: player.setRotation() existe en versiones muy recientes de Paper/Spigot,
            // si usas una versión antigua, teleport es la forma estándar.
        } catch (Exception e) {
            // Ignorar errores de teletransporte
        }
    }

    private void restoreOriginalRotation(Player player) {
        // Método mantenido pero cuidado al usarlo
        UUID playerId = player.getUniqueId();
        Float originalY = originalYaw.get(playerId);
        Float originalP = originalPitch.get(playerId);
        if (originalY != null && originalP != null) {
            setPlayerRotation(player, originalY, originalP);
        }
    }
}