package Handlers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomSpawnerHandlerTest {

    // Los items de antes (sin la línea) son rompibles; "Rompible: No" los deja irrompibles
    @Test
    void breakableComesFromTheLore() {
        assertTrue(CustomSpawnerHandler.readBreakable(null));
        assertTrue(CustomSpawnerHandler.readBreakable(List.of("§7Spawn Range: §f4")));
        assertTrue(CustomSpawnerHandler.readBreakable(List.of("§7Rompible: §fSí")));
        assertFalse(CustomSpawnerHandler.readBreakable(List.of("§7Spawn Range: §f4", "§7Rompible: §fNo")));
    }
}
