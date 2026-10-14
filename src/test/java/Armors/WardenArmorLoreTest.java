package Armors;

import items.WardenCaveItems;
import net.md_5.bungee.api.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardenArmorLoreTest {

    // Cada pieza lleva el color de su bioma, como su alma: casco cian, peto morado, pantalón gris y botas verde
    @Test void eachPieceUsesTheColorOfItsBiome() {
        for (WardenCaveItems.Variant bioma : WardenCaveItems.Variant.values()) {
            List<String> lore = WardenArmor.lore(bioma);
            String color = ChatColor.of(bioma.color()).toString();
            assertTrue(lore.stream().anyMatch(line -> line.startsWith(color + "> ")), bioma.name());
            assertTrue(lore.stream().anyMatch(line -> line.contains("Pieza " + bioma.biome())), bioma.name());
            assertTrue(lore.stream().anyMatch(line -> line.contains("+2 Corazones")), bioma.name());
            assertFalse(String.join("", lore).contains(ChatColor.of("#008b8b").toString()), bioma.name());
        }
    }

    @Test void shadesStayInsideTheColorRange() {
        assertEquals("#2c9f9f", WardenArmor.tono("#3ad4d4", 0.75));
        assertEquals("#3ad4d4", WardenArmor.tono("#3ad4d4", 1.0));
        assertEquals("#ffffff", WardenArmor.tono("#3ad4d4", 2.0));
    }
}
