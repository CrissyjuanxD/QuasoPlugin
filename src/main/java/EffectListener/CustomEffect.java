package EffectListener;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public interface CustomEffect {
    void applyEffect(Player player, int duration, int amplifier);

    void removeEffect(Player player);

    PotionEffectType getTriggerEffectType();

    boolean isEffectActive(Player player);

    default void cleanup() {}
}