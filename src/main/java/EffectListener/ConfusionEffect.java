package EffectListener;

import com.viciontmedia.api.ViciontMediaAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class ConfusionEffect implements CustomEffect {

    private final Plugin plugin;

    public ConfusionEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        // Tiene el efecto -> Aplicamos el Shader
        ViciontMediaAPI.sendShaderApply(player, "confusion");
    }

    @Override
    public void removeEffect(Player player) {

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            // Si el jugador sigue online y REALMENTE ya no tiene la poción, le quitamos el shader.
            // Si se desconectó, isOnline() es false, por lo que NO le mandamos a quitar el shader.
            if (player.isOnline() && !player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                ViciontMediaAPI.sendShaderRemove(player, "confusion");
            }

        }, 1L);
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.UNLUCK;
    }

    @Override
    public boolean isEffectActive(Player player) {
        return player.hasPotionEffect(PotionEffectType.UNLUCK);
    }

    @Override
    public void cleanup() {
        // Solo para recargas del servidor o apagados, limpiamos a los que estén online
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
                ViciontMediaAPI.sendShaderRemove(player, "confusion");
            }
        }
    }
}