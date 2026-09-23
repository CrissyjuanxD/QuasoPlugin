package InfestedCaves;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StructureManager {
    private final JavaPlugin plugin;
    private Clipboard templeSchematic;
    private Clipboard ancientCitySchematic;

    private final Set<Long> processedCells = ConcurrentHashMap.newKeySet();
    private final Set<Long> occupiedCells  = ConcurrentHashMap.newKeySet();

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Carga TemploRunico.schem y AncientCity.schem de la carpeta schematics del plugin
    public void loadSchematics() {
        File schemFolder = new File(plugin.getDataFolder(), "schematics");
        schemFolder.mkdirs();
        templeSchematic      = loadSchem(new File(schemFolder, "TemploRunico.schem"));
        ancientCitySchematic = loadSchem(new File(schemFolder, "AncientCity.schem"));
    }

    private Clipboard loadSchem(File file) {
        if (!file.exists()) {
            plugin.getLogger().warning("Schematic no encontrado: " + file.getName());
            return null;
        }
        try (ClipboardReader reader = ClipboardFormats.findByFile(file).getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (Exception e) {
            plugin.getLogger().severe("Error cargando " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public void pasteTempleAtSpawn(World world) {
        if (templeSchematic == null) return;
        plugin.getLogger().info("Pegando TemploRunico en 0, -56, 0");
        pasteSchematicAsync(templeSchematic, new Location(world, 0, -56, 0), null);
    }

    // Al generar un chunk nuevo revisa si le toca una ciudad y la pega una sola vez por celda
    public void tryGenerateAncientCity(Chunk chunk) {
        if (ancientCitySchematic == null) return;

        int blockX = chunk.getX() * 16 + 8;
        int blockZ = chunk.getZ() * 16 + 8;

        AncientCityLocator.CityInfo info =
                AncientCityLocator.findCityNear(chunk.getWorld().getSeed(), blockX, blockZ);
        if (info == null) return;

        long cellKey = AncientCityLocator.cellKey(info.cellX, info.cellZ);
        if (!processedCells.add(cellKey)) return;

        if (isNearOccupiedCell(info.cellX, info.cellZ)) return;
        occupiedCells.add(cellKey);

        World world = chunk.getWorld();
        plugin.getLogger().info("AncientCity programada en celda (" + info.cellX + "," + info.cellZ
                + "), origen=" + info.originX + "," + info.originY + "," + info.originZ
                + " centro=" + info.centerX + "," + info.centerZ);

        preloadChunksThenPaste(world, info);
    }

    // Carga todos los chunks que ocupa la ciudad antes de pegarla
    private void preloadChunksThenPaste(World world, AncientCityLocator.CityInfo info) {
        int radius = AncientCityLocator.CLEAR_RADIUS + AncientCityLocator.CLEAR_TRANSITION;
        int minCX = Math.floorDiv(info.centerX - radius, 16);
        int maxCX = Math.floorDiv(info.centerX + radius, 16);
        int minCZ = Math.floorDiv(info.centerZ - radius, 16);
        int maxCZ = Math.floorDiv(info.centerZ + radius, 16);

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++)
            for (int cz = minCZ; cz <= maxCZ; cz++)
                futures.add(world.getChunkAtAsync(cx, cz, true));

        plugin.getLogger().info("AncientCity: esperando " + futures.size() + " chunks...");

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("AncientCity: pegando estructura en "
                            + info.originX + "," + info.originY + "," + info.originZ);
                    Location pasteLoc = new Location(world, info.originX, info.originY, info.originZ);
                    pasteSchematicAsync(ancientCitySchematic, pasteLoc, () ->
                            plugin.getLogger().info("AncientCity generada correctamente."));
                }))
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Error pre-cargando chunks para AncientCity: " + ex.getMessage());
                    return null;
                });
    }

    private boolean isNearOccupiedCell(long cellX, long cellZ) {
        for (long dx = -1; dx <= 1; dx++)
            for (long dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (occupiedCells.contains(AncientCityLocator.cellKey(cellX + dx, cellZ + dz)))
                    return true;
            }
        return false;
    }

    // Pega el schematic con FAWE en async alineando la esquina del schem con la ubicación
    private void pasteSchematicAsync(Clipboard clipboard, Location loc, Runnable onComplete) {
        TaskManager.taskManager().async(() -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(loc.getWorld()))
                    .fastMode(true)
                    .build()) {

                BlockVector3 clipMin    = clipboard.getMinimumPoint();
                BlockVector3 clipOrigin = clipboard.getOrigin();

                int offX = clipOrigin.x() - clipMin.x();
                int offY = clipOrigin.y() - clipMin.y();
                int offZ = clipOrigin.z() - clipMin.z();

                BlockVector3 target = BlockVector3.at(
                        (int) loc.getX() + offX,
                        (int) loc.getY() + offY,
                        (int) loc.getZ() + offZ
                );

                plugin.getLogger().info("AncientCity paste: clipMin=" + clipMin
                        + " clipOrigin=" + clipOrigin + " offset=(" + offX + "," + offY + "," + offZ + ")"
                        + " target=" + target);

                Operation op = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(target)
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(op);
            } catch (Exception e) {
                plugin.getLogger().severe("Error pegando schematic: " + e.getMessage());
                e.printStackTrace();
            }
            if (onComplete != null)
                Bukkit.getScheduler().runTask(plugin, onComplete);
        });
    }
}