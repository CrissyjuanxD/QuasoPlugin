package EffectListener;

import org.bukkit.potion.PotionEffectType;

import java.util.Set;

// Efectos vanilla que el plugin usa como marca de sus efectos custom: Suerte = Keep Inventory, Wind Charged =
// Inmunidad y Weaving = Corrupture. Otros sistemas (estatuas, pesadilla) no los pueden dar sueltos
public final class ReservedEffects {

    private static final Set<PotionEffectType> RESERVED = Set.of(
            PotionEffectType.LUCK,
            PotionEffectType.WIND_CHARGED,
            PotionEffectType.WEAVING
    );

    private ReservedEffects() {}

    public static boolean isReserved(PotionEffectType type) {
        return type != null && RESERVED.contains(type);
    }
}
