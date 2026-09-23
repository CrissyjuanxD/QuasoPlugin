package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static Events.MissionSystem.MissionRewards.*;

public class Mission8 extends BaseMission {
    private static final int PER_ORE = 5;
    private static final Map<Material, String> ORES = new LinkedHashMap<>();

    static {
        ORES.put(Material.COAL_ORE, "Carbón");
        ORES.put(Material.DEEPSLATE_COAL_ORE, "Carbón Piz.");
        ORES.put(Material.COPPER_ORE, "Cobre");
        ORES.put(Material.DEEPSLATE_COPPER_ORE, "Cobre Piz.");
        ORES.put(Material.IRON_ORE, "Hierro");
        ORES.put(Material.DEEPSLATE_IRON_ORE, "Hierro Piz.");
        ORES.put(Material.GOLD_ORE, "Oro");
        ORES.put(Material.DEEPSLATE_GOLD_ORE, "Oro Piz.");
        ORES.put(Material.LAPIS_ORE, "Lapislázuli");
        ORES.put(Material.DEEPSLATE_LAPIS_ORE, "Lapis Piz.");
        ORES.put(Material.REDSTONE_ORE, "Redstone");
        ORES.put(Material.DEEPSLATE_REDSTONE_ORE, "Redstone Piz.");
        ORES.put(Material.DIAMOND_ORE, "Diamante");
        ORES.put(Material.DEEPSLATE_DIAMOND_ORE, "Diamante Piz.");
        ORES.put(Material.EMERALD_ORE, "Esmeralda");
        ORES.put(Material.DEEPSLATE_EMERALD_ORE, "Esmeralda Piz.");
        ORES.put(Material.NETHER_QUARTZ_ORE, "Cuarzo");
        ORES.put(Material.NETHER_GOLD_ORE, "Oro del Nether");
        ORES.put(Material.ANCIENT_DEBRIS, "Escombros");
    }

    public Mission8(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 8, "¡Si hay que ser minero!", MissionDifficulty.MEDIA, 16,
                "Junta con Toque de Seda 5 de cada mineral del juego.");
        ORES.forEach((ore, label) -> counter("ore_" + ore.name(), label, PER_ORE));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("excavator_pickaxe", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 1));
    }

    // Mientras cuenta suelta el drop normal en vez del bloque, así no se puede volver a poner y minar
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        if (!ORES.containsKey(type) || !tracking(player)) return;
        if (!player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;

        String key = "ore_" + type.name();
        if (data(player).getProgressInt(key) >= PER_ORE) return;

        event.setDropItems(false);
        ItemStack drop = normalDrop(type);
        if (drop != null) event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
        add(player, key, 1);
    }

    private ItemStack normalDrop(Material ore) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (ore) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> new ItemStack(Material.COAL);
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.RAW_COPPER, 2 + random.nextInt(4));
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.RAW_IRON);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.RAW_GOLD);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> new ItemStack(Material.LAPIS_LAZULI, 4 + random.nextInt(5));
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> new ItemStack(Material.REDSTONE, 4 + random.nextInt(2));
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> new ItemStack(Material.DIAMOND);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> new ItemStack(Material.EMERALD);
            case NETHER_QUARTZ_ORE -> new ItemStack(Material.QUARTZ);
            case NETHER_GOLD_ORE -> new ItemStack(Material.GOLD_NUGGET, 2 + random.nextInt(5));
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
            default -> null;
        };
    }
}
