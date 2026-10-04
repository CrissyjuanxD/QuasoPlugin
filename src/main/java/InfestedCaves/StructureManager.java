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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;

public class StructureManager {
    private final JavaPlugin plugin;
    private Clipboard templeSchematic;

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Carga TemploRunico.schem de la carpeta schematics del plugin
    public void loadSchematics() {
        File schemFolder = new File(plugin.getDataFolder(), "schematics");
        schemFolder.mkdirs();
        templeSchematic = loadSchem(new File(schemFolder, "TemploRunico.schem"));
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

    // Devuelve false si el schematic todavía no cargó, para intentarlo de nuevo la próxima vez
    // Se pega centrado en el 0 0 y apoyado en el piso del cráter del spawn (la build con el portal de salida)
    public boolean pasteTempleAtSpawn(World world) {
        if (templeSchematic == null) return false;
        BlockVector3 size = templeSchematic.getDimensions();
        int x = -size.x() / 2;
        int y = WardenGenerator.SPAWN_Y + 1;
        int z = -size.z() / 2;
        plugin.getLogger().info("Pegando TemploRunico en " + x + ", " + y + ", " + z);
        pasteSchematicAsync(templeSchematic, new Location(world, x, y, z), null);
        return true;
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