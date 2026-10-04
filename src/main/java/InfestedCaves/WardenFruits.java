package InfestedCaves;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import imp.crissyjuanxd.QuasoPlugin;
import items.WardenCaveItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

// Las frutas de los árboles de la Warden Cave: los froglights verdes (Caverna Sculk) sueltan Bayas Sculk, los perlados
// (Abismo Flotante) Frutas Abisales y las enredaderas del Pantano Profundo dan Bayas Luminosas en vez de glow berries
public class WardenFruits implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Set<Block> falling = new HashSet<>();

    public WardenFruits(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Sin Toque de Seda el froglight se rompe en frutas; con Toque de Seda sale el bloque para decorar
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFroglightBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Material type = block.getType();
        if (type != Material.VERDANT_FROGLIGHT && type != Material.PEARLESCENT_FROGLIGHT) return;
        if (!inWardenCave(block.getWorld())) return;
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;

        e.setDropItems(false);
        ItemStack fruit = type == Material.VERDANT_FROGLIGHT
                ? WardenCaveItems.createSculkBerry(1 + random.nextInt(3))
                : WardenCaveItems.createAbyssFruit(1 + random.nextInt(2));
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), fruit);
    }

    // Cosechar una enredadera de la dimensión da Bayas Luminosas en vez de glow berries
    @EventHandler(ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent e) {
        Block block = e.getHarvestedBlock();
        if (!isVine(block.getType()) || !inWardenCave(block.getWorld())) return;
        e.getItemsHarvested().replaceAll(stack -> stack.getType() == Material.GLOW_BERRIES && !WardenCaveItems.isWardenCaveItem(stack)
                ? WardenCaveItems.createGlowBerry(stack.getAmount()) : stack);
    }

    // Romperla con la mano
    @EventHandler(ignoreCancelled = true)
    public void onVineBreak(BlockDropItemEvent e) {
        if (!isVine(e.getBlockState().getType()) || !inWardenCave(e.getBlock().getWorld())) return;
        e.getItems().forEach(this::convert);
    }

    // Cuando se cae porque se rompió la de arriba, el juego avisa con BlockDestroyEvent justo antes de soltar lo que
    // tenía: se anota el bloque y las glow berries que aparecen ahí en ese tick se cambian. Solo esas, así no se
    // pueden convertir glow berries normales tirándolas dentro de una enredadera
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVineFall(BlockDestroyEvent e) {
        Block block = e.getBlock();
        if (!e.willDrop() || !isVine(block.getType()) || !inWardenCave(block.getWorld())) return;
        if (falling.isEmpty()) Bukkit.getScheduler().runTask(plugin, falling::clear);
        falling.add(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBerrySpawn(ItemSpawnEvent e) {
        if (falling.isEmpty()) return;
        Item item = e.getEntity();
        if (falling.contains(item.getLocation().getBlock())) convert(item);
    }

    private void convert(Item item) {
        ItemStack stack = item.getItemStack();
        if (stack.getType() != Material.GLOW_BERRIES || WardenCaveItems.isWardenCaveItem(stack)) return;
        item.setItemStack(WardenCaveItems.createGlowBerry(stack.getAmount()));
    }

    // Las frutas se comen, no se plantan. En la dimensión tampoco se plantan glow berries ni se les echa polvo de
    // hueso a las enredaderas: así las Bayas Luminosas solo salen de las que ya estaban y no se pueden farmear
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (WardenCaveItems.isFruit(e.getItemInHand())) {
            e.setCancelled(true);
            return;
        }
        if (isVine(e.getBlock().getType()) && inWardenCave(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBoneMeal(BlockFertilizeEvent e) {
        if (isVine(e.getBlock().getType()) && inWardenCave(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent e) {
        String id = WardenCaveItems.idOf(e.getItem());
        if (id == null) return;
        Player player = e.getPlayer();
        switch (id) {
            case "baya_sculk" -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 3 * 60 * 20, 0));
            case "fruta_abisal" -> {
                DarknessShield.protect(player, 90_000);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.of("#b46be0") + "Sin Oscuridad por 1 minuto y medio"));
            }
            case "baya_luminosa" -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1));
            default -> { }
        }
    }

    private static boolean isVine(Material type) {
        return type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT;
    }

    private static boolean inWardenCave(World world) {
        return world.getName().equals(QuasoPlugin.WORLD_NAME);
    }
}
