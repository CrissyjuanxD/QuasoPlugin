package SistemaTumbas;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraveStorageTest {
    @TempDir Path directory;

    @Test void aFileBiggerThanThreeMegabytesStillLoads() throws Exception {
        Path file = directory.resolve("tumbas_data.yml");
        StringBuilder yaml = new StringBuilder("graves:\n");
        String padding = "x".repeat(1000);
        for (int i = 0; i < 4000; i++) {
            yaml.append("  t").append(i).append(":\n    owner: '").append(padding).append("'\n");
        }
        Files.writeString(file, yaml);
        assertTrue(Files.size(file) > 3 * 1024 * 1024);
        assertEquals(4000, new GraveStorage(file.toFile()).records().size());
    }

    @Test void unknownWorldRecordsKeepTheirItemsAndGetAnOriginalFileBackup() throws Exception {
        Path file = directory.resolve("tumbas_data.yml");
        String original = """
                graves:
                  pendiente:
                    owner: jugador
                    location:
                      ==: org.bukkit.Location
                      world: mundo_ausente
                      x: 10.5
                      y: 64.0
                      z: -20.5
                    items:
                      - ==: org.bukkit.inventory.ItemStack
                        schema_version: 1
                        id: minecraft:diamond
                        count: 3
                        components:
                          minecraft:custom_data:
                            monedero_uuid: 'abc-123'
                """;
        Files.writeString(file, original);
        GraveStorage storage = new GraveStorage(file.toFile());
        Object expected = storage.records().get("pendiente");
        storage.save();
        assertEquals(expected, new GraveStorage(file.toFile()).records().get("pendiente"));
        assertEquals(original, Files.readString(directory.resolve("tumbas_data.yml.pre-26.2.bak")));
        storage.save();
        assertEquals(original, Files.readString(directory.resolve("tumbas_data.yml.pre-26.2.bak")));
    }

    @Test void activeLocationsAreSavedAsCoordinatesAndKeepItemMetadata() throws Exception {
        GraveStorage storage = new GraveStorage(directory.resolve("tumbas_data.yml").toFile());
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getName()).thenReturn("wardencave");
        when(world.getUID()).thenReturn(worldId);
        Grave grave = mock(Grave.class);
        UUID id = UUID.randomUUID();
        when(grave.getId()).thenReturn(id);
        when(grave.getOwner()).thenReturn(UUID.randomUUID());
        when(grave.getOwnerName()).thenReturn("Prueba");
        when(grave.getLocation()).thenReturn(new Location(world, 1.5, -50, -2.5, 90, 20));
        ItemStack item = mock(ItemStack.class);
        when(item.serialize()).thenReturn(Map.of("type", "DIAMOND", "amount", 3, "components", Map.of("custom_data", "propiedad-original")));
        when(grave.getItems()).thenReturn(List.of(item));
        storage.put(grave);
        storage.save();
        Map<?, ?> saved = (Map<?, ?>) new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().get(id.toString());
        Map<?, ?> location = (Map<?, ?>) saved.get("location");
        assertFalse(location.containsKey("=="));
        assertEquals(worldId.toString(), location.get("world-uuid"));
        assertEquals(-2.5, location.get("z"));
        Map<?, ?> savedItem = (Map<?, ?>) ((List<?>) saved.get("items")).getFirst();
        assertEquals(3, savedItem.get("amount"));
        assertEquals(Map.of("custom_data", "propiedad-original"), savedItem.get("components"));
    }

    @Test void malformedYamlIsRejectedWithoutChangingTheOriginalFile() throws Exception {
        Path file = directory.resolve("tumbas_data.yml");
        Files.writeString(file, "graves: [sin terminar");
        assertThrows(RuntimeException.class, () -> new GraveStorage(file.toFile()));
        assertEquals("graves: [sin terminar", Files.readString(file));
    }
}
