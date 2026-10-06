package BloodMoon;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Array;

/** WorldGuard sigue siendo opcional; Quaso no carga sus clases si no está instalado. */
final class WorldGuardSupport {
    private static boolean warned;
    private WorldGuardSupport() {}
    static boolean canSpawn(Location location) { return test(location, "MOB_SPAWNING"); }
    static boolean canDamage(Location location) { return test(location, "MOB_DAMAGE"); }

    private static boolean test(Location location, String flagName) {
        var plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !plugin.isEnabled()) return true;
        try {
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> wg = Class.forName("com.sk89q.worldguard.WorldGuard", true, loader);
            Object guard = wg.getMethod("getInstance").invoke(null);
            Object platform = wg.getMethod("getPlatform").invoke(guard);
            Object container = Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform", true, loader)
                    .getMethod("getRegionContainer").invoke(platform);
            Object query = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer", true, loader)
                    .getMethod("createQuery").invoke(container);
            Object adapted = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter", true, loader)
                    .getMethod("adapt", Location.class).invoke(null, location);
            Class<?> stateFlag = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag", true, loader);
            Object flags = Array.newInstance(stateFlag, 1);
            Array.set(flags, 0, Class.forName("com.sk89q.worldguard.protection.flags.Flags", true, loader).getField(flagName).get(null));
            return (boolean) query.getClass().getMethod("testState",
                    Class.forName("com.sk89q.worldedit.util.Location", true, loader),
                    Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable", true, loader), flags.getClass())
                    .invoke(query, adapted, null, flags);
        } catch (ReflectiveOperationException | LinkageError ex) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().warning("[Quaso/BloodMoon] No se pudo consultar WorldGuard; se omiten los spawns y efectos protegidos: " + ex.getMessage());
            }
            return false;
        }
    }
}
