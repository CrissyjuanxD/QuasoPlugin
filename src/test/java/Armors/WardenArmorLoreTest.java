package Armors;

import items.EquipoLore;
import items.WardenCaveItems;
import net.md_5.bungee.api.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardenArmorLoreTest {

    // Cada pieza lleva el color de su bioma, como su alma: casco cian, peto morado, pantalón gris y botas verde
    @Test void eachPieceUsesTheColorOfItsBiome() {
        for (WardenCaveItems.Variant bioma : WardenCaveItems.Variant.values()) {
            List<String> lore = WardenArmor.lore(bioma, 4, false);
            String color = ChatColor.of(bioma.color()).toString();
            assertTrue(lore.stream().anyMatch(line -> line.startsWith(" " + color + EquipoLore.VIDA)), bioma.name());
            assertTrue(lore.stream().anyMatch(line -> line.contains("Pieza " + bioma.biome())), bioma.name());
            assertTrue(lore.stream().anyMatch(line -> line.contains("+2 corazones")), bioma.name());
            assertTrue(lore.getLast().contains("Warden"), bioma.name());
            assertFalse(String.join("", lore).contains("Elytras"), bioma.name());
        }
        assertTrue(String.join("", WardenArmor.lore(WardenCaveItems.Variant.MORADO, 9, true)).contains("Planea como unas Elytras"));
    }

    // Netherite » Floral » Warden: lo que pasó en gris, la pieza actual resaltada y lo que falta oscuro
    @Test void theProgressionLineHighlightsTheCurrentStep() {
        List<String> floral = EquipoLore.lore("#FFB3D9", List.of("Historia"), false,
                List.of(new EquipoLore.Atributo(EquipoLore.DANO, "9 de daño")), EquipoLore.Rango.FLORAL);
        String linea = floral.getLast();
        assertTrue(linea.contains(ChatColor.GRAY + "Netherite"));
        assertTrue(linea.contains(ChatColor.BOLD + "Floral"));
        assertTrue(linea.contains(ChatColor.DARK_GRAY + "Celestita"));
        assertTrue(floral.contains(ChatColor.GRAY + "En la mano:"));
    }

    @Test void numbersAndShadesReadWell() {
        assertEquals("9", EquipoLore.numero(9.0));
        assertEquals("6,5", EquipoLore.numero(6.5));
        assertEquals("1,6", EquipoLore.numero(1.6000000953674316));
        assertEquals("#2c9f9f", EquipoLore.tono("#3ad4d4", 0.75));
        assertEquals("#3ad4d4", EquipoLore.tono("#3ad4d4", 1.0));
        assertEquals("#ffffff", EquipoLore.tono("#3ad4d4", 2.0));
    }
}
