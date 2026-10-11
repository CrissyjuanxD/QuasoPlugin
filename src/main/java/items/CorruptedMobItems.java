package items;

import org.bukkit.inventory.ItemStack;

/** Alias de las fábricas antiguas para integraciones existentes. */
public class CorruptedMobItems {
    public static ItemStack createCorruptedMeet() { return FloralMobItems.createFlesh(); }
    public static ItemStack createCorruptedSpiderEye() { return FloralMobItems.createEye(); }
}
