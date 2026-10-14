package Encantamientos;

import InfestedCaves.WardenBiome;
import Trabajos.TrabajosXp;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Veta (pico, segundo encantamiento de Minería): al romper un mineral se rompe toda la veta pegada a él (también en
// diagonal), hasta 12 bloques con el nivel I y 32 con el II. Cada bloque se rompe como si lo rompiera el jugador (gasta
// el pico, da la XP de Minería y respeta las protecciones). Agachado rompe uno solo. No funciona ni se puede poner en
// La Excavadora (el pico 3x3)
public class Veta implements Listener {

    static final int[] MAXIMO = {12, 32};
    private static final int POR_TICK = 4;
    private static final NamespacedKey EXCAVADORA = new NamespacedKey("quasoplugin", "la_excavadora");

    private final JavaPlugin plugin;
    // Mientras está rompiendo la veta no vuelve a empezar con los bloques que rompe él mismo
    private boolean minando;

    public Veta(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinar(BlockBreakEvent e) {
        if (minando) return;
        Player player = e.getPlayer();
        ItemStack pico = player.getInventory().getItemInMainHand();
        if (player.isSneaking() || esExcavadora(pico)) return;
        int nivel = QuasoEnchant.VETA.level(pico);
        Block inicio = e.getBlock();
        if (nivel <= 0 || familia(inicio) == null || TrabajosXp.puesto(inicio)) return;

        List<Block> veta = veta(inicio, MAXIMO[Math.min(nivel, MAXIMO.length) - 1]);
        if (!veta.isEmpty()) minar(player, veta);
    }

    private void minar(Player player, List<Block> veta) {
        String familia = familia(veta.getFirst());
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                for (int n = 0; n < POR_TICK && i < veta.size(); n++, i++) {
                    ItemStack pico = player.getInventory().getItemInMainHand();
                    if (!player.isOnline() || QuasoEnchant.VETA.level(pico) <= 0 || esExcavadora(pico)) {
                        cancel();
                        return;
                    }
                    Block bloque = veta.get(i);
                    if (!familia.equals(familia(bloque))) continue;
                    minando = true;
                    try {
                        player.breakBlock(bloque);
                    } finally {
                        minando = false;
                    }
                }
                if (i >= veta.size()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Los minerales de la misma familia pegados al primero, sin él (ese lo rompe el jugador) y sin los que puso un jugador
    static List<Block> veta(Block inicio, int maximo) {
        String familia = familia(inicio);
        Set<Block> vistos = new HashSet<>();
        Deque<Block> cola = new ArrayDeque<>();
        List<Block> veta = new ArrayList<>();
        vistos.add(inicio);
        cola.add(inicio);
        while (!cola.isEmpty() && veta.size() < maximo) {
            Block actual = cola.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (veta.size() >= maximo) break;
                        Block vecino = actual.getRelative(dx, dy, dz);
                        if (!vistos.add(vecino) || !familia.equals(familia(vecino)) || TrabajosXp.puesto(vecino)) continue;
                        veta.add(vecino);
                        cola.add(vecino);
                    }
                }
            }
        }
        return veta;
    }

    // De qué mineral es el bloque, sin importar si es de deepslate (null si no es un mineral). Los Minerales Profundos
    // (terracotas) solo cuentan adentro de la Warden Cave
    static String familia(Block block) {
        Material tipo = block.getType();
        String nombre = tipo.name();
        if (nombre.endsWith("_ORE")) return nombre.replace("DEEPSLATE_", "");
        if (tipo == Material.ANCIENT_DEBRIS) return nombre;
        if (WardenBiome.fromOre(tipo) != null && block.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return nombre;
        return null;
    }

    static boolean esExcavadora(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(EXCAVADORA, PersistentDataType.BYTE);
    }

    // En el yunque no se le puede poner Veta a La Excavadora
    @EventHandler
    public void onYunque(PrepareAnvilEvent e) {
        ItemStack resultado = e.getResult();
        if (esExcavadora(e.getInventory().getFirstItem()) && QuasoEnchant.VETA.level(resultado) > 0) e.setResult(null);
    }
}
