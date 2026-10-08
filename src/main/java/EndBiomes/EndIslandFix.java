package EndBiomes;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Random;

// Las islas chicas del End las pone el juego desde el chunk de al lado y a veces caen en un chunk que ya pasó por el
// populator: quedaban mitad end stone y mitad bioma. Cuando un chunk nuevo termina de generarse (ya no le va a caer
// nada más) se le pasa al suelo del bioma la end stone que quedó al aire, con sus plantas
public class EndIslandFix implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        World world = e.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;
        repasar(e.getChunk(), EndBiomeMap.forSeed(world.getSeed()));
    }

    static int repasar(Chunk chunk, EndBiomeMap map) {
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        EndBiome[] columns = new EndBiome[256];
        boolean any = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                columns[x << 4 | z] = map.biomeAt(baseX + x, baseZ + z);
                if (columns[x << 4 | z] != null) any = true;
            }
        }
        if (!any) return 0;

        World world = chunk.getWorld();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
        EndSuelo.Bloques blocks = bloques(chunk);
        EndSuelo.Ruido ruido = EndSuelo.Ruido.of(world.getSeed());
        Random random = new Random(world.getSeed() ^ ((long) chunk.getX() * 341873128712L + (long) chunk.getZ() * 132897987541L));
        int fixed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                EndBiome biome = columns[x << 4 | z];
                if (biome == null) continue;
                int wx = baseX + x, wz = baseZ + z;
                // Solo las columnas que todavía tienen end stone al aire: el resto ya lo hizo el populator
                if (!leftover(snapshot, x, z)) continue;
                int top = EndSuelo.suelo(blocks, random, ruido, wx, wz, biome);
                if (top == Integer.MIN_VALUE) continue;
                EndSuelo.planta(blocks, random, wx, top + 1, wz, biome);
                fixed++;
            }
        }
        return fixed;
    }

    private static boolean leftover(ChunkSnapshot snapshot, int x, int z) {
        for (int y = EndSuelo.MAX_Y; y > 1; y--) {
            if (snapshot.getBlockType(x, y, z) == Material.END_STONE && snapshot.getBlockType(x, y + 1, z).isAir()) return true;
        }
        return false;
    }

    // Los bloques del chunk ya cargado, sin físicas (así no se rompen las plantas al ponerlas)
    private static EndSuelo.Bloques bloques(Chunk chunk) {
        World world = chunk.getWorld();
        int cx = chunk.getX(), cz = chunk.getZ();
        return new EndSuelo.Bloques() {
            public boolean dentro(int x, int y, int z) {
                return (x >> 4) == cx && (z >> 4) == cz && y >= world.getMinHeight() && y < world.getMaxHeight();
            }

            public Material get(int x, int y, int z) {
                return dentro(x, y, z) ? chunk.getBlock(x & 15, y, z & 15).getType() : Material.AIR;
            }

            public void set(int x, int y, int z, Material type) {
                if (dentro(x, y, z)) chunk.getBlock(x & 15, y, z & 15).setType(type, false);
            }

            public void set(int x, int y, int z, BlockData data) {
                if (dentro(x, y, z)) chunk.getBlock(x & 15, y, z & 15).setBlockData(data, false);
            }
        };
    }
}
