package items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class FloralItemsTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    // Mejor que el Netherite (espada 8, hacha 10 a 1 ataque por segundo) y peor que la armadura de Warden
    @Test void theFloralGearSitsBetweenNetheriteAndWarden() {
        assertEquals(9.0, FloralItems.Pieza.ESPADA.dano);
        assertEquals(8.0, FloralItems.Pieza.HACHA.dano);
        assertTrue(FloralItems.Pieza.HACHA.velocidad > 1.0);
        assertTrue(FloralItems.Pieza.PICO.velocidad > 1.2);
        assertTrue(FloralItems.Pieza.PALA.velocidad > 1.0);
        for (FloralItems.Pieza pieza : new FloralItems.Pieza[]{FloralItems.Pieza.HACHA, FloralItems.Pieza.PICO, FloralItems.Pieza.PALA}) {
            assertTrue(pieza.minado > 0, pieza.name());
        }

        int armadura = 0;
        for (FloralItems.Pieza pieza : FloralItems.Pieza.values()) armadura += pieza.armadura;
        assertTrue(armadura >= 20 && armadura < 24, "armadura " + armadura);
        assertTrue(FloralItems.Pieza.DUREZA > 3 && FloralItems.Pieza.DUREZA < 4);
        // 1 corazón por pieza (Warden da 2)
        assertEquals(2, FloralItems.Pieza.VIDA);
        assertTrue(FloralItems.Pieza.SIN_DESGASTE < 0.30);

        // Hay pieza floral para cada herramienta de Celestita (así la Celestita sale de la floral) y el escudo
        for (EndItems.Tool tool : EndItems.Tool.values()) {
            assertEquals(tool.base, FloralItems.Pieza.valueOf(tool.name()).base, tool.name());
        }
        assertEquals(Material.SHIELD, FloralItems.Pieza.ESCUDO.base);
        assertFalse(FloralItems.Pieza.ESCUDO.esArmadura());
        assertEquals(11, FloralItems.Pieza.values().length);

        assertEquals(FloralItems.Pieza.PICO, FloralItems.piezaPorId("pico_floral"));
        assertEquals(Material.NETHERITE_PICKAXE, FloralItems.Pieza.PICO.base);
        assertNull(FloralItems.piezaPorId("pico_celestita"));
    }

    private static ItemStack item(Material type, int amount, String dropId, String customId) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        AtomicInteger cantidad = new AtomicInteger(amount);
        when(item.getType()).thenReturn(type);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.getAmount()).thenAnswer(call -> cantidad.get());
        doAnswer(call -> { cantidad.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(FloralMobItems.FLORAL_DROP, PersistentDataType.STRING)).thenReturn(dropId);
        when(data.get(EndItems.ITEM_KEY, PersistentDataType.STRING)).thenReturn(customId);
        return item;
    }

    private static ItemStack[] matrix(int carne) {
        return new ItemStack[]{
                item(Material.ROTTEN_FLESH, carne, "carne_podrida_floral", null),
                item(Material.SPIDER_EYE, 16, "ojo_arana_floral", null),
                item(Material.GUNPOWDER, 22, "polvora_floral", null),
                item(Material.PRISMARINE_SHARD, 16, "aguijon_floral", null),
                item(Material.FIREWORK_STAR, 1, null, "esencia_floral"),
                item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2, null, null),
                null, null, null};
    }

    // 16 de cada drop por mejora (no se puede copiar, hace falta una por pieza)
    @Test void theUpgradeNeedsSixteenOfEachFloralDropAndSpendsThem() {
        assertFalse(Dificultades.FloralForjaAccess.alcanza(matrix(15)));
        ItemStack[] matrix = matrix(16);
        assertTrue(Dificultades.FloralForjaAccess.completo(matrix));

        Dificultades.FloralForjaAccess.gastar(matrix);
        assertNull(matrix[0]);
        assertNull(matrix[1]);
        assertEquals(6, matrix[2].getAmount());
        assertNull(matrix[3]);
        assertNull(matrix[4]);
        assertEquals(1, matrix[5].getAmount());
        assertFalse(Dificultades.FloralForjaAccess.completo(matrix));
    }

    // La herrería reconoce la Mejora Floral por su id (sirven las hechas con la descripción vieja) y no mejora items custom
    @Test void theSmithingTableOnlyTakesTheFloralUpgradeOnPlainNetherite() {
        ItemStack mejora = item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, null, "mejora_floral");
        ItemStack espada = item(Material.NETHERITE_SWORD, 1, null, null);
        assertTrue(Dificultades.FloralForjaAccess.forjable(mejora, espada));
        assertFalse(Dificultades.FloralForjaAccess.forjable(item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, null, null), espada));
        assertFalse(Dificultades.FloralForjaAccess.forjable(mejora, item(Material.NETHERITE_SWORD, 1, null, "espada_floral")));
        assertFalse(Dificultades.FloralForjaAccess.forjable(mejora, item(Material.NETHERITE_PICKAXE, 1, null, "pico_celestita")));

        ItemStack warden = item(Material.NETHERITE_HELMET, 1, null, null);
        when(warden.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey("quasoplugin", "warden_armor"))).thenReturn(true);
        assertFalse(Dificultades.FloralForjaAccess.forjable(mejora, warden));
        assertFalse(Dificultades.FloralForjaAccess.forjable(null, espada));
    }
}
