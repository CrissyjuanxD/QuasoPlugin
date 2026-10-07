package Trabajos;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Recuerda los bloques que puso un jugador (o que se formaron solos, como la piedra de un generador) para que romperlos
// no dé XP de Minería, Leñador ni Granjero. Va en la PDC de cada chunk y en memoria mientras el chunk está cargado
final class BloquesColocados implements Listener {

    private record ChunkId(UUID mundo, long chunk) {}

    private final NamespacedKey clave;
    private final Map<ChunkId, Set<Integer>> cache = new HashMap<>();
    private final Set<ChunkId> cambiados = new HashSet<>();

    BloquesColocados(JavaPlugin plugin) {
        this.clave = new NamespacedKey(plugin, "trabajos_colocados");
    }

    // x y z dentro del chunk en un int: 4 bits para x, 4 para z y 12 para la altura
    static int posicion(int x, int y, int z) {
        return ((y + 2048) << 8) | ((x & 15) << 4) | (z & 15);
    }

    private static ChunkId id(Block block) {
        long chunk = ((long) (block.getZ() >> 4) << 32) | ((block.getX() >> 4) & 0xFFFFFFFFL);
        return new ChunkId(block.getWorld().getUID(), chunk);
    }

    private Set<Integer> del(Block block) {
        return cache.computeIfAbsent(id(block), id -> {
            Set<Integer> set = new HashSet<>();
            int[] guardados = block.getChunk().getPersistentDataContainer().get(clave, PersistentDataType.INTEGER_ARRAY);
            if (guardados != null) for (int pos : guardados) set.add(pos);
            return set;
        });
    }

    boolean esColocado(Block block) {
        return del(block).contains(posicion(block.getX(), block.getY(), block.getZ()));
    }

    void marcar(Block block) {
        if (del(block).add(posicion(block.getX(), block.getY(), block.getZ()))) cambiados.add(id(block));
    }

    void quitar(Block block) {
        if (del(block).remove(posicion(block.getX(), block.getY(), block.getZ()))) cambiados.add(id(block));
    }

    private void escribir(Chunk chunk, Set<Integer> set) {
        if (set.isEmpty()) {
            chunk.getPersistentDataContainer().remove(clave);
            return;
        }
        int[] datos = new int[set.size()];
        int i = 0;
        for (int pos : set) datos[i++] = pos;
        chunk.getPersistentDataContainer().set(clave, PersistentDataType.INTEGER_ARRAY, datos);
    }

    private void guardar(World world) {
        Iterator<ChunkId> it = cambiados.iterator();
        while (it.hasNext()) {
            ChunkId id = it.next();
            if (!id.mundo().equals(world.getUID())) continue;
            int x = (int) id.chunk();
            int z = (int) (id.chunk() >> 32);
            if (!world.isChunkLoaded(x, z)) continue;
            escribir(world.getChunkAt(x, z), cache.getOrDefault(id, Set.of()));
            it.remove();
        }
    }

    void guardarTodo(Iterable<World> mundos) {
        for (World world : mundos) guardar(world);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        long llave = ((long) chunk.getZ() << 32) | (chunk.getX() & 0xFFFFFFFFL);
        ChunkId id = new ChunkId(chunk.getWorld().getUID(), llave);
        Set<Integer> set = cache.remove(id);
        if (set != null && cambiados.remove(id)) escribir(chunk, set);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSave(WorldSaveEvent event) {
        guardar(event.getWorld());
    }

    // La piedra y el basalto de los generadores cuentan como puestos
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        if (TrabajosXp.seRastrea(event.getNewState().getType())) marcar(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        quitar(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) quitar(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) quitar(block);
    }

    // Si un pistón empuja un bloque puesto, la marca viaja con él (si no, empujarlo lo volvía "natural")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        mover(event.getBlocks(), frente(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        mover(event.getBlocks(), frente(event.getBlock()).getOppositeFace());
    }

    private static BlockFace frente(Block piston) {
        return piston.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.SELF;
    }

    private void mover(List<Block> bloques, BlockFace hacia) {
        if (hacia == BlockFace.SELF) return;
        List<Block> destinos = new ArrayList<>();
        for (Block block : bloques) {
            if (!esColocado(block)) continue;
            quitar(block);
            destinos.add(block.getRelative(hacia));
        }
        for (Block destino : destinos) marcar(destino);
    }
}
