package Encantamientos;

import Trabajos.TrabajosXp;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Talador (hacha, encantamiento de Leñador): al talar un tronco cae todo lo que está arriba y pegado a él. Cada tronco
// se rompe como si lo rompiera el jugador (gasta el hacha, da la XP de Leñador y respeta las protecciones). Agachado
// tala uno solo. Los troncos puestos por jugadores no caen y en el Overworld y el Nether tiene que haber hojas (o
// verrugas) naturales, así no se cae una casa de troncos
public class Talador implements Listener {

    // Troncos por nivel: I árboles chicos, II los grandes, III los gigantes
    static final int[] MAXIMO = {24, 64, 160};
    // Hasta dónde se busca el árbol (para ver si tiene hojas) aunque el nivel tale menos
    private static final int LIMITE = 400;
    private static final int POR_TICK = 6;

    private final JavaPlugin plugin;
    // Mientras está talando no vuelve a empezar con los troncos que rompe él mismo
    private boolean talando;

    public Talador(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTalar(BlockBreakEvent e) {
        if (talando) return;
        Player player = e.getPlayer();
        if (player.isSneaking()) return;
        int nivel = QuasoEnchant.TALADOR.level(player.getInventory().getItemInMainHand());
        Block inicio = e.getBlock();
        if (nivel <= 0 || !esTronco(inicio.getType()) || TrabajosXp.puesto(inicio)) return;

        List<Block> arbol = arbol(inicio, MAXIMO[Math.min(nivel, MAXIMO.length) - 1]);
        if (!arbol.isEmpty()) talar(player, arbol);
    }

    private void talar(Player player, List<Block> arbol) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                for (int n = 0; n < POR_TICK && i < arbol.size(); n++, i++) {
                    ItemStack hacha = player.getInventory().getItemInMainHand();
                    if (!player.isOnline() || QuasoEnchant.TALADOR.level(hacha) <= 0) {
                        cancel();
                        return;
                    }
                    Block tronco = arbol.get(i);
                    if (!esTronco(tronco.getType())) continue;
                    talando = true;
                    try {
                        player.breakBlock(tronco);
                    } finally {
                        talando = false;
                    }
                }
                if (i >= arbol.size()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Los troncos pegados (también en diagonal) desde el que se rompió hacia arriba, sin el primero (ese lo rompe el
    // jugador), de abajo para arriba y como mucho "maximo". Se revisa el árbol entero (hasta LIMITE troncos) para
    // encontrar las hojas aunque el nivel no alcance para talarlo todo. Vacío si no parece un árbol
    static List<Block> arbol(Block inicio, int maximo) {
        Set<Block> vistos = new HashSet<>();
        Deque<Block> cola = new ArrayDeque<>();
        List<Block> troncos = new ArrayList<>();
        vistos.add(inicio);
        cola.add(inicio);
        boolean hojas = dimensionNueva(inicio.getWorld());

        while (!cola.isEmpty() && troncos.size() < LIMITE) {
            Block actual = cola.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block vecino = actual.getRelative(dx, dy, dz);
                        if (vecino.getY() < inicio.getY() || !vistos.add(vecino)) continue;
                        if (!hojas && hojaNatural(vecino)) hojas = true;
                        if (troncos.size() >= LIMITE || !esTronco(vecino.getType()) || TrabajosXp.puesto(vecino)) continue;
                        troncos.add(vecino);
                        cola.add(vecino);
                    }
                }
            }
        }
        if (!hojas) return List.of();
        // Los de abajo primero, así el árbol "cae" de abajo para arriba
        troncos.sort(Comparator.comparingInt(Block::getY));
        return troncos.size() > maximo ? new ArrayList<>(troncos.subList(0, maximo)) : troncos;
    }

    // Troncos, madera y tallos, también pelados (los árboles del End son de troncos pelados)
    static boolean esTronco(Material tipo) {
        String nombre = tipo.name();
        return nombre.endsWith("_LOG") || nombre.endsWith("_WOOD") || nombre.endsWith("_STEM") || nombre.endsWith("_HYPHAE")
                || tipo == Material.MANGROVE_ROOTS;
    }

    private static boolean hojaNatural(Block block) {
        Material tipo = block.getType();
        if (tipo == Material.NETHER_WART_BLOCK || tipo == Material.WARPED_WART_BLOCK || tipo == Material.SHROOMLIGHT) return true;
        return block.getBlockData() instanceof Leaves leaves && !leaves.isPersistent();
    }

    // En la Warden Cave y el End los árboles del plugin tienen copas de lana, vidrio o froglights: alcanza con que los
    // troncos no los haya puesto un jugador
    private static boolean dimensionNueva(World world) {
        return world.getName().equals(QuasoPlugin.WORLD_NAME) || world.getEnvironment() == World.Environment.THE_END;
    }
}
