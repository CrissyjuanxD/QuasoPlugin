package SistemaTumbas;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GravesManagerTest {
    @TempDir Path directory;
    private final UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private JavaPlugin plugin;
    private World world;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ItemStack> items;
    private MockedConstruction<Grave> graves;
    private Runnable expiry;
    private List<Runnable> visualTasks;
    private Chunk chunk;

    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        Logger logger = Logger.getLogger("GravesTest");
        when(plugin.getLogger()).thenReturn(logger);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        visualTasks = new ArrayList<>();
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> {
            visualTasks.add(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            expiry = call.getArgument(1);
            return mock(BukkitTask.class);
        });
        world = mock(World.class);
        when(world.getName()).thenReturn("wardencave");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getKey()).thenReturn(NamespacedKey.minecraft("wardencave"));
        chunk = mock(Chunk.class);
        when(world.getChunkAt(any(Location.class))).thenReturn(chunk);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getLogger).thenReturn(logger);
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.serialize()).thenReturn(Map.of("v", 5000, "type", "DIAMOND", "amount", 3));
        items = mockStatic(ItemStack.class);
        items.when(() -> ItemStack.deserialize(anyMap())).thenReturn(diamond);
        graves = mockConstruction(Grave.class, (grave, context) -> {
            when(grave.getId()).thenReturn((UUID) context.arguments().get(0));
            when(grave.getOwner()).thenReturn((UUID) context.arguments().get(1));
            when(grave.getOwnerName()).thenReturn((String) context.arguments().get(2));
            when(grave.getLocation()).thenReturn((Location) context.arguments().get(3));
            when(grave.getCreationTime()).thenReturn((Long) context.arguments().get(4));
            when(grave.getExpiryTime()).thenReturn((Long) context.arguments().get(5));
            when(grave.getItems()).thenReturn((List<ItemStack>) context.arguments().get(6));
        });
    }

    @AfterEach void cleanup() { graves.close(); items.close(); bukkit.close(); }

    private void fixture(String name) throws Exception {
        Files.writeString(directory.resolve("tumbas_data.yml"), """
                graves:
                  11111111-1111-4111-8111-111111111111:
                    owner: 22222222-2222-4222-8222-222222222222
                    ownerName: Prueba
                    location:
                      ==: org.bukkit.Location
                      world: %s
                      x: 0.5
                      y: -55.0
                      z: 0.5
                    creationTime: 1
                    expiryTime: 2
                    items:
                      - ==: org.bukkit.inventory.ItemStack
                        v: 5000
                        type: DIAMOND
                        amount: 3
                """.formatted(name));
    }

    @Test void aLegacyGraveWaitsForItsWorldAndDoesNotLoseItsExpiredItemsOnSave() throws Exception {
        fixture("wardencave");
        GravesManager manager = assertDoesNotThrow(() -> new GravesManager(plugin));
        assertTrue(manager.getGraves().isEmpty());
        expiry.run();
        manager.saveData();
        Map<?, ?> record = (Map<?, ?>) new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().get(id.toString());
        assertEquals(3, ((Map<?, ?>) ((List<?>) record.get("items")).getFirst()).get("amount"));
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
    }

    @Test void worldLoadRestoresTheLegacyGraveAndDoesNotCreateDuplicateVisuals() throws Exception {
        fixture("wardencave");
        GravesManager manager = new GravesManager(plugin);
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        manager.onWorldLoad(new WorldLoadEvent(world));
        Grave grave = manager.getGraveById(id);
        assertNotNull(grave);
        assertEquals(world, grave.getLocation().getWorld());
        assertEquals(-55, grave.getLocation().getY());
        assertEquals(1, grave.getItems().size());
        manager.onWorldLoad(new WorldLoadEvent(world));
        assertEquals(1, graves.constructed().size());
        assertEquals(1, visualTasks.size());
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(world);
        visualTasks.getFirst().run();
        verify(chunk).getEntities();
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), contains("run kill @e[tag=grave_")), times(1));
        manager.saveData();
        Map<?, ?> saved = (Map<?, ?>) new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().get(id.toString());
        assertFalse(((Map<?, ?>) saved.get("location")).containsKey("=="));
        assertEquals(3, ((Map<?, ?>) ((List<?>) saved.get("items")).getFirst()).get("amount"));
    }

    @Test void unloadedWorldsKeepGravesUntilTheSameWorldReturns() throws Exception {
        fixture("wardencave");
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        GravesManager manager = new GravesManager(plugin);
        manager.onWorldUnload(new WorldUnloadEvent(world));
        assertTrue(manager.getGraves().isEmpty());
        expiry.run();
        assertTrue(new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().containsKey(id.toString()));
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(world);
        manager.onWorldLoad(new WorldLoadEvent(world));
        assertNotNull(manager.getGraveById(id));
    }

    @Test void invalidRecordsArePreservedAndDoNotPreventValidGravesFromLoading() throws Exception {
        fixture("wardencave");
        String yaml = Files.readString(directory.resolve("tumbas_data.yml"));
        Files.writeString(directory.resolve("tumbas_data.yml"), yaml + "  registro_invalido:\n    items: [objeto_pendiente]\n");
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        GravesManager manager = assertDoesNotThrow(() -> new GravesManager(plugin));
        assertEquals(1, manager.getGraves().size());
        manager.saveData();
        assertEquals(Map.of("items", List.of("objeto_pendiente")), new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().get("registro_invalido"));
    }

    @Test void removingAnActiveGraveDoesNotDeletePendingGraves() throws Exception {
        fixture("wardencave");
        String yaml = Files.readString(directory.resolve("tumbas_data.yml"));
        Files.writeString(directory.resolve("tumbas_data.yml"), yaml + "  33333333-3333-4333-8333-333333333333:\n    location:\n      world: mundo_ausente\n    items: [pendiente]\n");
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        GravesManager manager = new GravesManager(plugin);
        manager.removeGrave(id);
        GraveStorage saved = new GraveStorage(directory.resolve("tumbas_data.yml").toFile());
        assertFalse(saved.records().containsKey(id.toString()));
        assertTrue(saved.records().containsKey("33333333-3333-4333-8333-333333333333"));
    }

    @Test void unreadableItemsKeepTheirOriginalRecordInsteadOfBecomingAnEmptyGrave() throws Exception {
        fixture("wardencave");
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        items.when(() -> ItemStack.deserialize(anyMap())).thenReturn(null);
        GravesManager manager = new GravesManager(plugin);
        assertTrue(manager.getGraves().isEmpty());
        manager.saveData();
        Map<?, ?> saved = (Map<?, ?>) new GraveStorage(directory.resolve("tumbas_data.yml").toFile()).records().get(id.toString());
        assertEquals(3, ((Map<?, ?>) ((List<?>) saved.get("items")).getFirst()).get("amount"));
    }

    @Test void aDifferentWorldWithTheSameNameCannotClaimGravesSavedWithAWorldUuid() throws Exception {
        fixture("wardencave");
        UUID savedWorld = UUID.randomUUID();
        Path file = directory.resolve("tumbas_data.yml");
        Files.writeString(file, Files.readString(file).replace("world: wardencave", "world: wardencave\n      world-uuid: " + savedWorld));
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        GravesManager manager = new GravesManager(plugin);
        assertTrue(manager.getGraves().isEmpty());
        manager.onWorldLoad(new WorldLoadEvent(world));
        assertTrue(manager.getGraves().isEmpty());
        bukkit.when(() -> Bukkit.getWorld(savedWorld)).thenReturn(world);
        manager.onWorldLoad(new WorldLoadEvent(world));
        assertNotNull(manager.getGraveById(id));
    }

    @Test void aRemovedGraveCannotReappearWhenItsDeferredVisualTaskRuns() throws Exception {
        fixture("wardencave");
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(world);
        GravesManager manager = new GravesManager(plugin);
        manager.removeGrave(id);
        visualTasks.getFirst().run();
        verify(chunk, never()).getEntities();
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), contains("run summon")), never());
    }

    @Test void theModelAndItsClickBoxStayInsideTheGravesOwnBlock() throws Exception {
        fixture("wardencave");
        Path file = directory.resolve("tumbas_data.yml");
        Files.writeString(file, Files.readString(file).replace("x: 0.5", "x: 15.5").replace("z: 0.5", "z: 15.5"));
        bukkit.when(() -> Bukkit.getWorld("wardencave")).thenReturn(world);
        bukkit.when(() -> Bukkit.getWorld(world.getUID())).thenReturn(world);
        new GravesManager(plugin);
        visualTasks.getFirst().run();
        verify(chunk, times(1)).getEntities();
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), contains("positioned 15 -55 15 run summon block_display ~ ~ ~")));
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), contains("positioned 15.5 -55 15.5 run summon interaction ~ ~ ~")));
    }

    @Test void anOldConfigGetsTheNewModesWithTheMixedOneByDefault() throws Exception {
        Files.writeString(directory.resolve("tumbas_config.yml"), "anyone-can-open: false\nexpiry-minutes: 30\n");
        GravesManager manager = new GravesManager(plugin);
        assertEquals(ModoTumba.MIXTA, manager.getModo());
        assertFalse(manager.teleportsToGrave());
        String config = Files.readString(directory.resolve("tumbas_config.yml"));
        assertTrue(config.contains("minutos-privada: 20"));
        assertTrue(config.contains("minutos-abierta: 10"));
    }
}
