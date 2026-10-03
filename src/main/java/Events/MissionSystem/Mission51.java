package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission51 extends BaseMission {

    public Mission51(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 51, "Ve a tocar pasto", MissionDifficulty.DIFICIL, 17,
                "Rompe bloques de cobre, hierro, oro, esmeralda, diamante y netherite con Fatiga Minera III.");
        flag("cobre", "Bloque de cobre");
        flag("hierro", "Bloque de hierro");
        flag("oro", "Bloque de oro");
        flag("esmeralda", "Bloque de esmeralda");
        flag("diamante", "Bloque de diamante");
        flag("netherite", "Bloque de netherite");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("potion_haste_3", 3), item(Material.DIAMOND_BLOCK, 5));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PotionEffect fatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue == null || fatigue.getAmplifier() < 2) return;

        String key = blockKey(event.getBlock().getType());
        if (key != null) mark(player, key);
    }

    // El cobre vale en cualquier estado de oxidación, encerado o no
    private String blockKey(Material type) {
        return switch (type) {
            case IRON_BLOCK -> "hierro";
            case GOLD_BLOCK -> "oro";
            case EMERALD_BLOCK -> "esmeralda";
            case DIAMOND_BLOCK -> "diamante";
            case NETHERITE_BLOCK -> "netherite";
            case COPPER_BLOCK, EXPOSED_COPPER, WEATHERED_COPPER, OXIDIZED_COPPER,
                 WAXED_COPPER_BLOCK, WAXED_EXPOSED_COPPER, WAXED_WEATHERED_COPPER, WAXED_OXIDIZED_COPPER -> "cobre";
            default -> null;
        };
    }
}
