package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CustomEffectManager implements Listener {
    private final Map<PotionEffectType, CustomEffect> registeredEffects = new HashMap<>();
    private final Set<UUID> playersWithEffects = new HashSet<>();

    public void registerEffect(CustomEffect effect) {
        registeredEffects.put(effect.getTriggerEffectType(), effect);
    }

    public void unregisterEffect(PotionEffectType effectType) {
        registeredEffects.remove(effectType);
    }

    @EventHandler
    public void onPlayerPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PotionEffectType effectType = event.getModifiedType();

        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            switch (event.getAction()) {
                case ADDED:
                case CHANGED:
                    PotionEffect newEffect = event.getNewEffect();
                    if (newEffect != null) {
                        int duration = newEffect.getDuration() / 20;
                        int amplifier = newEffect.getAmplifier(); // Capturamos el nivel

                        // Pasamos el amplifier
                        customEffect.applyEffect(player, duration, amplifier);

                        // Solo añadimos al set si el efecto realmente se aplicó (lógica interna del efecto)
                        // Pero para simplificar el manager, lo marcamos, el efecto decide si hace algo o no.
                        playersWithEffects.add(player.getUniqueId());
                    }
                    break;

                case REMOVED:
                case CLEARED:
                    customEffect.removeEffect(player);
                    playersWithEffects.remove(player.getUniqueId());
                    break;
                default: break;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (CustomEffect effect : registeredEffects.values()) {
            effect.removeEffect(player);
        }
        playersWithEffects.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (PotionEffectType effectType : registeredEffects.keySet()) {
            if (player.hasPotionEffect(effectType)) {
                PotionEffect potionEffect = player.getPotionEffect(effectType);
                if (potionEffect != null) {
                    CustomEffect customEffect = registeredEffects.get(effectType);
                    int duration = potionEffect.getDuration() / 20;
                    int amplifier = potionEffect.getAmplifier();

                    customEffect.applyEffect(player, duration, amplifier);
                    playersWithEffects.add(player.getUniqueId());
                }
            }
        }
    }

    // Métodos manuales actualizados (asumen nivel 100/amp 99 por defecto si es manual)
    public void applyEffectManually(Player player, PotionEffectType effectType, int duration) {
        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            customEffect.applyEffect(player, duration, 99);
        }
    }

    public void removeEffectManually(Player player, PotionEffectType effectType) {
        CustomEffect customEffect = registeredEffects.get(effectType);
        if (customEffect != null) {
            customEffect.removeEffect(player);
        }
    }
}