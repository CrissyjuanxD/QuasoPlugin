package Encantamientos;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

// Cosecha (azada, encantamiento de Granjero): el cultivo maduro que rompes con la azada se vuelve a plantar con una
// semilla de lo que soltó. Nivel II cosecha también los maduros de alrededor (3x3) y nivel III un 5x5
public class Cosecha implements Listener {

    private final JavaPlugin plugin;
    // Los cultivos que se acaban de romper con Cosecha, con lo que había, hasta que sueltan sus cosas
    private final Map<Location, BlockData> porReplantar = new HashMap<>();
    private boolean cosechando;

    public Cosecha(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCosechar(BlockBreakEvent e) {
        Player player = e.getPlayer();
        int nivel = QuasoEnchant.COSECHA.level(player.getInventory().getItemInMainHand());
        Block block = e.getBlock();
        if (nivel <= 0 || semilla(block.getType()) == null || !maduro(block)) return;

        Location lugar = block.getLocation();
        porReplantar.put(lugar, block.getBlockData().clone());
        // Si no suelta nada (otro plugin le sacó las drops) se olvida al tick siguiente
        plugin.getServer().getScheduler().runTask(plugin, () -> porReplantar.remove(lugar));

        if (cosechando || nivel < 2) return;
        int radio = nivel >= 3 ? 2 : 1;
        cosechando = true;
        try {
            for (int dx = -radio; dx <= radio; dx++) {
                for (int dz = -radio; dz <= radio; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block otro = block.getRelative(dx, 0, dz);
                    if (semilla(otro.getType()) != null && maduro(otro)) player.breakBlock(otro);
                }
            }
        } finally {
            cosechando = false;
        }
    }

    // Saca una semilla de lo que soltó y vuelve a plantar el cultivo recién nacido
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSoltar(BlockDropItemEvent e) {
        BlockData antes = porReplantar.remove(e.getBlock().getLocation());
        if (antes == null) return;
        Material semilla = semilla(antes.getMaterial());
        for (Item item : e.getItems()) {
            ItemStack stack = item.getItemStack();
            if (stack.getType() != semilla || stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) continue;
            if (stack.getAmount() <= 1) e.getItems().remove(item);
            else {
                stack.setAmount(stack.getAmount() - 1);
                item.setItemStack(stack);
            }
            Block block = e.getBlock();
            plugin.getServer().getScheduler().runTask(plugin, () -> replantar(block, antes));
            return;
        }
    }

    private static void replantar(Block block, BlockData antes) {
        if (!block.getType().isAir() || !(antes.clone() instanceof Ageable brote)) return;
        brote.setAge(0);
        if (brote.isSupported(block)) block.setBlockData(brote);
    }

    private static boolean maduro(Block block) {
        return block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }

    // Lo que hay que gastar para volver a plantarlo (null si no es un cultivo que se replanta)
    static Material semilla(Material cultivo) {
        return switch (cultivo) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            case COCOA -> Material.COCOA_BEANS;
            default -> null;
        };
    }
}
