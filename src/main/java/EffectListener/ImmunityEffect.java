package EffectListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class ImmunityEffect implements CustomEffect, Listener {

    private final Plugin plugin;

    public ImmunityEffect(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        // No necesitamos hacer nada extra al aplicar, la inmunidad la maneja el evento de abajo
    }

    @Override
    public void removeEffect(Player player) {
        // Opcional: Podrías reproducir un sonido aquí cuando se le acabe la inmunidad
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        // En la 1.21, este es el efecto nativo de Wind Charged
        return PotionEffectType.WIND_CHARGED;
    }

    @Override
    public boolean isEffectActive(Player player) {
        return player.hasPotionEffect(PotionEffectType.WIND_CHARGED);
    }

    @Override
    public void cleanup() {
        // No es necesario limpiar nada complejo
    }

    // =========================================================================
    // LÓGICA DE INMUNIDAD
    // =========================================================================
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {

            if (isEffectActive(player)) {
                event.setCancelled(true);
            }
        }
    }
}