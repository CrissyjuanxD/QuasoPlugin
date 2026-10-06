package items;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemModelsTest {
    @Test
    void addsPackModelsAndPreservesExistingConfiguration() {
        QuasoPlugin plugin = mock(QuasoPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("modelos.enderbag", "quaso:bolsa_personalizada");
        when(plugin.getConfig()).thenReturn(config);

        ItemModels.load(plugin);

        assertEquals("quaso:bolsa_personalizada", config.getString("modelos.enderbag"));
        assertEquals("minecraft:keep_inv_liquido", config.getString("modelos.keep_inventory_liquido"));
        assertEquals("minecraft:statue_pr", config.getString("modelos.estatua_protectora"));
        assertEquals("minecraft:amuleto_esperanza", config.getString("modelos.amuleto_ultima_esperanza"));
        assertNotEquals(config.getString("modelos.doubletotem_1"), config.getString("modelos.doubletotem_2"));
        assertNotEquals(config.getString("modelos.mineral_crudo_cian"), config.getString("modelos.mineral_crudo_verde"));
        assertFalse(config.contains("modelos.gui_panel"));
        verify(plugin).saveConfig();

        ItemModels.load(plugin);
        verify(plugin, times(1)).saveConfig();
    }

    @Test
    void appliesConfiguredModelAndFallsBackFromInvalidResourceLocation() {
        QuasoPlugin plugin = mock(QuasoPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        ItemMeta meta = mock(ItemMeta.class);
        try (MockedStatic<QuasoPlugin> current = mockStatic(QuasoPlugin.class)) {
            current.when(QuasoPlugin::getInstance).thenReturn(plugin);
            config.set("modelos.enderbag", "quaso:bolsa");
            ItemModels.apply(meta, "enderbag");
            verify(meta).setItemModel(new NamespacedKey("quaso", "bolsa"));

            config.set("modelos.enderbag", "Modelo INVALIDO");
            ItemModels.apply(meta, "enderbag");
            verify(meta).setItemModel(NamespacedKey.minecraft("ender_bag"));
        }
    }
}
