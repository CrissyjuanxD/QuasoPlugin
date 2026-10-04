package Encantamientos;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

// Paso Ígneo: el datapack pone la obsidiana sobre la lava (como el Paso Helado con el hielo) y acá se lleva la cuenta
// de cada bloque: a los 4-7 segundos se vuelve obsidiana llorosa (aviso) y unos 3 segundos después otra vez lava
public class PasoIgneo implements Listener {

    private static final int CRACK_MIN = 80;
    private static final int CRACK_EXTRA = 60;
    private static final int MELT_MIN = 50;
    private static final int MELT_EXTRA = 20;

    private record Timer(long crackAt, long meltAt) {}

    private final JavaPlugin plugin;
    private final Map<Block, Timer> placed = new HashMap<>();
    private final Random random = new Random();
    private long tick = 0;

    public PasoIgneo(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::update, 10L, 10L);
    }

    // Solo la obsidiana que pone un encantamiento sobre lava (el juego nunca forma obsidiana con una entidad)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(EntityBlockFormEvent e) {
        if (e.getNewState().getType() != Material.OBSIDIAN || !(e.getEntity() instanceof LivingEntity)) return;
        if (e.getBlock().getType() != Material.LAVA) return;
        long crack = tick + CRACK_MIN + random.nextInt(CRACK_EXTRA);
        placed.put(e.getBlock(), new Timer(crack, crack + MELT_MIN + random.nextInt(MELT_EXTRA)));
    }

    private void update() {
        tick += 10;
        Iterator<Map.Entry<Block, Timer>> it = placed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Timer> entry = it.next();
            Block block = entry.getKey();
            Material type = block.getType();
            if (type != Material.OBSIDIAN && type != Material.CRYING_OBSIDIAN) {
                it.remove();
                continue;
            }
            Timer timer = entry.getValue();
            if (type == Material.OBSIDIAN && tick >= timer.crackAt()) {
                block.setType(Material.CRYING_OBSIDIAN, false);
            } else if (type == Material.CRYING_OBSIDIAN && tick >= timer.meltAt() && !someoneOn(block)) {
                // Si hay alguien parado encima espera a que se mueva, así nadie se hunde en la lava quieto
                block.setType(Material.LAVA);
                it.remove();
            }
        }
    }

    private boolean someoneOn(Block block) {
        for (Player player : block.getWorld().getNearbyPlayers(block.getLocation().add(0.5, 1, 0.5), 1.5)) {
            double feet = player.getLocation().getY();
            if (feet >= block.getY() + 0.9 && feet <= block.getY() + 1.6
                    && Math.abs(player.getLocation().getX() - block.getX() - 0.5) < 0.85
                    && Math.abs(player.getLocation().getZ() - block.getZ() - 0.5) < 0.85) return true;
        }
        return false;
    }

    // Romperla no da obsidiana (sería una granja infinita) y vuelve a ser lava, como el hielo del Paso Helado
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (placed.remove(block) == null) return;
        e.setDropItems(false);
        e.setExpToDrop(0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType().isAir()) block.setType(Material.LAVA);
        });
    }

    // Lo que quede en un chunk que se descarga vuelve a ser lava en el momento
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        Iterator<Block> it = placed.keySet().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (!block.getWorld().equals(chunk.getWorld())) continue;
            if (block.getX() >> 4 != chunk.getX() || block.getZ() >> 4 != chunk.getZ()) continue;
            restore(block);
            it.remove();
        }
    }

    // Al apagar el server todo vuelve a ser lava
    public void restoreAll() {
        placed.keySet().forEach(this::restore);
        placed.clear();
    }

    private void restore(Block block) {
        Material type = block.getType();
        if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) block.setType(Material.LAVA, false);
    }
}
