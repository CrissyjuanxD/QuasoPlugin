package support;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.MenuType;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

/** Inicializa los tipos que Paper obtiene de registros durante los tests sin servidor. */
public final class PaperTestRegistry {
    private static RegistryAccess access;

    private PaperTestRegistry() {}

    public static RegistryAccess initialize() {
        if (access == null) {
            access = mock(RegistryAccess.class, call -> {
                if (!call.getMethod().getName().equals("getRegistry")) return null;
                Object kind = call.getArgument(0);
                return mock(Registry.class, lookup -> {
                    if (!lookup.getMethod().getName().startsWith("get")) return null;
                    if (kind == RegistryKey.MENU || kind == MenuType.class) return mock(MenuType.Typed.class);
                    if (kind == RegistryKey.SOUND_EVENT || kind == Sound.class) return mock(Sound.class);
                    if (kind == RegistryKey.ENCHANTMENT || kind == Enchantment.class) return mock(Enchantment.class);
                    return null;
                });
            });
            try (MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class)) {
                registry.when(RegistryAccess::registryAccess).thenReturn(access);
                InventoryType.values();
                // Fuerza la inicialización mientras el proveedor está disponible.
                Sound.BLOCK_NOTE_BLOCK_BELL.getKey();
                Enchantment.UNBREAKING.getKey();
            }
        }
        return access;
    }
}
